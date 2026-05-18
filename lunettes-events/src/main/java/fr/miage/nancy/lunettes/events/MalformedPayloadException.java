package fr.miage.nancy.lunettes.events;

/**
 * Lancée par {@link Deserializer} quand un payload MQTT ne respecte pas
 * la grammaire attendue (champ manquant, type inconnu, quantité non
 * numérique, etc.).
 *
 * <p>{@code RuntimeException} (non checkée) parce que la désérialisation
 * est appelée dans des callbacks MQTT où la propagation d'exceptions
 * checkées serait pénible et où, en pratique, l'appelant attrape de
 * toute façon toutes les exceptions pour publier sur un topic
 * d'erreur.</p>
 */
public class MalformedPayloadException extends RuntimeException {

    public MalformedPayloadException(String message) {
        super(message);
    }
}