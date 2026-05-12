package fr.miage.nancy.lunettes.frontend.ui;

import fr.miage.nancy.lunettes.events.TypeLunette;
import fr.miage.nancy.lunettes.frontend.model.Catalogue;
import fr.miage.nancy.lunettes.frontend.model.Produit;
import fr.miage.nancy.lunettes.frontend.service.CommandeService;
import fr.miage.nancy.lunettes.frontend.service.EtatCommande;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Ecran de commande. Affiche le catalogue sous forme de cards 2x2 avec, pour
 * chaque produit, son image, son nom, sa description, son prix et un spinner
 * pour choisir la quantite.
 *
 * <p>En bas, un bandeau persistant rappelle le total et propose le bouton
 * "Valider la commande". Tant qu'aucun article n'est selectionne, le bouton
 * reste desactive. Tant qu'une commande est en cours cote service, l'ecran
 * affiche un bandeau d'info et bloque l'envoi d'une nouvelle commande
 * (contrainte du sujet : une seule commande a la fois par client).</p>
 *
 * <p>La validation envoie la commande puis bascule directement sur l'ecran
 * de suivi pour ne pas perdre l'utilisateur.</p>
 */
public final class EcranCommande extends VBox {

    private static final int QUANTITE_MAX = 9; // <=> [0, 10[ du record Commande

    private final CommandeService service;
    private final Map<TypeLunette, Spinner<Integer>> spinners = new EnumMap<>(TypeLunette.class);
    private final Button btnCommander;
    private final Label labelTotal;
    private final Label labelMessage;

    public EcranCommande(CommandeService service, Runnable allerSuivreCommande) {
        this.service = service;
        getStyleClass().add("contenu-principal");
        setSpacing(20);

        Label titre = new Label("Notre catalogue");
        titre.getStyleClass().add("titre-section");

        Label sousTitre = new Label(
                "Choisis les modeles et leurs quantites (max 9 par modele), "
                        + "puis valide ta commande pour lancer la fabrication."
        );
        sousTitre.getStyleClass().add("label-info");

        // Grille de cards 2x2 -> on encapsule dans un ScrollPane au cas ou
        // la fenetre serait plus petite que prevu
        GridPane grille = new GridPane();
        grille.setHgap(20);
        grille.setVgap(20);
        int col = 0;
        int row = 0;
        for (Produit p : Catalogue.tous()) {
            grille.add(creerCardProduit(p), col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grille);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Bandeau de message (info / erreur), masque par defaut
        labelMessage = new Label("");
        labelMessage.setWrapText(true);
        labelMessage.setMaxWidth(Double.MAX_VALUE);
        labelMessage.setVisible(false);
        labelMessage.setManaged(false);

        // Barre du bas avec le total et le bouton de validation
        labelTotal = new Label("Total : 0,00 EUR");
        labelTotal.getStyleClass().add("card-produit-prix");

        btnCommander = new Button("Valider la commande");
        btnCommander.getStyleClass().add("bouton-primaire");
        btnCommander.setDisable(true);
        btnCommander.setOnAction(e -> envoyerCommande(allerSuivreCommande));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox barreBas = new HBox(16, labelTotal, spacer, btnCommander);
        barreBas.setAlignment(Pos.CENTER_LEFT);
        barreBas.getStyleClass().add("card");
        barreBas.setPadding(new Insets(16, 24, 16, 24));

        // Synchro initiale + ecoute des changements d'etat de la commande
        rafraichirEtat();
        service.etatProperty().addListener((obs, ancien, nouveau) ->
                Platform.runLater(this::rafraichirEtat));

        getChildren().addAll(titre, sousTitre, scroll, labelMessage, barreBas);
    }

    /** Construit la card visuelle d'un produit (image + textes + spinner). */
    private VBox creerCardProduit(Produit produit) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card-produit");
        card.setMinWidth(380);
        card.setMaxWidth(480);

        // Zone image + badge eventuel superpose en haut a droite
        StackPane zoneImage = new StackPane();
        zoneImage.setMinHeight(170);
        zoneImage.setMaxHeight(170);

        try {
            Image image = new Image(
                    getClass().getResourceAsStream(produit.imagePath()),
                    320, 170, true, true
            );
            ImageView img = new ImageView(image);
            img.setPreserveRatio(true);
            img.setFitHeight(170);
            zoneImage.getChildren().add(img);
        } catch (Exception ex) {
            // Fallback : si l'image n'est pas dans le classpath, on affiche un placeholder
            Label fallback = new Label("[image " + produit.imagePath() + " manquante]");
            fallback.getStyleClass().add("label-info");
            zoneImage.getChildren().add(fallback);
        }

        if (produit.aUnBadge()) {
            Label badge = new Label(produit.badge());
            badge.getStyleClass().addAll("badge", styleBadge(produit.badge()));
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8, 8, 0, 0));
            zoneImage.getChildren().add(badge);
        }

        Label nom = new Label(produit.nom());
        nom.getStyleClass().add("card-produit-titre");

        Label desc = new Label(produit.description());
        desc.getStyleClass().add("card-produit-description");
        desc.setWrapText(true);

        Label prix = new Label(formaterPrix(produit.prix()));
        prix.getStyleClass().add("card-produit-prix");

        // Spinner de quantite : on borne entre 0 et 9 pour respecter la regle
        // du record Commande (quantite < 10).
        Spinner<Integer> spinner = new Spinner<>(0, QUANTITE_MAX, 0);
        spinner.setEditable(true);
        spinner.setPrefWidth(110);
        spinner.valueProperty().addListener((obs, ancien, nouveau) -> recalculerTotal());
        // Si l'utilisateur tape n'importe quoi, on revient en arriere proprement
        spinner.getEditor().focusedProperty().addListener((obs, etaitFocus, estFocus) -> {
            if (!estFocus) {
                try {
                    int v = Integer.parseInt(spinner.getEditor().getText());
                    if (v < 0) v = 0;
                    if (v > QUANTITE_MAX) v = QUANTITE_MAX;
                    spinner.getValueFactory().setValue(v);
                } catch (NumberFormatException ignored) {
                    spinner.getEditor().setText(String.valueOf(spinner.getValue()));
                }
            }
        });
        spinners.put(produit.type(), spinner);

        Label labelQte = new Label("Quantite :");
        labelQte.getStyleClass().add("label-info");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox ligneBas = new HBox(12, labelQte, spinner, spacer, prix);
        ligneBas.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(zoneImage, nom, desc, ligneBas);
        return card;
    }

    /** Mappe un badge sur sa classe CSS de couleur (heuristique simple). */
    private String styleBadge(String badge) {
        String b = badge.toLowerCase();
        if (b.contains("%") || b.contains("promo")) return "badge-promo";
        if (b.contains("best")) return "badge-bestseller";
        if (b.contains("nouv") || b.contains("new")) return "badge-nouveau";
        return "badge-default";
    }

    /** Recalcule le total et active/desactive le bouton de validation. */
    private void recalculerTotal() {
        double total = 0;
        int items = 0;
        for (Produit p : Catalogue.tous()) {
            int q = spinners.get(p.type()).getValue();
            total += q * p.prix();
            items += q;
        }
        labelTotal.setText(String.format(
                "Total : %s  (%d article%s)",
                formaterPrix(total), items, items > 1 ? "s" : ""
        ));
        // Bouton actif si on a au moins 1 article ET pas de commande deja en cours
        btnCommander.setDisable(items == 0 || service.aUneCommandeEnCours());
    }

    /** Envoie la commande via le service puis bascule vers l'ecran de suivi. */
    private void envoyerCommande(Runnable allerSuivreCommande) {
        Map<TypeLunette, Integer> lignes = new HashMap<>();
        for (Produit p : Catalogue.tous()) {
            int q = spinners.get(p.type()).getValue();
            if (q > 0) {
                lignes.put(p.type(), q);
            }
        }
        try {
            service.envoyerCommande(lignes);
            allerSuivreCommande.run();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            // Erreurs metier (deja une commande en cours, ou lignes invalides)
            afficherMessage(ex.getMessage(), "banniere-erreur", "banniere-erreur-texte");
        }
    }

    /** Met a jour le bandeau d'info en fonction de l'etat global. */
    private void rafraichirEtat() {
        if (service.aUneCommandeEnCours()) {
            btnCommander.setDisable(true);
            EtatCommande e = service.etatProperty().get();
            afficherMessage(
                    "Une commande est deja en cours (etat : " + e
                            + "). Va dans l'onglet Suivi pour la consulter.",
                    "banniere-info", "banniere-info-texte"
            );
        } else {
            masquerMessage();
            recalculerTotal();
        }
    }

    private void afficherMessage(String texte, String classeBanniere, String classeTexte) {
        labelMessage.setText(texte);
        labelMessage.getStyleClass().setAll(classeBanniere, classeTexte);
        labelMessage.setVisible(true);
        labelMessage.setManaged(true);
    }

    private void masquerMessage() {
        labelMessage.setVisible(false);
        labelMessage.setManaged(false);
    }

    private String formaterPrix(double prix) {
        // Format simple : 89,99 EUR (on evite le symbole € car certaines polices systemes l'ecrasent)
        return String.format("%.2f EUR", prix).replace('.', ',');
    }
}
