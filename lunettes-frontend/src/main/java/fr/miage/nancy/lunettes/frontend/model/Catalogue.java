package fr.miage.nancy.lunettes.frontend.model;

import fr.miage.nancy.lunettes.events.TypeLunette;

import java.util.List;

/**
 * Catalogue statique des produits proposés par l'application.
 *
 * <p>Les valeurs sont alignées sur {@code docs/assets/products.json} fourni avec
 * le sujet. On le code en dur côté frontend parce que :</p>
 * <ul>
 *   <li>Le sujet ne demande pas que le catalogue soit dynamique côté backend.</li>
 *   <li>Tous les types possibles sont déjà figés par l'enum {@link TypeLunette}.</li>
 *   <li>Ça évite une étape de parsing JSON qui n'apporterait rien ici.</li>
 * </ul>
 *
 * <p>Si plus tard on veut un catalogue piloté par le backend, il suffira de
 * remplacer ce singleton statique par une classe qui interroge un topic MQTT
 * dédié. L'API publique vue par l'UI ne changerait pas.</p>
 */
public final class Catalogue {

    private static final List<Produit> PRODUITS = List.of(
            new Produit(
                    TypeLunette.BANANA,
                    "Bananaaaa",
                    "Design iconique des annees 50, parfait pour un look vintage et decontracte.",
                    89.99,
                    "Nouveau",
                    "/images/banana.png"
            ),
            new Produit(
                    TypeLunette.CHATGPT,
                    "BlaBlaBla",
                    "Lunettes style aviateur avec verres polarises et monture en metal dore.",
                    74.99,
                    "",
                    "/images/chatgpt.png"
            ),
            new Produit(
                    TypeLunette.LE_CHAT,
                    "Miaousse",
                    "Lunettes de vue sophistiquees avec monture fine et design contemporain.",
                    129.99,
                    "-10%",
                    "/images/le_chat.png"
            ),
            new Produit(
                    TypeLunette.CLAUDE,
                    "Claude",
                    "Monture ultra-legere et resistante, ideale pour les activites sportives.",
                    99.99,
                    "Bestseller",
                    "/images/claude.png"
            )
    );

    private Catalogue() {
        // utility class
    }

    /** Tous les produits du catalogue, dans l'ordre d'affichage souhaité. */
    public static List<Produit> tous() {
        return PRODUITS;
    }

    /** Récupère un produit à partir de son type technique (utile pour la livraison). */
    public static Produit parType(TypeLunette type) {
        for (Produit p : PRODUITS) {
            if (p.type() == type) {
                return p;
            }
        }
        // ne devrait jamais arriver vu que le catalogue couvre tous les types de l'enum
        throw new IllegalArgumentException("type inconnu du catalogue : " + type);
    }
}
