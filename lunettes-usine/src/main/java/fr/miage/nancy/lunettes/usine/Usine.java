package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.List;
import java.util.Map;

/**
 * Contrat pour une usine capable de produire des lunettes à partir
 * d'une commande agrégée par type.
 *
 * <p>La signature de cette interface est imposée par le sujet du projet,
 * qui exige le prototype exact :</p>
 *
 * <pre>{@code
 * List<Lunette> produire(final Map<TypeLunette, Integer> typesLunettes);
 * }</pre>
 *
 * <p>où {@code Lunette} et {@code TypeLunette} désignent respectivement
 * {@link Fabricateur.Lunette} et {@link Fabricateur.TypeLunette}. Cette
 * contrainte fait que l'Usine est volontairement couplée aux types du
 * fabricateur tiers : c'est le contrat externe attendu par le sujet.</p>
 *
 * <p>Le serveur MQTT, qui consommera cette interface, fera la conversion
 * entre nos types de domaine ({@code lunettes-events}) et les types du
 * fabricateur à la frontière, en s'appuyant sur {@link TypeMapper}.</p>
 *
 * <p>Le sujet pose la question « Comment gérer plusieurs usines ? ».
 * L'existence de cette interface fournit la première brique de réponse :
 * on peut imaginer une implémentation distribuée qui fédère plusieurs
 * usines, sans que le code appelant n'ait à le savoir.</p>
 */
public interface Usine extends AutoCloseable {

    /**
     * Lance la production de lunettes. Chaque entrée de la {@code Map}
     * associe au type de lunette la quantité qu'il faut en produire.
     *
     * @param typesLunettes description agrégée des lunettes à produire
     * @return la liste des lunettes effectivement produites,
     *         dans un ordre non garanti
     * @throws UsineException si la commande est invalide ou si
     *         le mécanisme de fabrication échoue
     */
    List<Fabricateur.Lunette> produire(final Map<Fabricateur.TypeLunette, Integer> typesLunettes);

    // libère les ressources détenues par l'Usine (pool de threads, etc.).
    @Override
    void close();
}