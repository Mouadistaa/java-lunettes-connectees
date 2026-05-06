package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implémentation de référence de l'usine, s'appuyant sur le
 * fabricateur tiers fourni par le sujet.
 */
public final class UsineImpl implements Usine {

    private final Fabricateur fabricateur;

    public UsineImpl(Fabricateur fabricateur) {
        this.fabricateur = Objects.requireNonNull(fabricateur, "fabricateur ne peut pas être null");
    }

    @Override
    public List<Fabricateur.Lunette> produire(final Map<Fabricateur.TypeLunette, Integer> typesLunettes) {
        Objects.requireNonNull(typesLunettes, "typesLunettes ne peut pas être null");

        List<Fabricateur.TypeLunette> aFabriquer = aplatir(typesLunettes);
        if (aFabriquer.isEmpty()) {
            throw new UsineException("commande vide : aucune lunette à produire");
        }

        return produireParBatch(aFabriquer);
    }

    // aplatit la commande agrégée en une liste de types individuels
    private List<Fabricateur.TypeLunette> aplatir(Map<Fabricateur.TypeLunette, Integer> typesLunettes) {
        List<Fabricateur.TypeLunette> resultat = new ArrayList<>();
        for (Map.Entry<Fabricateur.TypeLunette, Integer> entry : typesLunettes.entrySet()) {
            int quantite = entry.getValue();
            for (int i = 0; i < quantite; i++) {
                resultat.add(entry.getKey());
            }
        }
        return resultat;
    }

    /**
     * Découpe la liste à fabriquer en batchs de taille au plus égale à
     * la capacité du fabricateur, et fabrique chaque batch
     * séquentiellement.
     */
    private List<Fabricateur.Lunette> produireParBatch(List<Fabricateur.TypeLunette> aFabriquer) {
        int capacite = fabricateur.getCapacity();
        List<Fabricateur.Lunette> resultat = new ArrayList<>(aFabriquer.size());

        for (int debut = 0; debut < aFabriquer.size(); debut += capacite) {
            int fin = Math.min(debut + capacite, aFabriquer.size());
            List<Fabricateur.TypeLunette> batch = aFabriquer.subList(debut, fin);
            resultat.addAll(produireBatch(batch));
        }

        return resultat;
    }

    /**
     * Configure le fabricateur avec les emplacements du batch et
     * fabrique les lunettes correspondantes.
     */
    private List<Fabricateur.Lunette> produireBatch(List<Fabricateur.TypeLunette> batch) {
        configurerFabricateur(batch);
        return fabriquer(batch);
    }

    private void configurerFabricateur(List<Fabricateur.TypeLunette> batch) {
        Fabricateur.TypeLunette[] types = batch.toArray(new Fabricateur.TypeLunette[0]);
        try {
            fabricateur.configurer(types);
        } catch (Exception e) {
            throw new UsineException("échec de configuration du fabricateur", e);
        }
    }

    private List<Fabricateur.Lunette> fabriquer(List<Fabricateur.TypeLunette> batch) {
        List<Fabricateur.Lunette> resultat = new ArrayList<>(batch.size());
        for (Fabricateur.TypeLunette type : batch) {
            try {
                resultat.add(fabricateur.fabriquer(type));
            } catch (IllegalArgumentException e) {
                throw new UsineException(
                        "le fabricateur a refusé de produire un " + type
                                + " (emplacement non configuré ?)", e);
            } catch (IllegalStateException e) {
                throw new UsineException("capacité du fabricateur dépassée", e);
            }
        }
        return resultat;
    }
}