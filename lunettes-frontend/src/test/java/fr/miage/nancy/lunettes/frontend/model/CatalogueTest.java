package fr.miage.nancy.lunettes.frontend.model;

import fr.miage.nancy.lunettes.events.TypeLunette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du modele catalogue.
 *
 * <p>L'idee : verifier que le catalogue colle bien a l'enum {@link TypeLunette} et
 * que les invariants du record {@link Produit} sont bien defendus.</p>
 */
class CatalogueTest {

    @Test
    void le_catalogue_contient_un_produit_par_type_de_lunette() {
        // Si on rajoute un nouveau type cote events, ce test pete tant qu'on n'a
        // pas mis a jour le catalogue. C'est exactement ce qu'on veut.
        for (TypeLunette type : TypeLunette.values()) {
            Produit p = Catalogue.parType(type);
            assertNotNull(p, "Type non trouve dans le catalogue : " + type);
            assertEquals(type, p.type());
        }
    }

    @Test
    void le_catalogue_a_exactement_4_produits() {
        assertEquals(TypeLunette.values().length, Catalogue.tous().size());
        assertEquals(4, Catalogue.tous().size());
    }

    @Test
    void un_produit_sans_badge_ne_se_revendique_pas_avec_badge() {
        Produit p = new Produit(TypeLunette.BANANA, "Test", "desc", 10.0, "", "/img.png");
        assertFalse(p.aUnBadge());
    }

    @Test
    void un_produit_avec_badge_se_revendique_avec_badge() {
        Produit p = new Produit(TypeLunette.BANANA, "Test", "desc", 10.0, "Promo", "/img.png");
        assertTrue(p.aUnBadge());
    }

    @Test
    void un_badge_null_est_tolere_et_ramene_a_chaine_vide() {
        // On veut etre indulgent avec les appelants : null => "" sans planter
        Produit p = new Produit(TypeLunette.BANANA, "Test", "desc", 10.0, null, "/img.png");
        assertEquals("", p.badge());
        assertFalse(p.aUnBadge());
    }

    @Test
    void un_prix_negatif_est_refuse() {
        assertThrows(IllegalArgumentException.class,
                () -> new Produit(TypeLunette.BANANA, "X", "desc", -1.0, "", "/img.png"));
    }

    @Test
    void un_type_null_est_refuse() {
        assertThrows(NullPointerException.class,
                () -> new Produit(null, "X", "desc", 10.0, "", "/img.png"));
    }
}
