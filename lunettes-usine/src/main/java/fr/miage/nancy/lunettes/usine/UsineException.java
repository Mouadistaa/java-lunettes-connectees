package fr.miage.nancy.lunettes.usine;

/**
 * Lancée par l'usine pour signaler une erreur lors
 * du traitement d'une commande de fabrication.
 */
public class UsineException extends RuntimeException {

    public UsineException(String message) {
        super(message);
    }

    public UsineException(String message, Throwable cause) {
        super(message, cause);
    }
}