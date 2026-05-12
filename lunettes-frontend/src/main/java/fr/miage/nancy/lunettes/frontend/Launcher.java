package fr.miage.nancy.lunettes.frontend;

/**
 * Classe wrapper qui sert juste de point d'entrée du fat jar.
 *
 * <p>Pourquoi cette classe existe : quand on packagine une appli JavaFX dans un
 * jar non-modulaire (cas typique avec maven-shade-plugin), pointer directement
 * sur {@link MainApp} comme main-class du manifest fait planter le lancement avec :
 * "Error: JavaFX runtime components are missing".</p>
 *
 * <p>Le truc qui contourne le problème : avoir un main qui n'est PAS dans une
 * classe qui étend {@code Application}. On délègue alors à {@code Application.launch()},
 * et tout se passe bien.</p>
 *
 * <p>Bref, c'est juste une indirection technique. Toute la vraie vie de l'app est
 * dans {@link MainApp}.</p>
 */
public final class Launcher {

    private Launcher() {
        // pas instanciable, c'est juste un porte d'entrée
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
