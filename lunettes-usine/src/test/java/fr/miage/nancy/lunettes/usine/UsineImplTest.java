package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsineImplTest {

    @Test
    void produitUneLunetteUnique() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        List<Fabricateur.Lunette> resultat = usine.produire(
                Map.of(Fabricateur.TypeLunette.BANANA, 1));

        assertEquals(1, resultat.size());
        assertEquals(Fabricateur.TypeLunette.BANANA, resultat.getFirst().type);
        assertNotNull(resultat.getFirst().serial);
    }

    @Test
    void produitPlusieursLunettesDuMemeType() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        List<Fabricateur.Lunette> resultat = usine.produire(
                Map.of(Fabricateur.TypeLunette.CLAUDE, 3));

        assertEquals(3, resultat.size());
        assertTrue(resultat.stream()
                .allMatch(l -> l.type == Fabricateur.TypeLunette.CLAUDE));
    }

    @Test
    void produitUnMelangeDeTypes() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        List<Fabricateur.Lunette> resultat = usine.produire(Map.of(
                Fabricateur.TypeLunette.BANANA, 2,
                Fabricateur.TypeLunette.CLAUDE, 1
        ));

        assertEquals(3, resultat.size());
        long banana = resultat.stream()
                .filter(l -> l.type == Fabricateur.TypeLunette.BANANA)
                .count();
        long claude = resultat.stream()
                .filter(l -> l.type == Fabricateur.TypeLunette.CLAUDE)
                .count();
        assertEquals(2, banana);
        assertEquals(1, claude);
    }

    @Test
    void commandeVideEstRejetee() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        assertThrows(UsineException.class, () -> usine.produire(Map.of()));
    }

    @Test
    void commandeAvecQuantiteZeroPourTousLesTypesEstRejetee() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        Map<Fabricateur.TypeLunette, Integer> commande = Map.of(
                Fabricateur.TypeLunette.BANANA, 0,
                Fabricateur.TypeLunette.CLAUDE, 0
        );

        assertThrows(UsineException.class, () -> usine.produire(commande));
    }

    @Test
    void commandeDepassantLaCapaciteEstRejetee() {
        // capacité 3, commande de 4 lunettes : doit échouer pour l'instant
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        assertThrows(UsineException.class, () -> usine.produire(
                Map.of(Fabricateur.TypeLunette.BANANA, 4)));
    }

    @Test
    void commandeExacteAvantCapaciteFonctionne() {
        // limite haute : capacité 3, commande de 3 lunettes, doit passer
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        List<Fabricateur.Lunette> resultat = usine.produire(
                Map.of(Fabricateur.TypeLunette.BANANA, 3));

        assertEquals(3, resultat.size());
    }

    @Test
    void usineRefuseFabricateurNull() {
        assertThrows(NullPointerException.class,
                () -> new UsineImpl(null));
    }

    @Test
    void produireRefuseCommandeNull() {
        Usine usine = new UsineImpl(new FakeFabricateur(5));

        assertThrows(NullPointerException.class,
                () -> usine.produire(null));
    }
}