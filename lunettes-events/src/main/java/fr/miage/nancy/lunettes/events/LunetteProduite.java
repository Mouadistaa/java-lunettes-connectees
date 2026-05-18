package fr.miage.nancy.lunettes.events;

import java.util.Objects;

/**
 * Représente une lunette effectivement produite par l'usine,
 * avec son type et son numéro de série généré par le fabricateur.
 *
 * <p>Le format du numéro de série est imposé par le fabricateur
 * (deux premières lettres du type, identifiant pseudo-aléatoire,
 * somme de contrôle). La validité formelle d'un numéro de série
 * n'est vérifiable qu'en interrogeant le fabricateur lui-même via
 * sa méthode {@code validateSerial}, ce que ce module ne peut pas
 * faire sans rompre son indépendance vis-à-vis du fabricateur.</p>
 *
 * <p>Ce record se contente donc de garantir que les champs ne
 * sont ni {@code null} ni vides : la vérification cryptographique
 * est déléguée à la couche qui dispose du fabricateur.</p>
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