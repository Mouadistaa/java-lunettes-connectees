package fr.miage.nancy.lunettes.events;

import java.util.List;
import java.util.Map;

/**
 * Convertit les DTOs du domaine en payloads MQTT (chaînes encodées en UTF-8)
 */
public final class Serializer {

    private Serializer() {
        // classe utilitaire, pas d'instanciation
    }

    // sérialise les lignes d'une commande au format <TYPE>;<QUANTITE> (une ligne par type)
    public static byte[] serializeCommande(Commande commande) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<TypeLunette, Integer> entry : commande.lignes().entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            if (!first) {
                sb.append(MqttFormat.LINE_SEPARATOR);
            }
            sb.append(entry.getKey().name())
              .append(MqttFormat.FIELD_SEPARATOR)
              .append(entry.getValue());
            first = false;
        }
        return sb.toString().getBytes(MqttFormat.CHARSET);
    }

    // sérialise une livraison au format <TYPE>;<NUMERO_SERIE> (une ligne par lunette)
    public static byte[] serializeDelivery(List<LunetteProduite> lunettes) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (LunetteProduite lunette : lunettes) {
            if (!first) {
                sb.append(MqttFormat.LINE_SEPARATOR);
            }
            sb.append(lunette.type().name())
              .append(MqttFormat.FIELD_SEPARATOR)
              .append(lunette.numeroSerie());
            first = false;
        }
        return sb.toString().getBytes(MqttFormat.CHARSET);
    }

    // sérialise un payload mono-ligne (message d'erreur, statut, type de retour pour vérification de numéro de série, etc.)
    public static byte[] serializeText(String text) {
        return text.getBytes(MqttFormat.CHARSET);
    }
}