package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.List;
import java.util.Map;

/**
 * Contrat pour une usine capable de produire des lunettes à partir
 * d'une commande agrégée par type.
 */
public interface Usine extends AutoCloseable {

    /**
     * Lance la production de lunettes. Chaque entrée de la Map
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