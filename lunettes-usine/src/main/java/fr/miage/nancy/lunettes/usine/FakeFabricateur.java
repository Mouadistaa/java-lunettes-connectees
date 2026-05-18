package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub du {@link Fabricateur} utilisé dans les tests de l'Usine.
 *
 * <p>Ce fake reproduit les contrats observables du vrai fabricateur
 * (capacité fixe, refus de produire un type non configuré, comptage
 * des fabrications en cours) sans la latence ({@code Thread.sleep})
 * ni le non-déterminisme (capacité aléatoire).</p>
 *
 * <p>Il s'agit d'une <em>fake implementation</em> écrite à la main
 * plutôt qu'un mock généré par une librairie : Mockito serait une
 * dépendance lourde pour ce seul besoin, et un stub explicite est
 * plus lisible et plus pédagogique.</p>
 *
 * <p>Le stub étend la classe concrète {@link Fabricateur} parce que
 * cette dernière ne dérive pas d'une interface : c'est l'option
 * standard quand on doit substituer une dépendance externe non
 * conçue pour le testing.</p>
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