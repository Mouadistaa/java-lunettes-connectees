package fr.miage.nancy.lunettes.serveur;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import fr.miage.nancy.lunettes.events.Deserializer;
import fr.miage.nancy.lunettes.events.TypeLunette;


public class ServeurMain {
    public static void main(String[] args) {
        // 1. Charger les propriétés depuis le fichier application.properties
        Properties properties = new Properties();
        try (InputStream input = ServeurMain.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Impossible de trouver le fichier application.properties");
                return;
            }
            properties.load(input);
        } catch (Exception ex) {
            System.err.println("Erreur lors de la lecture des propriétés : " + ex.getMessage());
            return;
        }

        String host = properties.getProperty("mqtt.broker.host", "localhost");
        int port = Integer.parseInt(properties.getProperty("mqtt.broker.port", "1883"));

        System.out.println("Démarrage du serveur et connexion au broker MQTT " + host + ":" + port + "...");

        // 2. Créer le client MQTT asynchrone avec reconnexion automatique
        Mqtt5AsyncClient client = Mqtt5Client.builder()
                .identifier("serveur-main-" + UUID.randomUUID())
                .serverHost(host)
                .serverPort(port)
                .automaticReconnectWithDefaultConfig()
                .buildAsync();

        // 3. Se connecter au broker
        client.connect()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("Erreur de connexion au broker MQTT : " + throwable.getMessage());
                    } else {
                        System.out.println("✅ Connecté avec succès au broker Mosquitto !");
                        // 4. Écouter sur orders/+
                        client.subscribeWith()
                        .topicFilter("orders/+")
                        .callback(publish -> {
                            byte[] payloadBytes = publish.getPayloadAsBytes();
                            String topic = publish.getTopic().toString();
                            try {
                                Map<TypeLunette, Integer> commande = Deserializer.deserializeCommandeLignes(payloadBytes);
                                System.out.println("📦 Nouvelle commande reçue sur [" + topic + "] : " + commande);
                                Deserializer.deserializeText(payloadBytes);
                            } catch (Exception e) {
                                System.err.println("❌ Impossible de désérialiser la commande : " + e.getMessage());
                            }
                        })
                        .send()
                        .whenComplete((subAck, subThrowable) -> {
                            if (subThrowable != null) {
                                System.err.println("Erreur de souscription : " + subThrowable.getMessage());
                            } else {
                                System.out.println("📡 En écoute sur le topic 'orders/+'...");
                            }
                        });
                    }
                });

        // Bloquer le thread principal pour laisser le client MQTT tourner en arrière-plan
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Arrêt du serveur.");
        }
    }
}
