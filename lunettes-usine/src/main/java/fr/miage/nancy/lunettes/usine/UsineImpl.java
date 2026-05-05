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
        if (aFabriquer.size() > fabricateur.getCapacity()) {
            throw new UsineException(
                    "commande de " + aFabriquer.size() + " lunettes dépasse la capacité "
                            + "du fabricateur (" + fabricateur.getCapacity() + ") ; "
                            + "le découpage multi-batch n'est pas encore supporté"
            );
        }

        configurerFabricateur(aFabriquer);
        return fabriquer(aFabriquer);
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

    // configure le fabricateur avec les emplacements correspondant à la liste de types à fabriquer
    private void configurerFabricateur(List<Fabricateur.TypeLunette> aFabriquer) {
        Fabricateur.TypeLunette[] types = aFabriquer.toArray(new Fabricateur.TypeLunette[0]);
        try {
            fabricateur.configurer(types);
        } catch (Exception e) {
            throw new UsineException("échec de configuration du fabricateur", e);
        }
    }

    // lance la fabrication séquentielle de chaque lunette demandée
    private List<Fabricateur.Lunette> fabriquer(List<Fabricateur.TypeLunette> aFabriquer) {
        List<Fabricateur.Lunette> resultat = new ArrayList<>(aFabriquer.size());
        for (Fabricateur.TypeLunette type : aFabriquer) {
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