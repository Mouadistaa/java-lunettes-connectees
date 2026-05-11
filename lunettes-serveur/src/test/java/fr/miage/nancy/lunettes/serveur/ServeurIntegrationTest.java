package fr.miage.nancy.lunettes.serveur;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

/**
 * Classe de tests d'intégration (JUnit 5) pour valider le comportement asynchrone et de bout en bout du serveur MQTT.
 * Simule des interactions client-serveur pour tester les cas de succès et les rejets de requêtes.
 */
class ServeurIntegrationTest {

    private static Mqtt5AsyncClient serveurClient;
    private static Mqtt5AsyncClient testClient;

    @BeforeAll
    static void setUp() throws Exception {
        // 1. Démarrer le serveur
        serveurClient = ServeurMain.creerClient();
        ServeurMain.clientConnect(serveurClient);
        
        // Attendre que le serveur soit bien connecté
        Thread.sleep(1000); 

        // 2. Créer et connecter le client de test
        testClient = Mqtt5Client.builder()
                .identifier("test-client-" + UUID.randomUUID())
                .serverHost("localhost")
                .serverPort(1883)
                .buildAsync();

        testClient.connect().get(5, TimeUnit.SECONDS);
    }

    @Test
    void testRejetCommandeMalFormee() throws Exception {
        String uuid = "test-uuid-1";
        CompletableFuture<String> future = new CompletableFuture<>();

        // S'abonner au topic d'annulation pour cette commande spécifique
        testClient.subscribeWith()
                .topicFilter("orders/" + uuid + "/cancelled")
                .callback(publish -> {
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    future.complete(payload);
                })
                .send()
                .get(5, TimeUnit.SECONDS);

        // Envoyer une commande mal formée (quantité non numérique)
        String invalidPayload = "BANANA;abc";
        testClient.publishWith()
                .topic("orders/" + uuid)
                .payload(invalidPayload.getBytes(StandardCharsets.UTF_8))
                .send()
                .get(5, TimeUnit.SECONDS);

        // Attendre la réponse du serveur (timeout de 5 secondes pour éviter un blocage)
        String reponse = future.get(5, TimeUnit.SECONDS);

        // Vérifier que la réponse contient bien une erreur de format / type numérique
        // (L'implémentation de Deserializer devrait renvoyer une erreur liée au parsing)
        // Vérifier que la réponse contient bien l'erreur demandée
        assertTrue(reponse.contains("quantité non numérique"),
                "La réponse d'erreur ne contient pas le texte attendu. Reçu : " + reponse);
    }

    @Test
    void testCommandeValide() throws Exception {
        String uuid = "test-uuid-valid-" + UUID.randomUUID().toString().substring(0, 8);
        CompletableFuture<String> futureDelivery = new CompletableFuture<>();

        // S'abonner à tous les sous-topics pour cette commande afin de voir ce qui se passe
        testClient.subscribeWith()
                .topicFilter("orders/" + uuid + "/#")
                .callback(publish -> {
                    String topic = publish.getTopic().toString();
                    String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
                    System.out.println("TEST REÇU : " + topic + " -> " + payload);

                    if (topic.endsWith("/delivery")) {
                        futureDelivery.complete(payload);
                    } else if (topic.endsWith("/error")) {
                        futureDelivery.completeExceptionally(new RuntimeException("Erreur serveur reçue : " + payload));
                    }
                })
                .send()
                .get(5, TimeUnit.SECONDS);

        // Envoyer une commande avec 4 articles (la capacité du fabricateur)
        String validPayload = "BANANA;4";
        testClient.publishWith()
                .topic("orders/" + uuid)
                .payload(validPayload.getBytes(StandardCharsets.UTF_8))
                .send()
                .get(5, TimeUnit.SECONDS);

        // Attendre la réponse du serveur (timeout de 60 secondes au cas où l'usine serait lente)
        String reponseDelivery = futureDelivery.get(60, TimeUnit.SECONDS);

        // Vérifier que la réponse de livraison contient bien le type attendu et un numéro de série
        assertTrue(reponseDelivery.contains("BANANA;"), 
                "La livraison ne contient pas le type attendu. Reçu : " + reponseDelivery);
        
        // S'assurer qu'un numéro de série a bien été généré après le point-virgule
        String[] parts = reponseDelivery.trim().split(";");
        assertTrue(parts.length >= 2 && !parts[1].trim().isEmpty(), 
                "Aucun numéro de série trouvé dans la livraison.");
    }

    @AfterAll
    static void endServerAndClient() throws Exception {
        if (testClient != null) {
            testClient.disconnect().get(5, TimeUnit.SECONDS);
        }
        if (serveurClient != null) {
            serveurClient.disconnect().get(5, TimeUnit.SECONDS);
        }
    }
}
