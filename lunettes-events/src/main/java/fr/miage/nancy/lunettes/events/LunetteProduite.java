package fr.miage.nancy.lunettes.events;

import java.util.Objects;

/**
 * Représente une lunette effectivement produite par l'usine,
 * avec son type et son numéro de série généré par le fabricateur.
 */
public record LunetteProduite(TypeLunette type, String numeroSerie) {

    public LunetteProduite {
        Objects.requireNonNull(type, "type ne peut pas être null");
        Objects.requireNonNull(numeroSerie, "numeroSerie ne peut pas être null");
        if (numeroSerie.isBlank()) {
            throw new IllegalArgumentException("numeroSerie ne peut pas être vide");
        }
    }
}