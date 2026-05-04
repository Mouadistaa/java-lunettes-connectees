package fr.miage.nancy.lunettes.events;

/**
 * Lancée par le deserializer quand un payload MQTT ne respecte pas
 * la grammaire attendue (champ manquant, type inconnu, quantité non
 * numérique, etc.)
 */
public class MalformedPayloadException extends RuntimeException {

    public MalformedPayloadException(String message) {
        super(message);
    }
}