package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;
import fr.miage.nancy.lunettes.events.TypeLunette;

/**
 * Convertit les types de lunettes entre la représentation utilisée par
 * le module {@code lunettes-events} (notre domaine) et celle utilisée
 * par le fabricateur tiers.
 *
 * <p>Le mapping est explicite (via un {@code switch}) plutôt qu'implicite
 * (par alignement de noms via {@code valueOf(name())}). Ce choix est
 * volontaire :</p>
 * <ul>
 *   <li>Si l'enum du fabricateur évolue (renommage, ajout, suppression),
 *       le compilateur signale immédiatement le problème — alors qu'un
 *       mapping par nom le manifesterait seulement à l'exécution.</li>
 *   <li>Le découplage entre nos types et ceux du fabricateur est rendu
 *       visible dans le code, conformément au principe de mapping
 *       explicite entre modèles de données indépendants.</li>
 * </ul>
 */
public final class TypeMapper {

    private TypeMapper() {
        // classe utilitaire, pas d'instanciation
    }

    // convertit un type de lunette du domaine events vers celui du fabricateur
    public static Fabricateur.TypeLunette toFabricateur(TypeLunette type) {
        return switch (type) {
            case BANANA -> Fabricateur.TypeLunette.BANANA;
            case CHATGPT -> Fabricateur.TypeLunette.CHATGPT;
            case LE_CHAT -> Fabricateur.TypeLunette.LE_CHAT;
            case CLAUDE -> Fabricateur.TypeLunette.CLAUDE;
        };
    }

    // convertit un type de lunette retourné par le fabricateur vers le type du domaine events
    public static TypeLunette fromFabricateur(Fabricateur.TypeLunette type) {
        return switch (type) {
            case BANANA -> TypeLunette.BANANA;
            case CHATGPT -> TypeLunette.CHATGPT;
            case LE_CHAT -> TypeLunette.LE_CHAT;
            case CLAUDE -> TypeLunette.CLAUDE;
        };
    }
}