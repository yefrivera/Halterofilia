#include "WiFi.h"

void setup(){
  Serial.begin(115200);
  Serial.println();
  
  // Poner el dispositivo en modo Wi-Fi Station
  WiFi.mode(WIFI_STA);
  
  // Imprimir la dirección MAC de la interfaz Wi-Fi
  Serial.println("==================================================");
  Serial.print("La dirección MAC Wi-Fi de este ESP32 es: ");
  Serial.println(WiFi.macAddress());
  Serial.println("==================================================");
  Serial.println("Copia esta dirección MAC. La necesitarás en el sketch principal.");
  Serial.println("Puedes cargar este mismo código en tu otro ESP32 para encontrar su MAC.");
}

void loop(){
  // No es necesario hacer nada en el loop.
}
