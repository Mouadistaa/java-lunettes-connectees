package fr.miage.nancy.lunettes.serveur;

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import java.util.UUID;

public class MiniMainTest {
    public static void main(String[] args) throws InterruptedException {
        // 1. Créer le client
        Mqtt5AsyncClient client = Mqtt5Client.builder()
                .identifier("theo-serveur-test-" + UUID.randomUUID()) // Toujours un ID unique
                .serverHost("localhost")
                .serverPort(1883)
                .automaticReconnectWithDefaultConfig() // Magique : gère les micro-coupures
                .buildAsync();

        // 2. Se connecter
        client.connect()
              .whenComplete((connAck, throwable) -> {
                  if (throwable != null) {
                      System.err.println("Erreur de connexion !");
                  } else {
                      System.out.println("Connecté à Mosquitto !");
                  }
              });

        // 3. Souscrire à un topic (Exemple : orders/+)
        client.subscribeWith()
              .topicFilter("orders/+")
              .callback(publish -> {
                  // Ce code s'exécute à chaque message reçu !
                  String topic = publish.getTopic().toString();
                  String payload = new String(publish.getPayloadAsBytes());
                  System.out.println("Reçu sur " + topic + " : \n" + payload);
              })
              .send();

        // 4. Publier un message
        client.publishWith()
              .topic("orders/1234/status")
              .payload("processing".getBytes())
              .send();

        // Bloquer le main pour laisser le thread async écouter
        Thread.sleep(100000); 
    }
}
