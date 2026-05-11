package fr.miage.nancy.lunettes.serveur;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import fr.miage.nancy.lunettes.events.Commande;
import fr.miage.nancy.lunettes.events.Serializer;
import fr.miage.nancy.lunettes.events.Topics;
import fr.miage.nancy.lunettes.events.TypeLunette;

/**
 * Outil de simulation d'un client complet pour le protocole MQTT de lunettes.
 * Ce script se connecte au broker, publie une commande valide pour plusieurs types de lunettes, s'abonne aux retours d'état du serveur     
 * (validation, processing, delivery), et vérifie le numéro de série de la première lunette produite via le topic de "check".
 */
public class ClientSimulateur {

    private static final Logger logger = LoggerFactory.getLogger(ClientSimulateur.class);

    public static void main(String[] args) throws Exception {
        // 1. Initialisation : lecture des properties
        Properties properties = new Properties();
        // On lit depuis lunettes-serveur qui contient le fichier
        try (InputStream input = ClientSimulateur.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                logger.error("Fichier application.properties introuvable.");
                return;
            }
            properties.load(input);
        }
        String host = properties.getProperty("mqtt.broker.host", "localhost");
        int port = Integer.parseInt(properties.getProperty("mqtt.broker.port", "1883"));

        // Création du client bloquant
        Mqtt5BlockingClient client = Mqtt5Client.builder()
                .identifier("client-simul-" + UUID.randomUUID())
                .serverHost(host)
                .serverPort(port)
                .buildBlocking();

        client.connect();
        logger.info("✅ Connecté au broker {}:{}", host, port);

        // 2. Préparation
        UUID uuid = UUID.randomUUID();
        String wildcardTopic = "orders/" + uuid + "/#";
        CountDownLatch checkLatch = new CountDownLatch(1);

        // Abonnement asynchrone pour intercepter les messages (utilise toAsync() pour le callback)
        client.toAsync().subscribeWith()
                .topicFilter(wildcardTopic)
                .callback(publish -> {
                    String topic = publish.getTopic().toString();
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    logger.info("📩 Reçu sur [{}] : {}", topic, payload);

                    if (topic.equals(Topics.delivery(uuid))) {
                        // 5. Action 3 - Vérification
                        String[] lines = payload.split("\n");
                        if (lines.length > 0) {
                            String firstLine = lines[0];
                            String numeroSerie = firstLine.split(";")[1].trim();
                            logger.info("🔍 Vérification du numéro de série extrait : {}", numeroSerie);
                            
                            // Abonnement à la réponse
                            client.toAsync().subscribeWith()
                                .topicFilter(Topics.serialResponse(numeroSerie))
                                .callback(res -> {
                                    String resPayload = new String(res.getPayloadAsBytes(), StandardCharsets.UTF_8);
                                    logger.info("🎯 Réponse de vérification pour [{}] : {}", res.getTopic(), resPayload);
                                    checkLatch.countDown();
                                })
                                .send();

                            // Demande de check
                            client.publishWith()
                                    .topic(Topics.serialCheck(numeroSerie))
                                    .payload(new byte[0])
                                    .send();
                        }
                    }
                })
                .send().join();

        logger.info("👂 En écoute sur {}", wildcardTopic);

        // 3. Action 1 - Commande
        Commande commande = new Commande(uuid, Map.of(
                TypeLunette.BANANA, 2,
                TypeLunette.CLAUDE, 1
        ));
        
        byte[] payloadCommande = Serializer.serializeCommande(commande);
        logger.info("📤 Envoi de la commande ({}) : {} BANANA, {} CLAUDE", uuid, 2, 1);
        
        client.publishWith()
                .topic(Topics.order(uuid))
                .payload(payloadCommande)
                .send();

        // 4. Attente de la fin du cycle
        logger.info("⏳ Attente des réponses du serveur...");
        if (!checkLatch.await(10, TimeUnit.SECONDS)) {
            logger.warn("⚠️ Temps d'attente dépassé sans réponse finale.");
        }

        // Déconnexion
        client.disconnect();
        logger.info("🛑 Déconnecté du broker.");
    }
}
