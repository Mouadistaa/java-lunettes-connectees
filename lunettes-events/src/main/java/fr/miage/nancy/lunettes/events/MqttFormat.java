package fr.miage.nancy.lunettes.events;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constantes définissant la grammaire des payloads MQTT échangés
 * entre le frontend et l'usine.
 */
public final class MqttFormat {

    private MqttFormat() {
        // classe utilitaire, pas d'instanciation
    }

    // séparateur de champs au sein d'une même ligne
    public static final String FIELD_SEPARATOR = ";";

    // séparateur de lignes au sein d'un payload multi-lignes
    public static final String LINE_SEPARATOR = "\n";

    // encodage utilisé pour la conversion String <-> byte[]
    public static final Charset CHARSET = StandardCharsets.UTF_8;
}