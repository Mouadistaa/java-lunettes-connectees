package fr.miage.nancy.lunettes.events;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Constantes définissant la grammaire des payloads MQTT échangés
 * entre le frontend et l'usine.
 *
 * <p>Le sujet du projet interdit l'utilisation d'un format de
 * sérialisation prêt à l'emploi (JSON via Jackson, protobuf, etc.).
 * Nous avons donc défini un format texte minimaliste, lisible
 * humainement, parsable trivialement, et adapté au volume de données
 * échangées (quelques lignes par message, sans imbrication).</p>
 *
 * <h2>Grammaire</h2>
 * <ul>
 *   <li>Encodage UTF-8.</li>
 *   <li>Champs au sein d'une ligne séparés par {@value FIELD_SEPARATOR}.</li>
 *   <li>Lignes séparées par {@code LF} (pas {@code CRLF}), c'est-à-dire {@code \n}.</li>
 *   <li>Pas de quoting ni d'échappement : les valeurs métier ne contiennent
 *       par construction ni le séparateur de champ ni le séparateur de ligne
 *       (ce sont des enums et des UUID).</li>
 *   <li>Le type de message n'est pas inscrit dans le payload : le topic MQTT
 *       sur lequel le message est publié l'identifie sans ambiguïté.</li>
 * </ul>
 *
 * <p>Voir aussi le document {@code docs/equipe.md} pour la spécification
 * complète des couples (topic, payload).</p>
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