// ===================================
//   ESP32 SLAVE (Código Refactorizado)
// ===================================
#include <Wire.h>
#include <MPU6050_tockn.h>
#include <esp_now.h>
#include <WiFi.h>
#include <ArduinoJson.h>

MPU6050 mpu6050(Wire);

// --- Variables de medición ---
float alpha = 0.98;
float anguloTotal = 0.0;
unsigned long tiempoPrevio;
unsigned long tiempoInicio;
int idx = 0;
bool midiendo = false;

// --- CAMBIO ---
// El buffer grande de 8KB ya no es necesario. Lo eliminamos.

// Dirección MAC del MASTER (DEBES USAR LA REAL DE TU DISPOSITIVO)
// La verás en el Monitor Serie del Master cuando se inicie.
uint8_t masterAddress[] = {0x78, 0x1C, 0x3C, 0xDB, 0xE2, 0x90};

// Prototipos de funciones
void obtenerAnguloTotal();
void recalibrarMPU();

// --- Callback para recibir comandos del MASTER ---
void OnDataRecv(const esp_now_recv_info *info, const uint8_t *data, int data_len) {
    if (data_len > 0) {
        char command[10]; // Buffer para el comando
        // Copiamos para asegurar que es un string terminado en null
        memcpy(command, data, min(data_len, (int)sizeof(command) - 1));
        command[min(data_len, (int)sizeof(command) - 1)] = '\0';

        if (strcmp(command, "START") == 0) {
            midiendo = true;
            idx = 0;
            tiempoInicio = millis();
            Serial.println("▶️  [SLAVE] Iniciando medición...");
        }
        else if (strcmp(command, "STOP") == 0) {
            midiendo = false;
            Serial.println("⏹️  [SLAVE] Medición detenida.");
            // Confirmar al MASTER que el SLAVE ha terminado
            esp_now_send(masterAddress, (uint8_t*)"DONE", 4);
            Serial.println("✅ [SLAVE] Enviando confirmación 'DONE' al MASTER.");
        }
        else if (strcmp(command, "CALIBRAR") == 0) {
            recalibrarMPU();
        }
    }
}

void setup() {
    Serial.begin(115200);
    Wire.begin();
    mpu6050.begin();
    Serial.println("Calibrando MPU6050 del SLAVE...");
    mpu6050.calcGyroOffsets();
    Serial.println("Calibración completa.");

    // --- Inicialización de ESP-NOW ---
    WiFi.mode(WIFI_STA);
    if (esp_now_init() != ESP_OK) {
        Serial.println("❌ Error inicializando ESP-NOW");
        return;
    }

    esp_now_register_recv_cb(OnDataRecv);

    // Añadir el peer del MASTER
    esp_now_peer_info_t peerInfo = {};
    memcpy(peerInfo.peer_addr, masterAddress, 6);
    peerInfo.channel = 1;
    peerInfo.encrypt = false;

    if (esp_now_add_peer(&peerInfo) != ESP_OK) {
        Serial.println("❌ Error añadiendo peer MASTER");
        return;
    }

    Serial.println("✅ SLAVE listo. Esperando comandos del MASTER...");
    tiempoPrevio = millis();
}

void loop() {
    if (midiendo) {
        obtenerAnguloTotal();

        StaticJsonDocument<100> doc;
        doc["id"] = "SLAVE";
        doc["idx"] = idx;
        doc["time"] = millis() - tiempoInicio;
        doc["angle"] = anguloTotal;

        // --- CAMBIO ---
        // Serializamos a un buffer local y lo enviamos directamente, SIN salto de línea.
        char jsonBuffer[100];
        size_t len = serializeJson(doc, jsonBuffer);

        // Enviar el JSON puro por ESP-NOW. El MASTER se encargará de añadir el '\n'.
        esp_err_t result = esp_now_send(masterAddress, (uint8_t*)jsonBuffer, len);

        if (result != ESP_OK) {
            Serial.println("❌ Error enviando datos por ESP-NOW");
        }

        idx++;
        delay(200);
    }
}

// Esta función no necesita cambios
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

    anguloTotal = sqrt(anguloX_filtrado * anguloX_filtrado + anguloY_filtrado * anguloY_filtrado) / sqrt(2);
}

// Esta función no necesita cambios
void recalibrarMPU() {
    Serial.println("⚙️  Calibrando SLAVE...");
    mpu6050.calcGyroOffsets();
}