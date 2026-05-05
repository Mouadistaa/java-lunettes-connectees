package fr.miage.nancy.lunettes.events;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Représente une commande de lunettes émise par un client.
 *
 * Une commande est identifiée par un UUID unique et porte la liste
 * des types souhaités avec leur quantité.
 */
public record Commande(UUID id, Map<TypeLunette, Integer> lignes) {

    private static final int QUANTITE_MAX_PAR_TYPE = 10;

    public Commande {
        Objects.requireNonNull(id, "id ne peut pas être null");
        Objects.requireNonNull(lignes, "lignes ne peut pas être null");

        // la commande devient indépendante du Map d'origine
        lignes = Map.copyOf(lignes);

        // règle : chaque quantité dans [0, 10[
        for (Map.Entry<TypeLunette, Integer> entry : lignes.entrySet()) {
            int quantite = entry.getValue();
            if (quantite < 0 || quantite >= QUANTITE_MAX_PAR_TYPE) {
                throw new IllegalArgumentException(
                        "Quantité invalide pour " + entry.getKey()
                                + " : " + quantite + " (attendu dans [0, " + QUANTITE_MAX_PAR_TYPE + "[)"
                );
            }
        }

        // règle : quantité totale > 0
        int total = lignes.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            throw new IllegalArgumentException(
                    "La quantité totale doit être strictement positive (reçu : " + total + ")"
            );
        }
    }
}