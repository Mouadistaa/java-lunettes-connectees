package fr.miage.nancy.lunettes.serveur;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import fr.miage.nancy.lunettes.events.Deserializer;
import fr.miage.nancy.lunettes.events.MalformedPayloadException;
import fr.miage.nancy.lunettes.events.TypeLunette;

public class ServeurMain {
    public static void main(String[] args) {
        
        Mqtt5AsyncClient client = creerClient();

        clientConnect(client);

        // Bloquer le thread principal pour laisser le client MQTT tourner en arrière-plan
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Arrêt du serveur.");
        }
    }

    /**
     * Connecte le client MQTT asynchrone au broker et s'abonne au topic "orders/+".
     * Lors de la réception d'un message, la méthode désérialise la commande et traite 
     * les éventuelles erreurs de format en publiant un message d'annulation.
     * 
     * @param client Le client MQTT asynchrone à connecter et configurer.
     */
    public static void clientConnect(Mqtt5AsyncClient client) {
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
                            } catch (MalformedPayloadException exception) {
                                String[] parts = topic.split("/");
                                if (parts.length >= 2) {
                                    String uuid = parts[1];
                                    String errorMessage = "Erreur de format : " + exception.getMessage();
                                    client.publishWith()
                                            .topic("orders/" + uuid + "/cancelled")
                                            .payload(errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                            .send();
                                    System.err.println("❌ Commande annulée (" + uuid + ") : " + exception.getMessage());
                                }
                            }
                            catch (Exception e) {
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
    }




    /**
     * Crée et configure un client MQTT asynchrone en chargeant les paramètres de
     * connexion depuis le fichier application.properties.
     * 
     * @return Le client MQTT asynchrone configuré, ou null en cas d'erreur de lecture
     *         des propriétés de configuration.
     */
    public static Mqtt5AsyncClient creerClient() {
       // 1. Charger les propriétés depuis le fichier application.properties
        Properties properties = new Properties();
        try (InputStream input = ServeurMain.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Impossible de trouver le fichier application.properties");
                return null;
            }
            properties.load(input);
        } catch (Exception ex) {
            System.err.println("Erreur lors de la lecture des propriétés : " + ex.getMessage());
            return null;
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
        
        return client;
    }
}