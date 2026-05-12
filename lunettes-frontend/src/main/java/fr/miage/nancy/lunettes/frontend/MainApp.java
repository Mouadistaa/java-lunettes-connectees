package fr.miage.nancy.lunettes.frontend;

import fr.miage.nancy.lunettes.frontend.config.Configuration;
import fr.miage.nancy.lunettes.frontend.mqtt.ClientMQTT;
import fr.miage.nancy.lunettes.frontend.service.CommandeService;
import fr.miage.nancy.lunettes.frontend.ui.BarreNavigation;
import fr.miage.nancy.lunettes.frontend.ui.EcranAccueil;
import fr.miage.nancy.lunettes.frontend.ui.EcranCommande;
import fr.miage.nancy.lunettes.frontend.ui.EcranLivraison;
import fr.miage.nancy.lunettes.frontend.ui.EcranVerification;
import fr.miage.nancy.lunettes.frontend.ui.Navigation;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe Application JavaFX : c'est ici que tout demarre.
 *
 * <p>Le {@link Launcher} appelle {@code main()} qui delegue a {@code Application.launch()},
 * lequel finit par instancier cette classe et appeler {@link #start(Stage)}.</p>
 *
 * <h3>Cycle de vie</h3>
 * <ol>
 *   <li>Chargement de la {@link Configuration} depuis le classpath</li>
 *   <li>Creation du {@link ClientMQTT} (pas encore connecte)</li>
 *   <li>Creation du {@link CommandeService} qui orchestre tout</li>
 *   <li>Construction du squelette UI (barre de nav + zone centrale)</li>
 *   <li>Affichage de l'ecran d'accueil</li>
 *   <li>Connexion MQTT en arriere-plan (non bloquante, l'app reste utilisable)</li>
 * </ol>
 *
 * <h3>Note importante</h3>
 * <p>La connexion MQTT est <strong>asynchrone</strong>. Si le broker est down,
 * l'app s'affiche quand meme et un dialog d'erreur previent l'utilisateur.
 * On ne plante pas. C'est ce qui rend l'app "production ready" demandee par le sujet.</p>
 */
public class MainApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private ClientMQTT client;
    private CommandeService commandeService;
    private Navigation navigation;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Demarrage du frontend Lunettes connectees");

        // 1. Configuration + couches techniques
        Configuration config = new Configuration();
        client = new ClientMQTT(config);
        commandeService = new CommandeService(client, config);

        // 2. Squelette de l'UI : BorderPane avec barre en haut, contenu au centre
        BorderPane racine = new BorderPane();
        BarreNavigation barreNav = new BarreNavigation(
                () -> allerVers(Navigation.Ecran.ACCUEIL),
                () -> allerVers(Navigation.Ecran.COMMANDE),
                () -> allerVers(Navigation.Ecran.LIVRAISON),
                () -> allerVers(Navigation.Ecran.VERIFICATION)
        );
        racine.setTop(barreNav);
        navigation = new Navigation(racine, barreNav);

        // 3. Scene + CSS
        Scene scene = new Scene(racine, 1100, 760);
        scene.getStylesheets().add(
                getClass().getResource("/styles/theme.css").toExternalForm()
        );

        primaryStage.setTitle("Lunettes connectees");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        // On intercepte la fermeture pour debrancher proprement le client MQTT
        primaryStage.setOnCloseRequest(e -> fermer());
        primaryStage.show();

        // 4. Affiche l'accueil immediatement, puis tente de se connecter en arriere-plan
        allerVers(Navigation.Ecran.ACCUEIL);
        client.connecter().exceptionally(err -> {
            logger.error("Connexion au broker echouee : {}", err.getMessage());
            Platform.runLater(() -> afficherErreurConnexion(err));
            return null;
        });
    }

    /** Centralise la navigation : evite que chaque ecran sache instancier les autres. */
    private void allerVers(Navigation.Ecran ecran) {
        switch (ecran) {
            case ACCUEIL -> navigation.afficher(
                    new EcranAccueil(
                            () -> allerVers(Navigation.Ecran.COMMANDE),
                            () -> allerVers(Navigation.Ecran.VERIFICATION)
                    ),
                    Navigation.Ecran.ACCUEIL
            );
            case COMMANDE -> navigation.afficher(
                    new EcranCommande(
                            commandeService,
                            () -> allerVers(Navigation.Ecran.LIVRAISON)
                    ),
                    Navigation.Ecran.COMMANDE
            );
            case LIVRAISON -> navigation.afficher(
                    new EcranLivraison(
                            commandeService,
                            () -> allerVers(Navigation.Ecran.COMMANDE),
                            () -> allerVers(Navigation.Ecran.VERIFICATION)
                    ),
                    Navigation.Ecran.LIVRAISON
            );
            case VERIFICATION -> navigation.afficher(
                    new EcranVerification(commandeService),
                    Navigation.Ecran.VERIFICATION
            );
        }
    }

    private void afficherErreurConnexion(Throwable err) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Broker MQTT injoignable");
        alert.setHeaderText("Impossible de se connecter au broker.");
        alert.setContentText(
                "Verifie que Mosquitto tourne :\n\n"
                        + "  docker compose up\n\n"
                        + "L'application reste utilisable, mais aucune commande "
                        + "ne pourra etre envoyee tant que la connexion n'est pas etablie.\n\n"
                        + "Detail : " + (err.getMessage() != null ? err.getMessage() : err)
        );
        alert.show();
    }

    /**
     * Fermeture propre : on coupe la connexion MQTT, on libere les ressources,
     * et on quitte. Sans ce hook, l'app pouvait laisser des threads MQTT vivants.
     */
    @Override
    public void stop() {
        fermer();
    }

    private void fermer() {
        logger.info("Fermeture du frontend, deconnexion du broker...");
        try {
            if (client != null) {
                client.deconnecter().get();
            }
        } catch (Exception e) {
            logger.warn("Erreur lors de la deconnexion : {}", e.getMessage());
        }
        Platform.exit();
    }
}
