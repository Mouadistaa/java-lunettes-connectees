package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub du Fabricateur utilisé dans les tests de l'Usine
 */
class FakeFabricateur extends Fabricateur {

    private final int capacity;
    private final AtomicInteger enCours = new AtomicInteger();
    private List<TypeLunette> emplacements = new ArrayList<>();
    private final AtomicInteger compteur = new AtomicInteger();

    FakeFabricateur(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void configurer(TypeLunette[] types) {
        this.emplacements = new ArrayList<>(Arrays.asList(types));
        this.compteur.set(0);
    }

    @Override
    public Lunette fabriquer(TypeLunette type) {
        if (enCours.get() >= capacity) {
            throw new IllegalStateException("trop de lunettes en cours de fabrication");
        }
        if (!emplacements.remove(type)) {
            throw new IllegalArgumentException("aucun emplacement configuré pour " + type);
        }
        enCours.incrementAndGet();
        try {
            String serial = type.name().substring(0, 2)
                    + "-FAKE" + compteur.incrementAndGet()
                    + "-DEADBEEF";
            return new Lunette(type, serial);
        } finally {
            enCours.decrementAndGet();
        }
    }
}