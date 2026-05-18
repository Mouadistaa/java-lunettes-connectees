package fr.miage.nancy.lunettes.events;

import java.util.UUID;

/**
 * Centralise la construction des topics MQTT du protocole.
 * Évite que chaque module concatène les chaînes à la main et risque
 * une divergence (slash en trop, suffixe au pluriel, etc.).
 */
public final class Topics {

    private Topics() {
    }

    // Préfixes et suffixes du protocole
    private static final String ORDERS = "orders/";
    private static final String SERIALS = "serials/";

    private static final String VALIDATED = "/validated";
    private static final String CANCELLED = "/cancelled";
    private static final String DELIVERY = "/delivery";
    private static final String ERROR = "/error";
    private static final String STATUS = "/status";
    private static final String CHECK = "/check";

    // Wildcards pour les souscriptions côté serveur
    public static final String ALL_ORDERS = ORDERS + "+";
    public static final String ALL_SERIAL_CHECKS = SERIALS + "+" + CHECK;

    // Topics publiés par le client
    public static String order(UUID id) {
        return ORDERS + id;
    }

    public static String serialCheck(String numeroSerie) {
        return SERIALS + numeroSerie + CHECK;
    }

    // Topics publiés par l'usine
    public static String validated(UUID id) {
        return ORDERS + id + VALIDATED;
    }

    public static String cancelled(UUID id) {
        return ORDERS + id + CANCELLED;
    }

    public static String delivery(UUID id) {
        return ORDERS + id + DELIVERY;
    }

    public static String error(UUID id) {
        return ORDERS + id + ERROR;
    }

    public static String status(UUID id) {
        return ORDERS + id + STATUS;
    }

    public static String serialResponse(String numeroSerie) {
        return SERIALS + numeroSerie;
    }
}