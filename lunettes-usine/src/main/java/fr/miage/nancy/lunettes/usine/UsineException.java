package fr.miage.nancy.lunettes.usine;

/**
 * Lancée par l'{@link Usine} pour signaler une erreur lors du traitement
 * d'une commande de fabrication.
 *
 * <p>Cette exception encapsule les erreurs internes du fabricateur sous-jacent
 * (capacité dépassée, type non configuré, état machine invalide) ainsi que
 * les erreurs propres à l'Usine (commande inconsistante, échec de batchage,
 * thread interrompu pendant la fabrication).</p>
 *
 * <p>L'objectif est d'offrir une frontière nette aux consommateurs de l'Usine
 * (typiquement le serveur MQTT) : peu importe la cause technique sous-jacente,
 * une défaillance est toujours signalée par {@code UsineException} avec un
 * message explicatif. Le consommateur n'a pas à connaître les détails du
 * fabricateur.</p>
 *
 * <p>Choisie {@code unchecked} ({@link RuntimeException}) pour la même raison
 * que {@code MalformedPayloadException} dans le module events : la propagation
 * d'exceptions checkées dans des callbacks MQTT et des appels asynchrones
 * serait verbeuse sans bénéfice réel.</p>
 */
public class UsineException extends RuntimeException {

    public UsineException(String message) {
        super(message);
    }

    public UsineException(String message, Throwable cause) {
        super(message, cause);
    }
}