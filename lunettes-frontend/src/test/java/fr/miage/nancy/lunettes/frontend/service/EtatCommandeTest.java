package fr.miage.nancy.lunettes.frontend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests des etats terminaux de la machine a etats des commandes.
 *
 * <p>L'idee : se proteger des regressions futures sur la logique
 * "est-ce que l'UI doit deverrouiller le bouton de nouvelle commande ?".</p>
 */
class EtatCommandeTest {

    @Test
    void les_etats_intermediaires_ne_sont_pas_terminaux() {
        assertFalse(EtatCommande.CREEE.estTerminal());
        assertFalse(EtatCommande.ENVOYEE.estTerminal());
        assertFalse(EtatCommande.VALIDEE.estTerminal());
        assertFalse(EtatCommande.EN_FABRICATION.estTerminal());
    }

    @Test
    void les_etats_terminaux_sont_terminaux() {
        assertTrue(EtatCommande.LIVREE.estTerminal());
        assertTrue(EtatCommande.ANNULEE.estTerminal());
        assertTrue(EtatCommande.EN_ERREUR.estTerminal());
    }
}
