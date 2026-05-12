package fr.miage.nancy.lunettes.frontend.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * La barre de navigation en haut de la fenetre.
 *
 * <p>Trois liens cliquables (accueil n'est pas dedans pour eviter la redondance
 * avec le logo) plus un titre/logo a gauche. Le lien correspondant a l'ecran
 * courant est mis en evidence visuellement.</p>
 */
public final class BarreNavigation extends HBox {

    private final Map<Navigation.Ecran, Button> liens = new EnumMap<>(Navigation.Ecran.class);

    /**
     * @param actionLogo       click sur le titre = retour accueil
     * @param actionCommande   click sur "Commander"
     * @param actionLivraison  click sur "Suivi" (utile pour revenir voir une commande en cours)
     * @param actionVerification click sur "Verification"
     */
    public BarreNavigation(Runnable actionLogo,
                           Runnable actionCommande,
                           Runnable actionLivraison,
                           Runnable actionVerification) {
        getStyleClass().add("barre-nav");

        Label logo = new Label("Lunettes connectees");
        logo.getStyleClass().add("logo");
        logo.setOnMouseClicked(e -> actionLogo.run());
        logo.setStyle("-fx-cursor: hand;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCommande = creerLien("Commander", actionCommande, Navigation.Ecran.COMMANDE);
        Button btnLivraison = creerLien("Suivi de commande", actionLivraison, Navigation.Ecran.LIVRAISON);
        Button btnVerification = creerLien("Verification", actionVerification, Navigation.Ecran.VERIFICATION);

        getChildren().addAll(logo, spacer, btnCommande, btnLivraison, btnVerification);
    }

    private Button creerLien(String texte, Runnable action, Navigation.Ecran ecran) {
        Button b = new Button(texte);
        b.getStyleClass().add("lien-nav");
        b.setOnAction(e -> action.run());
        liens.put(ecran, b);
        return b;
    }

    /** Met le lien de l'ecran donne en style "actif", les autres en style normal. */
    public void surlignerEcran(Navigation.Ecran ecranCourant) {
        liens.forEach((ecran, lien) -> {
            lien.getStyleClass().remove("actif");
            if (ecran == ecranCourant) {
                lien.getStyleClass().add("actif");
            }
        });
    }
}
