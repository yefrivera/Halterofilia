#include <Wire.h>
#include <MPU6050_tockn.h>
#include <esp_now.h>
#include <WiFi.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <ArduinoJson.h>

// Objeto para el sensor MPU6050
MPU6050 mpu6050(Wire);

// --- Variables de Medición ---
float alpha = 0.98;
float anguloTotal = 0.0;
unsigned long tiempoPrevio;
unsigned long tiempoInicio;
int idx = 0;
bool midiendo = false;

// --- Configuración de ESP-NOW ---
// Reemplaza esta dirección MAC con la de tu dispositivo SLAVE
uint8_t slaveAddress[] = {0x78, 0x1C, 0x3C, 0xF6, 0x33, 0x64};

// --- UUIDs para el Servicio BLE ---
#define SERVICE_UUID "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define ANGLE_UUID   "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
#define CMD_UUID     "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

// Punteros para el servidor y características BLE
BLEServer *pServer = nullptr;
BLECharacteristic *pAnguloTotal = nullptr;
BLECharacteristic *pComando = nullptr;

// Prototipos de funciones
void obtenerAnguloTotal();
void enviarLineaPorBLE(const char* linea);

// --- Clases y Funciones ---

// Callback para cuando la app escribe en la característica de comandos (CMD_UUID)
class CmdCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pChar) {
        String value = String(pChar->getValue().c_str());

        if (value == "START") {
            midiendo = true;
            idx = 0;
            tiempoInicio = millis();
            esp_now_send(slaveAddress, (uint8_t*)"START", 5);
            Serial.println("✅ [MASTER] Medición iniciada");
        }
        else if (value == "STOP") {
            Serial.println("⏹️ [MASTER] Comando STOP recibido. Pidiendo al SLAVE que termine.");
            // Le pedimos al slave que se detenga. El slave terminará y luego enviará "DONE".
            // La señal "END" se enviará a la app cuando recibamos la confirmación "DONE".
            esp_now_send(slaveAddress, (uint8_t*)"STOP", 4);
        }
    }
};

// Callback para cuando se reciben datos por ESP-NOW
void OnDataRecv(const esp_now_recv_info *info, const uint8_t *data, int data_len) {
    // Asegurarse de que los datos recibidos terminen en null para tratarlos como string
    char bufferRx[251]; // Límite de ESP-NOW es 250 bytes
    if (data_len > 0 && data_len < sizeof(bufferRx)) {
        memcpy(bufferRx, data, data_len);
        bufferRx[data_len] = '\0'; // Carácter nulo terminador

        // Comprobar si es la señal "DONE" del SLAVE, que indica que ha terminado
        if (strcmp(bufferRx, "DONE") == 0) {
            Serial.println("✅ [MASTER] Confirmación DONE recibida del SLAVE.");

            // Ahora que la medición terminó por completo, enviamos la señal de fin a la app
            enviarLineaPorBLE("END");

            midiendo = false; // Detenemos la generación de datos del master
        } else {
            // Si no es "DONE", es un JSON del SLAVE. Lo reenviamos a la app.
            enviarLineaPorBLE(bufferRx);
        }
    }
}

void setup() {
    Serial.begin(115200);
    Wire.begin();
    mpu6050.begin();
    Serial.println("Calibrando MPU6050...");
    mpu6050.calcGyroOffsets();
    Serial.println("Calibración completa.");

    // --- Inicialización de ESP-NOW ---
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
        Serial.println("❌ Error añadiendo peer de ESP-NOW");
        return;
    }

    // --- Inicialización de BLE ---
    BLEDevice::init("ESP32_MASTER");
    pServer = BLEDevice::createServer();

    BLEService *pService = pServer->createService(SERVICE_UUID);

    // Característica para recibir comandos desde la app
    pComando = pService->createCharacteristic(CMD_UUID, BLECharacteristic::PROPERTY_WRITE);
    pComando->setCallbacks(new CmdCallbacks());

    // Característica para enviar datos a la app
    pAnguloTotal = pService->createCharacteristic(ANGLE_UUID, BLECharacteristic::PROPERTY_NOTIFY);
    pAnguloTotal->addDescriptor(new BLE2902()); // Descriptor estándar para notificaciones

    pService->start();

    // Iniciar advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    BLEDevice::startAdvertising();

    Serial.println("✅ MASTER listo. Esperando conexión y comandos BLE...");
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

        char jsonBuffer[100];
        serializeJson(doc, jsonBuffer);

        // Enviar el JSON directamente a la app en lugar de acumularlo
        enviarLineaPorBLE(jsonBuffer);

        idx++;
        delay(200); // Frecuencia de muestreo del master
    }
}

// --- NUEVA FUNCIÓN ---
// Envía una línea de datos (JSON) a la app a través de BLE, añadiendo un salto de línea.
void enviarLineaPorBLE(const char* linea) {
    // Solo enviar si hay un dispositivo conectado
    if (pServer->getConnectedCount() > 0) {
        char bufferConSaltoDeLinea[120]; // Buffer temporal para añadir el '\n'
        snprintf(bufferConSaltoDeLinea, sizeof(bufferConSaltoDeLinea), "%s\n", linea);

        pAnguloTotal->setValue(bufferConSaltoDeLinea);
        pAnguloTotal->notify();

        // Log para depuración en el Monitor Serie
        Serial.print("BLE TX -> ");
        Serial.print(bufferConSaltoDeLinea);

        delay(20); // Pequeña pausa para dar tiempo al stack de BLE a enviar los datos
    }
}

// Calcula el ángulo total a partir de los datos del MPU6050
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

    static float anguloX_filtrado = 0.0, anguloY_filtrado = 0.0, anguloZ_raw = 0.0;

    anguloX_filtrado = alpha * (anguloX_filtrado + gyroX * dt) + (1 - alpha) * angAccX;
    anguloY_filtrado = alpha * (anguloY_filtrado + gyroY * dt) + (1 - alpha) * angAccY;
    anguloZ_raw += gyroZ * dt;

    // Cálculo simplificado del ángulo total
    anguloTotal = sqrt(anguloX_filtrado * anguloX_filtrado + anguloY_filtrado * anguloY_filtrado) / sqrt(2);
}