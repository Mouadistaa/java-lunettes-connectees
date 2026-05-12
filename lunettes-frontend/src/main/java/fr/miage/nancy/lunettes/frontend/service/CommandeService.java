package fr.miage.nancy.lunettes.frontend.service;

import fr.miage.nancy.lunettes.events.Commande;
import fr.miage.nancy.lunettes.events.Deserializer;
import fr.miage.nancy.lunettes.events.LunetteProduite;
import fr.miage.nancy.lunettes.events.Serializer;
import fr.miage.nancy.lunettes.events.Topics;
import fr.miage.nancy.lunettes.events.TypeLunette;
import fr.miage.nancy.lunettes.frontend.config.Configuration;
import fr.miage.nancy.lunettes.frontend.mqtt.ClientMQTT;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrateur du cycle de vie d'une commande, vu depuis le frontend.
 *
 * <h3>Role</h3>
 * <p>C'est le pont entre la couche MQTT (brute, asynchrone, multi-thread) et la
 * couche UI (qui veut juste des proprietes observables sur le thread JavaFX).</p>
 *
 * <h3>Pourquoi cette classe</h3>
 * <p>Sans elle, chaque ecran devrait :</p>
 * <ul>
 *   <li>Gerer ses souscriptions MQTT,</li>
 *   <li>Re-router les messages vers le thread JavaFX,</li>
 *   <li>Tenir un etat coherent de la commande en cours,</li>
 *   <li>Empecher l'envoi de plusieurs commandes simultanees.</li>
 * </ul>
 * <p>On centralise tout ici. Les ecrans n'ont qu'a binder des labels/listes sur
 * les proprietes exposees.</p>
 *
 * <h3>Contrainte du sujet</h3>
 * <p>"Une seule commande a la fois par client". On respecte ca en exposant un
 * unique etat global : tant que la commande courante n'est pas terminale, on
 * refuse d'en lancer une nouvelle.</p>
 */
public class CommandeService {

    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    private final ClientMQTT client;
    private final Configuration config;

    // Identifiant de la commande en cours. null s'il n'y en a aucune.
    private UUID commandeCouranteId;

    // --- Proprietes JavaFX, observables par les ecrans ---
    /** Etat de la commande en cours, mis a jour au fil des messages MQTT. */
    private final ObjectProperty<EtatCommande> etat =
            new SimpleObjectProperty<>(EtatCommande.CREEE);
    /** Message d'erreur du backend (annulation ou erreur de fab), vide sinon. */
    private final StringProperty messageErreur = new SimpleStringProperty("");
    /** Liste des lunettes recues a la livraison. Vide tant que pas livre. */
    private final ObservableList<LunetteProduite> lunettesLivrees =
            FXCollections.observableArrayList();

    public CommandeService(ClientMQTT client, Configuration config) {
        this.client = client;
        this.config = config;
    }

    // === API exposee aux ecrans ===

    public ObjectProperty<EtatCommande> etatProperty() {
        return etat;
    }

    public StringProperty messageErreurProperty() {
        return messageErreur;
    }

    public ObservableList<LunetteProduite> lunettesLivrees() {
        return lunettesLivrees;
    }

    public UUID commandeCouranteId() {
        return commandeCouranteId;
    }

    /** Vrai si on a une commande non terminale en cours (UI doit bloquer un nouvel envoi). */
    public boolean aUneCommandeEnCours() {
        return commandeCouranteId != null && !etat.get().estTerminal();
    }

    /**
     * Envoie une nouvelle commande au backend.
     *
     * <p>S'abonne au prealable aux 5 topics de retour pour ne rien rater. La
     * souscription est cablee sur tous les sous-topics via le wildcard
     * {@code orders/{uuid}/#}.</p>
     *
     * @param lignes les quantites par type. Doit respecter les regles de validation
     *               du record {@link Commande} (quantite > 0 par type, < 10, total > 0).
     * @throws IllegalStateException si une commande est deja en cours
     * @throws IllegalArgumentException si les lignes ne forment pas une commande valide
     */
    public synchronized void envoyerCommande(Map<TypeLunette, Integer> lignes) {
        if (aUneCommandeEnCours()) {
            throw new IllegalStateException(
                    "Une commande est deja en cours, attends sa fin avant d'en lancer une autre.");
        }

        // 1. Reset de l'etat (au cas ou on enchaine apres une commande terminee)
        lunettesLivrees.clear();
        messageErreur.set("");
        etat.set(EtatCommande.CREEE);

        // 2. Generation de l'identifiant unique (UUID v4 -> "unique dans l'univers")
        UUID uuid = UUID.randomUUID();
        Commande commande = new Commande(uuid, lignes); // valide les lignes
        this.commandeCouranteId = uuid;

        logger.info("Envoi d'une nouvelle commande uuid={} contenu={}", uuid, lignes);

        // 3. On s'abonne AVANT de publier, sinon on peut rater les messages rapides
        String wildcard = "orders/" + uuid + "/#";
        client.souscrire(wildcard, publish -> {
            String topic = publish.getTopic().toString();
            byte[] payload = publish.getPayloadAsBytes();
            traiterMessage(topic, payload, uuid);
        }).whenComplete((ok, err) -> {
            if (err != null) {
                logger.error("Souscription aux retours echouee : {}", err.getMessage());
                Platform.runLater(() -> {
                    etat.set(EtatCommande.EN_ERREUR);
                    messageErreur.set("Impossible de s'abonner aux reponses du serveur : "
                            + err.getMessage());
                });
                return;
            }
            // 4. Une fois la souscription confirmee, on publie la commande
            byte[] payloadCommande = Serializer.serializeCommande(commande);
            client.publier(Topics.order(uuid), payloadCommande)
                    .whenComplete((okPub, errPub) -> {
                        if (errPub != null) {
                            logger.error("Publication de la commande echouee : {}", errPub.getMessage());
                            Platform.runLater(() -> {
                                etat.set(EtatCommande.EN_ERREUR);
                                messageErreur.set("Envoi impossible : " + errPub.getMessage());
                            });
                        } else {
                            Platform.runLater(() -> etat.set(EtatCommande.ENVOYEE));
                        }
                    });
        });
    }

    /**
     * Verifie un numero de serie aupres du backend.
     *
     * <p>Pattern request/response avec timeout : on s'abonne a {@code serials/{numero}},
     * on publie sur {@code serials/{numero}/check}, et on attend la reponse. Si le backend
     * ne repond pas dans le timeout configure, on remonte une erreur.</p>
     *
     * <p>Le {@link CompletableFuture} retourne se complete sur un thread MQTT : c'est a
     * l'appelant (UI) de remonter sur le thread JavaFX si necessaire.</p>
     *
     * @param numeroSerie le numero a verifier
     * @return un futur portant la reponse ("BANANA", "CHATGPT", "LE_CHAT", "CLAUDE" ou "INVALID")
     */
    public CompletableFuture<String> verifierNumeroSerie(String numeroSerie) {
        CompletableFuture<String> resultat = new CompletableFuture<>();

        String topicReponse = Topics.serialResponse(numeroSerie);
        String topicCheck = Topics.serialCheck(numeroSerie);

        // 1. S'abonner avant de publier
        client.souscrire(topicReponse, publish -> {
            String reponse = Deserializer.deserializeText(publish.getPayloadAsBytes());
            // On se desinscrit pour ne pas accumuler des subs (utile si on verifie plein de numeros)
            client.desinscrire(topicReponse);
            resultat.complete(reponse);
        }).whenComplete((ok, err) -> {
            if (err != null) {
                resultat.completeExceptionally(err);
                return;
            }
            // 2. Publier la demande de check
            client.publier(topicCheck, new byte[0])
                    .whenComplete((okPub, errPub) -> {
                        if (errPub != null) {
                            resultat.completeExceptionally(errPub);
                        }
                    });
        });

        // 3. Timeout : si pas de reponse, on echoue
        long timeoutMs = config.verificationTimeoutMs();
        resultat.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(err -> {
                    client.desinscrire(topicReponse); // nettoie meme en cas de timeout
                    if (err instanceof TimeoutException) {
                        logger.warn("Timeout sur verification de {} apres {}ms", numeroSerie, timeoutMs);
                    }
                    return null;
                });

        return resultat;
    }

    /**
     * Route un message recu sur un sous-topic de la commande vers l'etat correspondant.
     * Tout passe par {@code Platform.runLater} parce qu'on touche aux proprietes JavaFX.
     */
    private void traiterMessage(String topic, byte[] payload, UUID uuid) {
        logger.debug("Recu [{}] ({} octets)", topic, payload.length);

        // Calcul du sous-type une seule fois, plus lisible que de comparer 5 strings completes
        String suffix = topic.substring(("orders/" + uuid).length());

        Platform.runLater(() -> {
            switch (suffix) {
                case "/validated" -> {
                    logger.info("Commande {} validee", uuid);
                    etat.set(EtatCommande.VALIDEE);
                }
                case "/status" -> {
                    String statut = Deserializer.deserializeText(payload);
                    logger.info("Statut commande {} : {}", uuid, statut);
                    if ("processing".equals(statut)) {
                        etat.set(EtatCommande.EN_FABRICATION);
                    }
                    // "processed" ne change pas l'etat : la livraison qui suit fera passer en LIVREE
                }
                case "/delivery" -> {
                    List<LunetteProduite> recues = Deserializer.deserializeDelivery(payload);
                    logger.info("Livraison commande {} : {} lunette(s)", uuid, recues.size());
                    lunettesLivrees.setAll(recues);
                    etat.set(EtatCommande.LIVREE);
                    nettoyerSouscriptionCommande(uuid);
                }
                case "/cancelled" -> {
                    String message = Deserializer.deserializeText(payload);
                    logger.warn("Commande {} annulee : {}", uuid, message);
                    messageErreur.set(message);
                    etat.set(EtatCommande.ANNULEE);
                    nettoyerSouscriptionCommande(uuid);
                }
                case "/error" -> {
                    String message = Deserializer.deserializeText(payload);
                    logger.error("Erreur commande {} : {}", uuid, message);
                    messageErreur.set(message);
                    etat.set(EtatCommande.EN_ERREUR);
                    nettoyerSouscriptionCommande(uuid);
                }
                default -> logger.warn("Topic non gere : {}", topic);
            }
        });
    }

    /**
     * Une fois la commande terminee, on se desabonne du wildcard pour ne pas
     * accumuler les souscriptions au fil des commandes successives.
     */
    private void nettoyerSouscriptionCommande(UUID uuid) {
        String wildcard = "orders/" + uuid + "/#";
        client.desinscrire(wildcard).exceptionally(err -> {
            logger.warn("Echec du unsubscribe sur {} : {}", wildcard, err.getMessage());
            return null;
        });
    }
}
