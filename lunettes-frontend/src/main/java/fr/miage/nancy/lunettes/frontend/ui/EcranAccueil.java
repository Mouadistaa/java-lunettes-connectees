package fr.miage.nancy.lunettes.frontend.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Ecran d'accueil. Une hero section minimaliste avec :
 *   - un gros titre accrocheur
 *   - un sous-titre qui explique ce que fait l'app
 *   - deux boutons CTA : commander (primaire), verifier (secondaire)
 *
 * <p>Volontairement sobre, dans l'esprit Apple : peu de texte, beaucoup d'air,
 * un seul message. Pas de carrousel, pas de "features", pas de testimonials.</p>
 */
public final class EcranAccueil extends VBox {

    public EcranAccueil(Runnable allerCommander, Runnable allerVerifier) {
        getStyleClass().add("contenu-principal");
        setAlignment(Pos.CENTER);
        setSpacing(28);
        setPadding(new Insets(80, 64, 80, 64));

        Label titre = new Label("Reinventez votre regard.");
        titre.getStyleClass().add("titre-hero");
        titre.setWrapText(true);
        titre.setMaxWidth(820);

        Label sousTitre = new Label(
                "Quatre modeles iconiques, fabriques sur commande. Choisissez vos lunettes, "
                        + "suivez la fabrication en temps reel, et recevez vos numeros de serie uniques."
        );
        sousTitre.getStyleClass().add("sous-titre-hero");
        sousTitre.setWrapText(true);
        sousTitre.setMaxWidth(700);

        Button btnPrimaire = new Button("Commander maintenant");
        btnPrimaire.getStyleClass().add("bouton-primaire");
        btnPrimaire.setOnAction(e -> allerCommander.run());

        Button btnSecondaire = new Button("Verifier un numero de serie");
        btnSecondaire.getStyleClass().add("bouton-secondaire");
        btnSecondaire.setOnAction(e -> allerVerifier.run());

        HBox boutons = new HBox(16, btnPrimaire, btnSecondaire);
        boutons.setAlignment(Pos.CENTER);

        // Petit espace pour faire respirer entre le sous-titre et les boutons
        Region airEntre = new Region();
        airEntre.setMinHeight(8);

        getChildren().addAll(titre, sousTitre, airEntre, boutons);
    }
}
