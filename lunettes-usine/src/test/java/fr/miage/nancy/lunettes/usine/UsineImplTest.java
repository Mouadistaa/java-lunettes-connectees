package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class UsineImplTest {

    // tests simples

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
    void commandeDepassantLaCapaciteEstDecoupeeEnBatchs() {
        // capacité 3, commande de 4 lunettes : doit fonctionner via 2 batchs (3+1)
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        List<Fabricateur.Lunette> resultat = usine.produire(
                Map.of(Fabricateur.TypeLunette.BANANA, 4));

        assertEquals(4, resultat.size());
        assertTrue(resultat.stream()
                .allMatch(l -> l.type == Fabricateur.TypeLunette.BANANA));
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

    // tests multi batch

    @Test
    void produitExactementDeuxFoisLaCapacite() {
        // capacité 3, commande de 6 lunettes : 2 batchs pleins
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        List<Fabricateur.Lunette> resultat = usine.produire(
                Map.of(Fabricateur.TypeLunette.BANANA, 6));

        assertEquals(6, resultat.size());
        assertTrue(resultat.stream()
                .allMatch(l -> l.type == Fabricateur.TypeLunette.BANANA));
    }

    @Test
    void produitCommandeAvecBatchFinalPartiel() {
        // capacité 4, commande de 7 lunettes : un batch de 4 + un batch de 3
        Usine usine = new UsineImpl(new FakeFabricateur(4));

        List<Fabricateur.Lunette> resultat = usine.produire(Map.of(
                Fabricateur.TypeLunette.BANANA, 4,
                Fabricateur.TypeLunette.CLAUDE, 3
        ));

        assertEquals(7, resultat.size());
        long banana = resultat.stream()
                .filter(l -> l.type == Fabricateur.TypeLunette.BANANA).count();
        long claude = resultat.stream()
                .filter(l -> l.type == Fabricateur.TypeLunette.CLAUDE).count();
        assertEquals(4, banana);
        assertEquals(3, claude);
    }

    @Test
    void produitGrosseCommandeMultiTypes() {
        // capacité 3, commande de 9 lunettes mélangées : 3 batchs
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        List<Fabricateur.Lunette> resultat = usine.produire(Map.of(
                Fabricateur.TypeLunette.BANANA, 3,
                Fabricateur.TypeLunette.CHATGPT, 3,
                Fabricateur.TypeLunette.CLAUDE, 3
        ));

        assertEquals(9, resultat.size());
    }

    // tests concurrence

    @Test
    void deuxCommandesConcurrentesProduisentToutesLeursLunettes() throws Exception {
        // deux threads externes qui appellent produire() simultanément
        // ne doivent pas interférer entre eux
        Usine usine = new UsineImpl(new FakeFabricateur(4));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<List<Fabricateur.Lunette>> commandeA = () -> usine.produire(
                    Map.of(Fabricateur.TypeLunette.BANANA, 3));
            Callable<List<Fabricateur.Lunette>> commandeB = () -> usine.produire(
                    Map.of(Fabricateur.TypeLunette.CLAUDE, 2));

            Future<List<Fabricateur.Lunette>> futureA = pool.submit(commandeA);
            Future<List<Fabricateur.Lunette>> futureB = pool.submit(commandeB);

            List<Fabricateur.Lunette> resultatA = futureA.get(10, TimeUnit.SECONDS);
            List<Fabricateur.Lunette> resultatB = futureB.get(10, TimeUnit.SECONDS);

            assertEquals(3, resultatA.size());
            assertTrue(resultatA.stream()
                    .allMatch(l -> l.type == Fabricateur.TypeLunette.BANANA));
            assertEquals(2, resultatB.size());
            assertTrue(resultatB.stream()
                    .allMatch(l -> l.type == Fabricateur.TypeLunette.CLAUDE));
        } finally {
            pool.shutdown();
        }
    }

    // test cycle de vie
    @Test
    void closeLibereProprementLeFabricateur() {
        Usine usine = new UsineImpl(new FakeFabricateur(3));

        // Une production normale doit fonctionner avant close
        usine.produire(Map.of(Fabricateur.TypeLunette.BANANA, 1));

        // close ne doit pas planter
        usine.close();

        // close peut être appelé plusieurs fois sans plantage
        usine.close();
    }
}