#include <Wire.h>
#include <MPU6050_tockn.h>
#include <esp_now.h>
#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>

MPU6050 mpu6050(Wire);

// Variables de medición
float alpha = 0.98;
float anguloX = 0, anguloY = 0, anguloZ = 0, anguloTotal = 0;
unsigned long tiempoPrevio;
unsigned long tiempoInicio;
int idx = 0;
bool midiendo = false;

// Buffers para datos
#define BUFFER_SIZE 8192
char bufferMaster[BUFFER_SIZE];
char bufferSlave[BUFFER_SIZE];
int masterIndex = 0;
int slaveIndex = 0;

// Dirección MAC del SLAVE (cambiar por la real)
uint8_t slaveAddress[] = {0xA8, 0x42, 0xE3, 0xA9, 0xDC, 0x34};

// UUIDs para BLE
#define SERVICE_UUID "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define ANGLE_UUID   "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
#define CMD_UUID     "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

BLEServer *pServer;
BLECharacteristic *pAnguloTotal;
BLECharacteristic *pComando;

// --- Funciones ---
void obtenerAnguloTotal();
void recalibrarMPU();

class CmdCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pChar) {
        String value = String(pChar->getValue().c_str());
        if (value == "START") {
            midiendo = true;
            masterIndex = 0;
            slaveIndex = 0;
            bufferMaster[0] = '\0';
            bufferSlave[0] = '\0';
            idx = 0;
            tiempoInicio = millis();
            esp_now_send(slaveAddress, (uint8_t*)"START", 5);
            Serial.println("✅ [MASTER] Medición iniciada");
        }
        else if (value == "STOP") {
            Serial.println("========================================");
            Serial.println("DEBUG: Comando STOP recibido.");
            midiendo = false;
            esp_now_send(slaveAddress, (uint8_t*)"STOP", 4);

            size_t totalSize = masterIndex + slaveIndex;
            Serial.print("DEBUG: Tamaño total del buffer a enviar: ");
            Serial.println(totalSize);

            if (totalSize == 0) {
                Serial.println("DEBUG: Buffer vacío, no se enviará nada.");
            } else {
                char* paqueteFinal = (char*)malloc(totalSize + 1);

                if (paqueteFinal) {
                    Serial.println("DEBUG: Memoria para paqueteFinal alocada correctamente.");
                    memcpy(paqueteFinal, bufferMaster, masterIndex);
                    memcpy(paqueteFinal + masterIndex, bufferSlave, slaveIndex);
                    paqueteFinal[totalSize] = '\0';

                    Serial.println("DEBUG: Iniciando envío por fragmentos...");
                    size_t chunkSize = 500;
                    size_t offset = 0;
                    int chunkCount = 0;
                    while (offset < totalSize) {
                        chunkCount++;
                        size_t remaining = totalSize - offset;
                        size_t currentChunkSize = remaining > chunkSize ? chunkSize : remaining;

                        Serial.print("DEBUG: Enviando fragmento #");
                        Serial.print(chunkCount);
                        Serial.print(" (tamaño: ");
                        Serial.print(currentChunkSize);
                        Serial.println(")");

                        pAnguloTotal->setValue((uint8_t*)(paqueteFinal + offset), currentChunkSize);
                        bool fueEnviado = pAnguloTotal->notify();

                        Serial.print("DEBUG: pAnguloTotal->notify() retornó: ");
                        Serial.println(fueEnviado ? "true" : "false");

                        offset += currentChunkSize;
                        delay(20); // Aumentamos el delay para dar más tiempo
                    }
                    Serial.println("DEBUG: Bucle de envío finalizado.");

                    free(paqueteFinal);
                } else {
                    Serial.println("ERROR FATAL: No se pudo alocar memoria para el paquete final");
                }
            }

            Serial.println("DEBUG: Enviando señal de finalización 'END'...");
            pAnguloTotal->setValue((uint8_t*)"END", 3);
            bool endEnviado = pAnguloTotal->notify();
            Serial.print("DEBUG: notify() para 'END' retornó: ");
            Serial.println(endEnviado ? "true" : "false");

            masterIndex = 0;
            slaveIndex = 0;
            bufferMaster[0] = '\0';
            bufferSlave[0] = '\0';
            Serial.println("========================================");
        }
    }
};

// --- Callback recepción datos ESP-NOW ---
void OnDataRecv(const esp_now_recv_info *info, const uint8_t *data, int data_len) {
    if ((slaveIndex + data_len + 1) < BUFFER_SIZE) {
        memcpy(bufferSlave + slaveIndex, data, data_len);
        slaveIndex += data_len;
        bufferSlave[slaveIndex] = '\n';
        slaveIndex++;
    }
}

void setup() {
    Serial.begin(115200);
    Wire.begin();
    mpu6050.begin();
    mpu6050.calcGyroOffsets();

    // --- ESP-NOW ---
    WiFi.mode(WIFI_STA);
    if (esp_now_init() != ESP_OK) {
        Serial.println("❌ Error inicializando ESP-NOW");
        return;
    }
    esp_now_register_recv_cb(OnDataRecv);

    esp_now_peer_info_t peerInfo = {};
    memcpy(peerInfo.peer_addr, slaveAddress, 6);
    peerInfo.channel = 1;
    peerInfo.encrypt = false;

    if (esp_now_add_peer(&peerInfo) != ESP_OK) {
        Serial.println("❌ Error añadiendo peer");
        return;
    }

    // --- BLE ---
    BLEDevice::init("ESP32_MASTER");
    pServer = BLEDevice::createServer();

    BLEService *pService = pServer->createService(SERVICE_UUID);
    pAnguloTotal = pService->createCharacteristic(ANGLE_UUID, BLECharacteristic::PROPERTY_NOTIFY | BLECharacteristic::PROPERTY_READ);
    pAnguloTotal->addDescriptor(new BLE2902());

    pComando = pService->createCharacteristic(CMD_UUID, BLECharacteristic::PROPERTY_WRITE);
    pComando->setCallbacks(new CmdCallbacks());

    pService->start();
    pServer->getAdvertising()->start();

    Serial.println("✅ MASTER listo. Esperando comandos BLE...");
    tiempoPrevio = millis();
}

void loop() {
    if (midiendo) {
        obtenerAnguloTotal();

        StaticJsonDocument<100> doc;
        doc["id"] = "MASTER";
        doc["idx"] = idx;
        doc["time"] = millis() - tiempoInicio;
        doc["angle"] = anguloTotal;

        char buffer[100];
        serializeJson(doc, buffer);

        if ((masterIndex + strlen(buffer) + 2) < BUFFER_SIZE) {
            int n = snprintf(bufferMaster + masterIndex, BUFFER_SIZE - masterIndex, "%s\n", buffer);
            if (n > 0) masterIndex += n;
        }

        idx++;
        delay(20);
    }
}

void obtenerAnguloTotal() {
    mpu6050.update();
    float accX = mpu6050.getAccX();
    float accY = mpu6050.getAccY();
    float accZ = mpu6050.getAccZ();

    float angAccX = atan2(accY, accZ) * 180 / PI;
    float angAccY = atan2(-accX, sqrt(accY * accY + accZ * accZ)) * 180 / PI;

    float gyroX = mpu6050.getGyroX();
    float gyroY = mpu6050.getGyroY();
    float gyroZ = mpu6050.getGyroZ();

    float dt = (millis() - tiempoPrevio) / 1000.0;
    tiempoPrevio = millis();

    anguloX += gyroX * dt;
    anguloY += gyroY * dt;
    anguloZ += gyroZ * dt;

    float angleX_Final = alpha * anguloX + (1 - alpha) * angAccX;
    float angleY_Final = alpha * anguloY + (1 - alpha) * angAccY;
    float angleZ_Final = anguloZ;

    anguloTotal = sqrt(angleX_Final * angleX_Final + angleY_Final * angleY_Final + angleZ_Final * angleZ_Final) / sqrt(3);
}

void recalibrarMPU() {
    Serial.println("⚙️ Calibrando Master...");
    mpu6050.calcGyroOffsets();
}
