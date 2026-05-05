package fr.miage.nancy.lunettes.events;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandeTest {

    @Test
    void commandeValideSeConstruit() {
        UUID id = UUID.randomUUID();
        Map<TypeLunette, Integer> lignes = Map.of(
                TypeLunette.BANANA, 3,
                TypeLunette.CLAUDE, 1
        );

        Commande commande = new Commande(id, lignes);

        assertEquals(id, commande.id());
        assertEquals(lignes, commande.lignes());
    }

    @Test
    void idNullEstRejete() {
        Map<TypeLunette, Integer> lignes = Map.of(TypeLunette.BANANA, 1);

        assertThrows(NullPointerException.class,
                () -> new Commande(null, lignes));
    }

    @Test
    void lignesNullSontRejetees() {
        UUID id = UUID.randomUUID();

        assertThrows(NullPointerException.class,
                () -> new Commande(id, null));
    }

    @Test
    void quantiteNegativeEstRejetee() {
        UUID id = UUID.randomUUID();
        Map<TypeLunette, Integer> lignes = Map.of(TypeLunette.BANANA, -1);

        assertThrows(IllegalArgumentException.class,
                () -> new Commande(id, lignes));
    }

    @Test
    void quantiteEgaleA10EstRejetee() {
        // la borne supérieure est exclue : 10 n'est pas autorisé
        UUID id = UUID.randomUUID();
        Map<TypeLunette, Integer> lignes = Map.of(TypeLunette.CLAUDE, 10);

        assertThrows(IllegalArgumentException.class,
                () -> new Commande(id, lignes));
    }

    @Test
    void commandeAvecQuantiteTotaleNulleEstRejetee() {
        // une commande qui ne demande rien n'a pas de sens
        UUID id = UUID.randomUUID();
        Map<TypeLunette, Integer> lignes = Map.of(
                TypeLunette.BANANA, 0,
                TypeLunette.CLAUDE, 0
        );

        assertThrows(IllegalArgumentException.class,
                () -> new Commande(id, lignes));
    }

    @Test
    void commandeEstImmutableMemeSiLaMapSourceEstModifiee() {
        UUID id = UUID.randomUUID();
        Map<TypeLunette, Integer> lignesMutables = new HashMap<>();
        lignesMutables.put(TypeLunette.BANANA, 2);

        Commande commande = new Commande(id, lignesMutables);

        // l'appelant modifie sa map après construction de la commande :
        // la commande ne doit pas être affectée.
        lignesMutables.put(TypeLunette.CLAUDE, 5);

        assertEquals(1, commande.lignes().size());
        assertEquals(2, commande.lignes().get(TypeLunette.BANANA));
    }
}