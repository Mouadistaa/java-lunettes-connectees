package fr.miage.nancy.lunettes.serveur;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import bernard_flou.Fabricateur;
import fr.miage.nancy.lunettes.events.Deserializer;
import fr.miage.nancy.lunettes.events.MalformedPayloadException;
import fr.miage.nancy.lunettes.events.TypeLunette;
import fr.miage.nancy.lunettes.usine.TypeMapper;
import fr.miage.nancy.lunettes.usine.Usine;
import fr.miage.nancy.lunettes.usine.UsineImpl;

public class ServeurMain {
    
    private static final Logger logger = LoggerFactory.getLogger(ServeurMain.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Usine usine = new UsineImpl(new Fabricateur());
    
    /**
     * Point d'entrée principal du serveur.
     * Crée le client MQTT et lance la connexion et l'écoute des messages.
     * 
     * @param args Les arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        
        Mqtt5AsyncClient client = creerClient();

        clientConnect(client);
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
                        logger.error("Erreur de connexion", throwable);
                    } else {
                        logger.info("✅ Connecté avec succès au broker Mosquitto !");
                        // 4. Écouter sur orders/+
                        ecouterCommandes(client);

                        // 5. Écouter sur serials/+/check
                        ecouterSerials(client);
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

    /**
     * S'abonne au topic "serials/+/check" pour vérifier la validité des numéros de série.
     * Envoie la réponse sur le topic "serials/<numero>".
     * 
     * @param client Le client MQTT asynchrone utilisé pour s'abonner et publier.
     */
    public static void ecouterSerials(Mqtt5AsyncClient client) {
        client.subscribeWith()
            .topicFilter("serials/+/check")
            .callback(publish -> {
                String topic = publish.getTopic().toString();
                String[] parts = topic.split("/");
                if (parts.length >= 3) {
                    String numero = parts[1];
                    logger.info("🔍 Demande de vérification reçue pour le numéro : " + numero);
                    
                    Fabricateur.TypeLunette type = Fabricateur.validateSerial(numero);
                    String response = (type != null) ? type.name() : "INVALID";
                    
                    client.publishWith()
                            .topic("serials/" + numero)
                            .payload(response.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                            .send();
                }
            })
            .send()
            .whenComplete((subAck, subThrowable) -> {
                if (subThrowable != null) {
                    logger.error("Erreur de souscription (serials) : " + subThrowable.getMessage());
                } else {
                    logger.info("📡 En écoute sur le topic 'serials/+/check'...");
                }
            });
    }

    /**
     * Traite de manière asynchrone une commande de lunettes validée.
     * Lance la fabrication via l'usine et publie les résultats ou les erreurs sur les topics correspondants.
     * 
     * @param client Le client MQTT asynchrone.
     * @param commande La commande contenant les types et quantités de lunettes.
     * @param uuid L'identifiant unique de la commande.
     */
    public static void traiterCommande(Mqtt5AsyncClient client, Map<TypeLunette, Integer> commande, String uuid) {
        executorService.submit(() -> {
            try {
                Map<Fabricateur.TypeLunette, Integer> fabCommande = new HashMap<>();
                for (Map.Entry<TypeLunette, Integer> entry : commande.entrySet()) {
                    fabCommande.put(TypeMapper.toFabricateur(entry.getKey()), entry.getValue());
                }

                List<Fabricateur.Lunette> lunettes = usine.produire(fabCommande);
                logger.info("✅ Fabrication terminée pour la commande (" + uuid + ")");

                // 4.3.1 Mettre à jour le statut
                client.publishWith()
                        .topic("orders/" + uuid + "/status")
                        .payload("processed".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .send();

                // 4.3.2 Livrer les numéros de série
                StringBuilder payloadBuilder = new StringBuilder();
                for (Fabricateur.Lunette lunette : lunettes) {
                    payloadBuilder.append(lunette.type.name()).append(";").append(lunette.serial).append("\n");
                }

                client.publishWith()
                        .topic("orders/" + uuid + "/delivery")
                        .payload(payloadBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .send();

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Erreur inconnue";
                client.publishWith()
                        .topic("orders/" + uuid + "/error")
                        .payload(errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                        .send();
                logger.error("❌ Erreur lors de la fabrication de la commande (" + uuid + ")", e);
            }
        });
    }

    /**
     * S'abonne au topic "orders/+" pour recevoir, valider et traiter les nouvelles commandes.
     * Publie le statut de traitement et gère les erreurs de format (annulation).
     * 
     * @param client Le client MQTT asynchrone.
     */
    private static void ecouterCommandes(Mqtt5AsyncClient client) {
        client.subscribeWith()
            .topicFilter("orders/+")
            .callback(publish -> {
                byte[] payloadBytes = publish.getPayloadAsBytes();
                String topic = publish.getTopic().toString();
                try {
                    Map<TypeLunette, Integer> commande = Deserializer.deserializeCommandeLignes(payloadBytes);
                    logger.info("📦 Nouvelle commande reçue sur [" + topic + "] : " + commande);
                    Deserializer.deserializeText(payloadBytes);
                    
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        String uuid = parts[1];
                        
                        //4.1 Valider la commande
                        client.publishWith()
                                .topic("orders/" + uuid + "/validated")
                                .payload(new byte[0])
                                .send();
                                
                        //4.2 Mettre à jour le statut
                        client.publishWith()
                                .topic("orders/" + uuid + "/status")
                                .payload("processing".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .send();
                        
                        logger.info("✅ Commande validée et en cours de traitement (" + uuid + ")");

                        //4.3 Appeler l'Usine (Fabrication) de manière asynchrone
                        traiterCommande(client, commande, uuid);
                    }
                } catch (MalformedPayloadException exception) {
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        String uuid = parts[1];
                        String errorMessage = "Erreur de format : " + exception.getMessage();
                        client.publishWith()
                                .topic("orders/" + uuid + "/cancelled")
                                .payload(errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .send();
                        logger.error("❌ Commande annulée (" + uuid + ") : " + exception.getMessage());
                    }
                }
                catch (Exception e) {
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        String uuid = parts[1];
                        String errorMessage = "Erreur de format : " + e.getMessage();
                        client.publishWith()
                                .topic("orders/" + uuid + "/cancelled")
                                .payload(errorMessage.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .send();
                    }
                    logger.error("❌ Impossible de désérialiser la commande : " + e.getMessage());
                }
            })
            .send()
            .whenComplete((subAck, subThrowable) -> {
                if (subThrowable != null) {
                    logger.error("Erreur de souscription : " + subThrowable.getMessage());
                } else {
                    logger.info("📡 En écoute sur le topic 'orders/+'...");
                }
            });
    }
}