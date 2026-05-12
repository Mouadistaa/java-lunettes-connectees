package fr.miage.nancy.lunettes.frontend.ui;

import fr.miage.nancy.lunettes.events.LunetteProduite;
import fr.miage.nancy.lunettes.frontend.service.CommandeService;
import fr.miage.nancy.lunettes.frontend.service.EtatCommande;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.animation.PauseTransition;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;

/**
 * Ecran de suivi de commande + livraison des numeros de serie.
 *
 * <p>Cet ecran a deux phases visuelles :</p>
 * <ul>
 *   <li><strong>Phase 1 - attente</strong> : on affiche un spinner et un label
 *       qui evolue ("Envoi" -> "Validee" -> "Fabrication").</li>
 *   <li><strong>Phase 2 - livre</strong> : on cache le spinner et on affiche
 *       la liste des numeros de serie recus, plus des boutons "nouvelle commande"
 *       et "verifier un n° de serie".</li>
 * </ul>
 *
 * <p>Toute la mecanique est <strong>reactive</strong> : on bind les widgets sur
 * les proprietes du {@link CommandeService}. Quand le service met a jour son
 * etat (apres reception d'un message MQTT), l'UI suit automatiquement.</p>
 *
 * <p>En cas d'echec (commande annulee ou erreur), un bandeau rouge affiche
 * le message d'erreur du backend.</p>
 */
public final class EcranLivraison extends VBox {

    public EcranLivraison(CommandeService service,
                          Runnable allerNouvelleCommande,
                          Runnable allerVerifier) {
        getStyleClass().add("contenu-principal");
        setSpacing(20);

        Label titre = new Label("Suivi de ta commande");
        titre.getStyleClass().add("titre-section");

        // --- Bandeau d'etat (spinner + label dynamique) ---
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(44, 44);

        Label etatTitre = new Label();
        etatTitre.getStyleClass().add("etat-titre");

        Label etatDetail = new Label();
        etatDetail.getStyleClass().add("etat-detail");
        etatDetail.setWrapText(true);

        VBox etatTextes = new VBox(4, etatTitre, etatDetail);
        etatTextes.setAlignment(Pos.CENTER_LEFT);

        HBox bandeauEtat = new HBox(20, spinner, etatTextes);
        bandeauEtat.setAlignment(Pos.CENTER_LEFT);
        bandeauEtat.getStyleClass().add("etat-encours");

        // --- Bandeau d'erreur (visible uniquement si message d'erreur present) ---
        Label banniereErreur = new Label();
        banniereErreur.getStyleClass().addAll("banniere-erreur", "banniere-erreur-texte");
        banniereErreur.setWrapText(true);
        banniereErreur.textProperty().bind(service.messageErreurProperty());
        banniereErreur.visibleProperty().bind(service.messageErreurProperty().isNotEmpty());
        banniereErreur.managedProperty().bind(banniereErreur.visibleProperty());

        // --- Section liste des numeros de serie ---
        Label sousTitreListe = new Label("Numeros de serie de tes lunettes");
        sousTitreListe.getStyleClass().add("label-fort");
        sousTitreListe.visibleProperty().bind(Bindings.isNotEmpty(service.lunettesLivrees()));
        sousTitreListe.managedProperty().bind(sousTitreListe.visibleProperty());

        ListView<LunetteProduite> liste = new ListView<>(service.lunettesLivrees());
        liste.getStyleClass().add("list-view-livraison");
        liste.setCellFactory(lv -> new ListCell<>() {

            // On construit la cellule une seule fois pour eviter de recreer
            // les composants a chaque update (perf + memoire)
            private final Label texteSerie = new Label();
            private final Button btnCopier = new Button("Copier le numéro de série");
            private final Region pousseADroite = new Region();
            private final HBox conteneur = new HBox(12, texteSerie, pousseADroite, btnCopier);

            {
                // Bloc d'initialisation : pousse le bouton tout a droite
                HBox.setHgrow(pousseADroite, Priority.ALWAYS);
                conteneur.setAlignment(Pos.CENTER_LEFT);
                btnCopier.getStyleClass().add("bouton-copier");

                // Action du bouton : copie dans le presse-papier + feedback "Copie !"
                btnCopier.setOnAction(e -> {
                    LunetteProduite item = getItem();
                    if (item != null) {
                        // 1. Copie effective dans le clipboard systeme
                        ClipboardContent contenu = new ClipboardContent();
                        contenu.putString(item.numeroSerie());
                        Clipboard.getSystemClipboard().setContent(contenu);

                        // 2. Feedback visuel : le bouton dit "Copie !" pendant 1.5s
                        btnCopier.setText("Copié !");
                        PauseTransition delai = new PauseTransition(Duration.seconds(1.5));
                        delai.setOnFinished(ev -> btnCopier.setText("Copier le numéro de série"));
                        delai.play();
                    }
                });
            }

            @Override
            protected void updateItem(LunetteProduite item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Important : setText(null) + setGraphic(...) pour que notre
                    // composant custom (HBox avec bouton) soit affiche a la place
                    // du texte simple par defaut.
                    setText(null);
                    texteSerie.setText(String.format("%-10s ->  %s",
                            item.type().name(), item.numeroSerie()));
                    setGraphic(conteneur);
                }
            }
        });
        liste.setPrefHeight(280);
        liste.visibleProperty().bind(Bindings.isNotEmpty(service.lunettesLivrees()));
        liste.managedProperty().bind(liste.visibleProperty());
        VBox.setVgrow(liste, Priority.SOMETIMES);

        // --- Boutons d'action (visibles uniquement quand la commande est terminale) ---
        Button btnNouvelle = new Button("Passer une nouvelle commande");
        btnNouvelle.getStyleClass().add("bouton-primaire");
        btnNouvelle.setOnAction(e -> allerNouvelleCommande.run());

        Button btnVerifier = new Button("Verifier un numero de serie");
        btnVerifier.getStyleClass().add("bouton-secondaire");
        btnVerifier.setOnAction(e -> allerVerifier.run());

        HBox boutons = new HBox(12, btnNouvelle, btnVerifier);
        boutons.setAlignment(Pos.CENTER_LEFT);

        // --- Mise a jour reactive du bandeau d'etat ---
        Runnable rafraichirEtat = () -> {
            EtatCommande etat = service.etatProperty().get();
            switch (etat) {
                case CREEE, ENVOYEE -> {
                    etatTitre.setText("Envoi en cours...");
                    etatDetail.setText("Ta commande est en cours de transmission au backend.");
                    afficher(spinner, true);
                    afficher(boutons, false);
                }
                case VALIDEE -> {
                    etatTitre.setText("Commande validee");
                    etatDetail.setText("Le backend a accepte ta commande. La fabrication va demarrer dans un instant.");
                    afficher(spinner, true);
                    afficher(boutons, false);
                }
                case EN_FABRICATION -> {
                    etatTitre.setText("Fabrication en cours...");
                    etatDetail.setText("L'usine est en train de produire tes lunettes. Patiente quelques secondes.");
                    afficher(spinner, true);
                    afficher(boutons, false);
                }
                case LIVREE -> {
                    int n = service.lunettesLivrees().size();
                    etatTitre.setText("Livraison terminee");
                    etatDetail.setText(n + " lunette" + (n > 1 ? "s ont " : " a ") + "ete fabriquee" + (n > 1 ? "s " : " ") + "et te sont livree" + (n > 1 ? "s" : "") + " ci-dessous.");
                    afficher(spinner, false);
                    afficher(boutons, true);
                }
                case ANNULEE -> {
                    etatTitre.setText("Commande annulee");
                    etatDetail.setText("Le backend a refuse ta commande (validation metier).");
                    afficher(spinner, false);
                    afficher(boutons, true);
                }
                case EN_ERREUR -> {
                    etatTitre.setText("Erreur de fabrication");
                    etatDetail.setText("Une erreur s'est produite cote backend pendant la fabrication.");
                    afficher(spinner, false);
                    afficher(boutons, true);
                }
            }
        };
        rafraichirEtat.run();
        service.etatProperty().addListener((o, a, n) -> Platform.runLater(rafraichirEtat));

        // --- Composition finale ---
        Region petitAir = new Region();
        petitAir.setMinHeight(4);

        getChildren().addAll(
                titre,
                bandeauEtat,
                banniereErreur,
                petitAir,
                sousTitreListe,
                liste,
                boutons
        );
    }

    private void afficher(Region node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
