package fr.miage.nancy.lunettes.frontend.model;

import fr.miage.nancy.lunettes.events.TypeLunette;

import java.util.Objects;

/**
 * Représente un produit affichable dans le catalogue du frontend.
 *
 * <p>C'est l'équivalent côté UI du {@link TypeLunette}, enrichi de tout ce qu'il
 * faut pour faire un joli rendu : nom commercial, description, prix, badge
 * marketing, et chemin de l'image.</p>
 *
 * <p>Cette classe vit côté frontend uniquement : elle n'a pas à fuiter vers le
 * backend, qui ne connaît que des {@link TypeLunette}.</p>
 *
 * @param type        le type technique (clé envoyée au backend)
 * @param nom         le nom commercial affiché à l'écran
 * @param description un baratin marketing court
 * @param prix        le prix unitaire en euros (utilisé pour l'affichage du total)
 * @param badge       texte de badge optionnel (ex : "Bestseller"), {@code ""} si aucun
 * @param imagePath   chemin de l'image dans le classpath (ex : "/images/banana.png")
 */
public record Produit(
        TypeLunette type,
        String nom,
        String description,
        double prix,
        String badge,
        String imagePath
) {

    public Produit {
        Objects.requireNonNull(type, "type ne peut pas etre null");
        Objects.requireNonNull(nom, "nom ne peut pas etre null");
        Objects.requireNonNull(description, "description ne peut pas etre null");
        Objects.requireNonNull(imagePath, "imagePath ne peut pas etre null");
        if (prix < 0) {
            throw new IllegalArgumentException("le prix ne peut pas etre negatif");
        }
        // badge peut etre "" mais pas null -> on tolere le null en le ramenant a ""
        if (badge == null) {
            badge = "";
        }
    }

    /** Vrai si le produit affiche un badge marketing (utilisé par la vue pour l'afficher ou non). */
    public boolean aUnBadge() {
        return !badge.isBlank();
    }
}
