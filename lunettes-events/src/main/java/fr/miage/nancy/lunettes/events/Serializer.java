package fr.miage.nancy.lunettes.events;

import java.util.List;
import java.util.Map;

/**
 * Convertit les DTOs du domaine en payloads MQTT (chaînes encodées en UTF-8).
 *
 * <p>L'id de commande n'est jamais sérialisé dans le payload : il est porté
 * par le topic MQTT lui-même.</p>
 *
 * @see MqttFormat
 * @see Deserializer
 */
public final class Serializer {

    private Serializer() {
        // classe utilitaire, pas d'instanciation
    }

    /**
     * Sérialise les lignes d'une commande au format
     * {@code <TYPE>;<QUANTITE>} (une ligne par type).
     *
     * <p>Les types ayant une quantité de zéro sont omis du payload :
     * ils n'apporteraient aucune information utile à l'usine.</p>
     */
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

    /**
     * Sérialise une livraison au format {@code <TYPE>;<NUMERO_SERIE>}
     * (une ligne par lunette).
     */
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

    /**
     * Sérialise un payload mono-ligne (message d'erreur, statut, type
     * de retour pour vérification de numéro de série, etc.).
     */
    public static byte[] serializeText(String text) {
        return text.getBytes(MqttFormat.CHARSET);
    }
}