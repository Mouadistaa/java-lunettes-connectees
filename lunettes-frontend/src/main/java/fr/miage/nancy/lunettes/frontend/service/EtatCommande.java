package fr.miage.nancy.lunettes.frontend.service;

/**
 * Etats successifs traverses par une commande cote frontend.
 *
 * <p>Ils representent ce que <strong>le client</strong> sait de la commande, en
 * synthese de tous les messages MQTT recus pour cette commande precise.</p>
 *
 * <p>Diagramme :</p>
 * <pre>
 *  CREEE (locale, pas encore publiee)
 *    |
 *    | publish orders/{uuid}
 *    v
 *  ENVOYEE
 *    |
 *    | recu orders/{uuid}/validated
 *    v
 *  VALIDEE
 *    |
 *    | recu orders/{uuid}/status = "processing"
 *    v
 *  EN_FABRICATION
 *    |
 *    | recu orders/{uuid}/delivery
 *    v
 *  LIVREE  (etat terminal heureux)
 *
 *  A tout moment, on peut basculer vers :
 *    ANNULEE  (recu orders/{uuid}/cancelled, payload invalide)
 *    EN_ERREUR (recu orders/{uuid}/error, plantage de fabrication)
 * </pre>
 */
public enum EtatCommande {
    /** La commande existe en memoire cote client mais n'a pas encore ete publiee. */
    CREEE,
    /** Le payload a ete publie sur le broker, on attend la confirmation. */
    ENVOYEE,
    /** Le backend a accuse reception et valide la commande. */
    VALIDEE,
    /** L'usine a commence la fabrication. */
    EN_FABRICATION,
    /** Tout est bon : on a recu les numeros de serie. Etat terminal. */
    LIVREE,
    /** Le backend a refuse la commande (validation metier echouee). Etat terminal. */
    ANNULEE,
    /** Erreur survenue pendant la fabrication. Etat terminal. */
    EN_ERREUR;

    /** Vrai si la commande ne peut plus evoluer (succes ou echec definitif). */
    public boolean estTerminal() {
        return this == LIVREE || this == ANNULEE || this == EN_ERREUR;
    }
}
