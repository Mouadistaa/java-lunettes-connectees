package fr.miage.nancy.lunettes.frontend.ui;

import fr.miage.nancy.lunettes.frontend.service.CommandeService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Ecran de verification d'un numero de serie.
 *
 * <p>L'utilisateur saisit un numero (au format <code>XX-NNNNN-CCCCCCCC</code>),
 * clique sur "Verifier", et l'app interroge le backend via le pattern
 * request/response decrit dans la grammaire MQTT :</p>
 * <ul>
 *   <li>publish sur <code>serials/{numero}/check</code> avec un payload vide</li>
 *   <li>subscribe sur <code>serials/{numero}</code> et attente de la reponse</li>
 * </ul>
 *
 * <p>La reponse est soit un type de lunette (<code>BANANA</code>, <code>CHATGPT</code>,
 * <code>LE_CHAT</code>, <code>CLAUDE</code>) -> bandeau vert "authentique",
 * soit la chaine <code>INVALID</code> -> bandeau rouge.</p>
 *
 * <p>On gere aussi le cas du timeout (broker injoignable ou serveur down) :
 * bandeau rouge "pas de reponse dans les temps".</p>
 */
public final class EcranVerification extends VBox {

    private final CommandeService service;
    private final TextField champ;
    private final Button btnVerifier;
    private final Label resultat;

    public EcranVerification(CommandeService service) {
        this.service = service;
        getStyleClass().add("contenu-principal");
        setSpacing(20);

        Label titre = new Label("Verification d'un numero de serie");
        titre.getStyleClass().add("titre-section");

        Label sousTitre = new Label(
                "Saisis un numero (format XX-NNNNN-CCCCCCCC, par exemple BA-5XK2J-A1B2C3D4) "
                        + "et nous interrogeons l'usine pour confirmer son authenticite."
        );
        sousTitre.getStyleClass().add("label-info");
        sousTitre.setWrapText(true);

        champ = new TextField();
        champ.setPromptText("Numero de serie...");
        champ.setPrefWidth(360);
        champ.setMaxWidth(360);

        btnVerifier = new Button("Verifier");
        btnVerifier.getStyleClass().add("bouton-primaire");
        btnVerifier.setOnAction(e -> lancerVerification());

        // Permet de valider via la touche Entree depuis le champ de saisie
        champ.setOnAction(e -> lancerVerification());

        HBox ligneSaisie = new HBox(12, champ, btnVerifier);
        ligneSaisie.setAlignment(Pos.CENTER_LEFT);

        resultat = new Label();
        resultat.setWrapText(true);
        resultat.setMaxWidth(Double.MAX_VALUE);
        resultat.setVisible(false);
        resultat.setManaged(false);
        resultat.setPadding(new Insets(0, 0, 0, 0));

        getChildren().addAll(titre, sousTitre, ligneSaisie, resultat);
    }

    private void lancerVerification() {
        String numero = champ.getText() == null ? "" : champ.getText().trim();
        if (numero.isEmpty()) {
            afficherResultat("Saisis d'abord un numero de serie a verifier.",
                    "banniere-info", "banniere-info-texte");
            return;
        }

        btnVerifier.setDisable(true);
        afficherResultat("Verification en cours...",
                "banniere-info", "banniere-info-texte");

        service.verifierNumeroSerie(numero).whenComplete((reponse, err) -> Platform.runLater(() -> {
            btnVerifier.setDisable(false);
            if (err != null || reponse == null) {
                // Timeout ou erreur reseau
                afficherResultat(
                        "Pas de reponse du serveur dans les temps. "
                                + "Verifie que le broker et le backend tournent, puis reessaie.",
                        "banniere-erreur", "banniere-erreur-texte"
                );
                return;
            }
            if ("INVALID".equalsIgnoreCase(reponse.trim())) {
                afficherResultat(
                        "Ce numero de serie n'est pas reconnu par l'usine. "
                                + "Il est invalide ou n'a pas encore ete fabrique.",
                        "banniere-erreur", "banniere-erreur-texte"
                );
            } else {
                afficherResultat(
                        "Numero authentique - c'est une lunette de type " + reponse.trim() + ".",
                        "banniere-succes", "banniere-succes-texte"
                );
            }
        }));
    }

    private void afficherResultat(String texte, String classeBanniere, String classeTexte) {
        resultat.setText(texte);
        resultat.getStyleClass().setAll(classeBanniere, classeTexte);
        resultat.setVisible(true);
        resultat.setManaged(true);
    }
}
