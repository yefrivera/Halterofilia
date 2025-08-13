// =============================
//  ESP32 SLAVE (Sincronizado)
// =============================
#include <Wire.h>
#include <MPU6050_tockn.h>
#include <esp_now.h>
#include <WiFi.h>
#include <ArduinoJson.h>

MPU6050 mpu6050(Wire);

// --- Variables de medición ---
float alpha = 0.98;
float anguloX = 0, anguloY = 0, anguloZ = 0, anguloTotal = 0;
unsigned long tiempoPrevio;
unsigned long tiempoInicio;
int idx = 0;
bool midiendo = false;

// --- Buffer para datos ---
#define BUFFER_SIZE 8192
char dataBuffer[BUFFER_SIZE];
int bufferIndex = 0;

// Dirección MAC del MASTER (CORREGIDA)
uint8_t masterAddress[] = {0x78, 0x1C, 0x3C, 0xDB, 0xE2, 0x92};

// --- Funciones ---
void obtenerAnguloTotal();
void recalibrarMPU();

// --- Callback recepción de comandos ---
void OnDataRecv(const esp_now_recv_info *info, const uint8_t *data, int data_len) {
  if (data_len > 0) {
    if (memcmp(data, "START", 5) == 0) {
      midiendo = true;
      idx = 0;
      bufferIndex = 0;
      dataBuffer[0] = '\0';
      tiempoInicio = millis();
      Serial.println("▶️ [SLAVE] Iniciando medición...");
    }
    else if (memcmp(data, "STOP", 4) == 0) {
      midiendo = false;
      Serial.println("⏹ [SLAVE] Medición detenida. Enviando datos...");
      // Send all buffered data to master
      esp_now_send(masterAddress, (uint8_t*)dataBuffer, bufferIndex);
      delay(50); // Give it a moment to send
      // Send end marker
      esp_now_send(masterAddress, (uint8_t*)"SLAVE_DONE", 10);
      bufferIndex = 0; // Reset buffer
    }
    else if (memcmp(data, "CALIBRAR", 8) == 0) {
      recalibrarMPU();
      Serial.println("⚙️ [SLAVE] Calibración completada.");
    }
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

  // Registramos callback de recepción
  esp_now_register_recv_cb(OnDataRecv);

  // Añadir peer MASTER
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

    // Crear JSON
    StaticJsonDocument<100> doc;
    doc["id"] = "SLAVE";
    doc["idx"] = idx;
    doc["time"] = millis() - tiempoInicio;
    doc["angle"] = anguloTotal;

    char jsonBuffer[100];
    size_t len = serializeJson(doc, jsonBuffer);
    
    // Append to buffer instead of sending immediately
    if ((bufferIndex + len + 2) < BUFFER_SIZE) {
      int n = snprintf(dataBuffer + bufferIndex, BUFFER_SIZE - bufferIndex, "%s\n", jsonBuffer);
      if (n > 0) bufferIndex += n;
    }

    idx++;
    delay(20); // 50Hz
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
  Serial.println("⚙️ Calibrando SLAVE...");
  mpu6050.calcGyroOffsets();
}
