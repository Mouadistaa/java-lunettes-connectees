package fr.miage.nancy.lunettes.frontend.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge la configuration externalisée du frontend depuis le classpath.
 *
 * <p>Le sujet impose que les paramètres (adresse du broker, port, ...) soient
 * lus depuis un fichier properties, pas hardcodés. On embarque ce fichier dans
 * le jar pour que le frontend marche tout seul, mais l'utilisateur peut aussi
 * surcharger les valeurs via les System Properties au lancement (utile en CI).</p>
 *
 * <p>L'ordre de priorité, du plus fort au plus faible :</p>
 * <ol>
 *   <li>Les System Properties JVM (ex : {@code -Dmqtt.broker.host=192.168.1.42})</li>
 *   <li>Les valeurs du {@code application.properties} embarqué</li>
 *   <li>Les valeurs par défaut codées en dur ici (filet de sécurité)</li>
 * </ol>
 */
public final class Configuration {

    private static final String PROPERTIES_FILE = "application.properties";

    // --- clés des paramètres ---
    public static final String KEY_BROKER_HOST = "mqtt.broker.host";
    public static final String KEY_BROKER_PORT = "mqtt.broker.port";
    public static final String KEY_CLIENT_ID_PREFIX = "mqtt.client.id.prefix";
    public static final String KEY_VERIFICATION_TIMEOUT_MS = "verification.timeout.ms";

    // --- valeurs par défaut (filet de sécurité si le fichier n'est pas trouvé) ---
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "1883";
    private static final String DEFAULT_CLIENT_ID_PREFIX = "frontend-";
    private static final String DEFAULT_VERIFICATION_TIMEOUT_MS = "5000";

    private final Properties properties;

    public Configuration() {
        this.properties = new Properties();
        chargerDepuisClasspath();
    }

    private void chargerDepuisClasspath() {
        try (InputStream input = Configuration.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                properties.load(input);
            }
            // Si le fichier est absent, on tombera sur les valeurs par défaut.
            // Pas d'erreur fatale ici : on veut que l'app reste utilisable.
        } catch (IOException e) {
            // Log discret, on continue avec les defaults
            System.err.println("Lecture de " + PROPERTIES_FILE + " impossible : " + e.getMessage());
        }
    }

    private String get(String key, String defaultValue) {
        // Priorité : -D<key>  >  application.properties  >  defaultValue
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }

    public String brokerHost() {
        return get(KEY_BROKER_HOST, DEFAULT_HOST);
    }

    public int brokerPort() {
        return Integer.parseInt(get(KEY_BROKER_PORT, DEFAULT_PORT));
    }

    public String clientIdPrefix() {
        return get(KEY_CLIENT_ID_PREFIX, DEFAULT_CLIENT_ID_PREFIX);
    }

    public long verificationTimeoutMs() {
        return Long.parseLong(get(KEY_VERIFICATION_TIMEOUT_MS, DEFAULT_VERIFICATION_TIMEOUT_MS));
    }
}
