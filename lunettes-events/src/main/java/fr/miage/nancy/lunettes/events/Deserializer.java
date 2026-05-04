package fr.miage.nancy.lunettes.events;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Convertit les payloads MQTT (octets encodés en UTF-8) en DTOs du domaine
 */
public final class Deserializer {

    private Deserializer() {
        // classe utilitaire, pas d'instanciation
    }

    // Désérialise les lignes d'une commande depuis un payload au format <TYPE>;<QUANTITE> (une ligne par type)
    public static Map<TypeLunette, Integer> deserializeCommandeLignes(byte[] payload) {
        String text = new String(payload, MqttFormat.CHARSET);
        if (text.isBlank()) {
            throw new MalformedPayloadException("payload vide");
        }

        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        String[] rows = text.split(MqttFormat.LINE_SEPARATOR);
        for (String row : rows) {
            String[] fields = row.split(MqttFormat.FIELD_SEPARATOR);
            if (fields.length != 2) {
                throw new MalformedPayloadException(
                        "ligne mal formée (attendu 2 champs séparés par '"
                                + MqttFormat.FIELD_SEPARATOR + "') : " + row);
            }
            TypeLunette type = parseType(fields[0]);
            int quantite = parseQuantite(fields[1]);
            if (lignes.put(type, quantite) != null) {
                throw new MalformedPayloadException(
                        "type dupliqué dans le payload : " + type);
            }
        }
        return lignes;
    }

    // Désérialise une livraison depuis un payload au format <TYPE>;<NUMERO_SERIE> (une ligne par lunette)
    public static List<LunetteProduite> deserializeDelivery(byte[] payload) {
        String text = new String(payload, MqttFormat.CHARSET);
        if (text.isBlank()) {
            throw new MalformedPayloadException("payload vide");
        }

        List<LunetteProduite> lunettes = new ArrayList<>();
        String[] rows = text.split(MqttFormat.LINE_SEPARATOR);
        for (String row : rows) {
            String[] fields = row.split(MqttFormat.FIELD_SEPARATOR);
            if (fields.length != 2) {
                throw new MalformedPayloadException(
                        "ligne mal formée (attendu 2 champs séparés par '"
                                + MqttFormat.FIELD_SEPARATOR + "') : " + row);
            }
            TypeLunette type = parseType(fields[0]);
            String numeroSerie = fields[1];
            try {
                lunettes.add(new LunetteProduite(type, numeroSerie));
            } catch (IllegalArgumentException e) {
                throw new MalformedPayloadException(
                        "lunette invalide dans la livraison : " + e.getMessage());
            }
        }
        return lunettes;
    }

    /**
     * Désérialise un payload mono-ligne en chaîne de caractères.
     */
    public static String deserializeText(byte[] payload) {
        return new String(payload, MqttFormat.CHARSET);
    }

    private static TypeLunette parseType(String value) {
        try {
            return TypeLunette.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new MalformedPayloadException(
                    "type de lunette inconnu : '" + value + "'");
        }
    }

    private static int parseQuantite(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new MalformedPayloadException(
                    "quantité non numérique : '" + value + "'");
        }
    }
}