package fr.miage.nancy.lunettes.frontend.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import fr.miage.nancy.lunettes.frontend.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Wrapper minimal autour du client MQTT HiveMQ pour le frontend.
 *
 * <p>Cette classe a un seul objectif : exposer une API "facile" (connect,
 * publish, subscribe, disconnect) qui cache la complexité du client HiveMQ.
 * Les écrans n'ont pas à comprendre la subtilité du builder asynchrone.</p>
 *
 * <h3>Threading</h3>
 * <p>Tous les callbacks de souscription remontent sur des threads MQTT internes,
 * <strong>pas</strong> sur le thread JavaFX. Il est donc impératif que le code
 * appelant fasse un {@code Platform.runLater(...)} pour toute mise à jour
 * d'UI déclenchée par un message MQTT. On documente ça côté caller, pas ici,
 * parce que cette classe ne doit pas dépendre de JavaFX.</p>
 */
public class ClientMQTT {

    private static final Logger logger = LoggerFactory.getLogger(ClientMQTT.class);

    private final Configuration config;
    private final Mqtt5AsyncClient client;

    public ClientMQTT(Configuration config) {
        this.config = config;
        // Identifiant unique : prefix + UUID. Si on lance plusieurs frontends en parallele
        // sur la meme machine, ils ne s'ecraseront pas mutuellement cote broker.
        String clientId = config.clientIdPrefix() + UUID.randomUUID();

        this.client = Mqtt5Client.builder()
                .identifier(clientId)
                .serverHost(config.brokerHost())
                .serverPort(config.brokerPort())
                .automaticReconnectWithDefaultConfig()
                .buildAsync();
    }

    /**
     * Etablit la connexion au broker. Renvoie un {@link CompletableFuture} qui se complete
     * une fois la connexion confirmee (ou exceptionnellement en cas d'echec).
     */
    public CompletableFuture<Void> connecter() {
        logger.info("Connexion au broker {}:{}...", config.brokerHost(), config.brokerPort());
        return client.connect().thenAccept(connAck -> {
            logger.info("Connecte au broker MQTT.");
        }).whenComplete((ok, err) -> {
            if (err != null) {
                logger.error("Echec de connexion au broker : {}", err.getMessage());
            }
        });
    }

    /**
     * Publie un payload sur un topic. Sans retain (les anciens clients n'ont pas
     * a recevoir les anciennes commandes).
     */
    public CompletableFuture<?> publier(String topic, byte[] payload) {
        logger.debug("Publish [{}] ({} octets)", topic, payload.length);
        return client.publishWith()
                .topic(topic)
                .payload(payload)
                .send()
                .toCompletableFuture();
    }

    /**
     * S'abonne a un topic et execute le callback fourni a chaque message recu.
     *
     * <p>Le callback recoit le payload deserialise en {@code byte[]}, charge a
     * l'appelant de le decoder via le {@link fr.miage.nancy.lunettes.events.Deserializer}.</p>
     *
     * <p>Attention : le callback s'execute sur un thread MQTT, pas sur le thread JavaFX.</p>
     */
    public CompletableFuture<?> souscrire(String topicFilter, Consumer<Mqtt5Publish> callback) {
        logger.debug("Subscribe [{}]", topicFilter);
        return client.subscribeWith()
                .topicFilter(topicFilter)
                .callback(callback)
                .send()
                .toCompletableFuture();
    }

    /**
     * Annule une souscription. Utile quand on quitte un ecran et qu'on ne veut plus
     * recevoir d'evenements pour une commande terminee.
     */
    public CompletableFuture<?> desinscrire(String topicFilter) {
        logger.debug("Unsubscribe [{}]", topicFilter);
        return client.unsubscribeWith()
                .topicFilter(topicFilter)
                .send()
                .toCompletableFuture();
    }

    /** Ferme proprement la connexion (appele a la fermeture de l'application). */
    public CompletableFuture<Void> deconnecter() {
        logger.info("Deconnexion du broker...");
        return client.disconnect();
    }

    /** Vrai si le client a deja recu un CONNACK du broker. */
    public boolean estConnecte() {
        return client.getState().isConnected();
    }
}
