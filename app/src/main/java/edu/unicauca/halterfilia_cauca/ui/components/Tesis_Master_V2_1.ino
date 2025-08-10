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
volatile bool slaveDone = false;
volatile bool pongReceived = false;
unsigned long pingSentTime = 0;

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
void sendDataInChunks(BLECharacteristic* pChar, const char* data, int length);
void sendCombinedData();

class CmdCallbacks: public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pChar) {
    String value = String(pChar->getValue().c_str());
    if (value == "START") {
      midiendo = true;
      slaveDone = false;
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
      midiendo = false;
      esp_now_send(slaveAddress, (uint8_t*)"STOP", 4);
      Serial.println("⏹ [MASTER] Comando STOP enviado a SLAVE. Esperando datos...");
    }
    else if (value == "CALIBRAR") {
      recalibrarMPU();
      esp_now_send(slaveAddress, (uint8_t*)"CALIBRAR", 8);
      Serial.println("⚙️ [MASTER] Calibración completada");
    }
    else if (value == "CHECK_SLAVE") {
      Serial.println("Recibido CHECK_SLAVE. Enviando PING a SLAVE...");
      pongReceived = false;
      esp_now_send(slaveAddress, (uint8_t*)"PING", 4);
      pingSentTime = millis();
      // The result will be sent from the main loop after timeout or PONG
    }
  }
};

// --- Callback recepción datos ESP-NOW ---
void OnDataRecv(const esp_now_recv_info *info, const uint8_t *data, int data_len) {
  if (data_len == 10 && memcmp(data, "SLAVE_DONE", 10) == 0) {
    slaveDone = true;
    Serial.println("✅ [MASTER] SLAVE_DONE recibido. Enviando datos a la app.");
    sendCombinedData();
  } else if (data_len == 4 && memcmp(data, "PONG", 4) == 0) {
    pongReceived = true;
    Serial.println("✅ [MASTER] PONG recibido de SLAVE.");
  } else {
    if ((slaveIndex + data_len) < BUFFER_SIZE) {
      memcpy(bufferSlave + slaveIndex, data, data_len);
      slaveIndex += data_len;
    }
  }
}

void sendCombinedData() {
    char* paqueteFinal = (char*) malloc(masterIndex + slaveIndex + 1);
    if (paqueteFinal) {
      memcpy(paqueteFinal, bufferMaster, masterIndex);
      memcpy(paqueteFinal + masterIndex, bufferSlave, slaveIndex);
      paqueteFinal[masterIndex + slaveIndex] = '\0';

      sendDataInChunks(pAnguloTotal, paqueteFinal, masterIndex + slaveIndex);
      free(paqueteFinal);

      delay(100);
      pAnguloTotal->setValue((uint8_t*)"END", 3);
      pAnguloTotal->notify();
      Serial.println("⏹ [MASTER] Datos completos enviados a BLE");
    } else {
      Serial.println("❌ Error allocating memory for final packet");
    }
    masterIndex = 0;
    slaveIndex = 0;
    bufferMaster[0] = '\0';
    bufferSlave[0] = '\0';
    slaveDone = false;
}

void setup() {
  Serial.begin(115200);
  Wire.begin();
  mpu6050.begin();
  mpu6050.calcGyroOffsets();

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

  BLEDevice::init("ESP32_MASTER");
  pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pAnguloTotal = pService->createCharacteristic(ANGLE_UUID, BLECharacteristic::PROPERTY_NOTIFY);
  pAnguloTotal->addDescriptor(new BLE2902());
  pComando = pService->createCharacteristic(CMD_UUID, BLECharacteristic::PROPERTY_WRITE);
  pComando->setCallbacks(new CmdCallbacks());
  pService->start();
  pServer->getAdvertising()->start();

  Serial.println("✅ MASTER listo. Esperando comandos BLE...");
  tiempoPrevio = millis();
}

void loop() {
  // Handle PING timeout
  if (pingSentTime > 0 && !pongReceived && (millis() - pingSentTime > 2000)) {
    Serial.println("❌ [MASTER] PING timeout. SLAVE no encontrado.");
    pAnguloTotal->setValue((uint8_t*)"SLAVE_ERROR", 11);
    pAnguloTotal->notify();
    pingSentTime = 0; // Reset ping state
  }
  
  // Handle PONG received
  if (pongReceived) {
    pAnguloTotal->setValue((uint8_t*)"SLAVE_OK", 8);
    pAnguloTotal->notify();
    pongReceived = false; // Reset
    pingSentTime = 0;
  }

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

void sendDataInChunks(BLECharacteristic* pChar, const char* data, int length) {
  int mtu = 20;
  int offset = 0;
  while (offset < length) {
    int chunkSize = length - offset;
    if (chunkSize > mtu) {
      chunkSize = mtu;
    }
    pChar->setValue((uint8_t*)(data + offset), chunkSize);
    pChar->notify();
    offset += chunkSize;
  }
}
