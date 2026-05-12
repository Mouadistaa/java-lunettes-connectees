package fr.miage.nancy.lunettes.frontend.ui;

import javafx.animation.FadeTransition;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Gere le switch d'ecrans dans l'application.
 *
 * <p>L'idee : la fenetre a un layout fixe (barre de nav + zone centrale), seul
 * le contenu de la zone centrale change quand on navigue. C'est plus propre que
 * de recreer la Scene a chaque fois et ca permet de garder l'etat de la barre
 * de nav coherent (mise en evidence du lien actif).</p>
 *
 * <p>On ajoute aussi une petite animation de fondu en arrivant sur un nouvel
 * ecran. Subtil, mais ca rend l'app moins seche qu'un switch brutal.</p>
 */
public final class Navigation {

    /** Identifiants des ecrans pour mettre en surbrillance le bon lien. */
    public enum Ecran {
        ACCUEIL, COMMANDE, LIVRAISON, VERIFICATION
    }

    private final BorderPane racine;
    private final BarreNavigation barreNav;

    public Navigation(BorderPane racine, BarreNavigation barreNav) {
        this.racine = Objects.requireNonNull(racine);
        this.barreNav = Objects.requireNonNull(barreNav);
    }

    /**
     * Affiche un nouveau contenu central avec une transition de fondu.
     *
     * @param contenu      la vue a afficher (ex : la racine de l'ecran)
     * @param ecranCourant l'identifiant de l'ecran (pour la barre de nav)
     */
    public void afficher(Region contenu, Ecran ecranCourant) {
        racine.setCenter(contenu);
        barreNav.surlignerEcran(ecranCourant);
        // Petite anim de fade-in : passe de 0 a 1 d'opacite en 250ms
        FadeTransition fade = new FadeTransition(Duration.millis(250), contenu);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }
}
