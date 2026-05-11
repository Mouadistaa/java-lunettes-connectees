package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implémentation de référence de l'usine, s'appuyant sur le
 * fabricateur tiers fourni par le sujet.
 */
public final class UsineImpl implements Usine {

    private final Fabricateur fabricateur;
    private final ExecutorService executor;

    /**
     * Verrou interne pour sérialiser les blocs configurer+fabriquer entre
     * commandes concurrentes. On ne peut pas verrouiller sur le fabricateur
     * lui-même : il synchronise déjà ses propres méthodes en interne
     */
    private final Object batchLock = new Object();

    public UsineImpl(Fabricateur fabricateur) {
        this.fabricateur = Objects.requireNonNull(fabricateur, "fabricateur ne peut pas être null");
        this.executor = Executors.newFixedThreadPool(fabricateur.getCapacity());
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

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

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
            synchronized (batchLock) {                  // ← verrou interne, distinct de fabricateur
                resultat.addAll(produireBatch(batch));
            }
        }

        return resultat;
    }

    /**
     * Configure le fabricateur avec les emplacements du batch et
     * fabrique les lunettes correspondantes.
     */
    private List<Fabricateur.Lunette> produireBatch(List<Fabricateur.TypeLunette> batch) {
        configurerFabricateur(batch);
        return fabriquerEnParallele(batch);
    }

    private void configurerFabricateur(List<Fabricateur.TypeLunette> batch) {
        Fabricateur.TypeLunette[] types = batch.toArray(new Fabricateur.TypeLunette[0]);
        try {
            fabricateur.configurer(types);
        } catch (Exception e) {
            throw new UsineException("échec de configuration du fabricateur", e);
        }
    }

    /**
     * Soumet une tâche de fabrication par lunette du batch au pool de
     * threads, puis attend la complétion de toutes les tâches avant de
     * retourner les résultats agrégés. Si une tâche lève une exception,
     * elle est wrappée dans une UsineException.
     */
    private List<Fabricateur.Lunette> fabriquerEnParallele(List<Fabricateur.TypeLunette> batch) {
        List<Future<Fabricateur.Lunette>> futures = new ArrayList<>(batch.size());
        for (Fabricateur.TypeLunette type : batch) {
            futures.add(executor.submit(() -> fabricateur.fabriquer(type)));
        }

        List<Fabricateur.Lunette> resultat = new ArrayList<>(batch.size());
        for (Future<Fabricateur.Lunette> future : futures) {
            try {
                resultat.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new UsineException("fabrication interrompue", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IllegalArgumentException) {
                    throw new UsineException(
                            "le fabricateur a refusé une fabrication "
                                    + "(emplacement non configuré ?)", cause);
                }
                if (cause instanceof IllegalStateException) {
                    throw new UsineException("capacité du fabricateur dépassée", cause);
                }
                throw new UsineException("erreur inattendue pendant la fabrication", cause);
            }
        }
        return resultat;
    }
}