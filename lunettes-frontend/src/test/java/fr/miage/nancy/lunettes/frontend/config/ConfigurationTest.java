package fr.miage.nancy.lunettes.frontend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de la couche de configuration.
 *
 * <p>On verifie surtout que la priorite "System Properties > properties > defaults"
 * est bien respectee. C'est ce qui rend l'app deployable sans recompilation.</p>
 */
class ConfigurationTest {

    @AfterEach
    void nettoyer() {
        // On nettoie les system properties pour ne pas polluer les autres tests
        System.clearProperty(Configuration.KEY_BROKER_HOST);
        System.clearProperty(Configuration.KEY_BROKER_PORT);
    }

    @Test
    void valeurs_par_defaut_disponibles_si_aucune_surcharge() {
        Configuration config = new Configuration();
        // Si application.properties est present (cas normal), on lit dedans
        // Sinon les defaults s'appliquent. Dans les deux cas, ces valeurs doivent etre raisonnables.
        assertTrue(config.brokerPort() > 0 && config.brokerPort() < 65536);
        assertTrue(config.brokerHost() != null && !config.brokerHost().isBlank());
        assertTrue(config.verificationTimeoutMs() > 0);
    }

    @Test
    void system_property_surcharge_le_fichier_properties() {
        System.setProperty(Configuration.KEY_BROKER_HOST, "broker.example.com");
        System.setProperty(Configuration.KEY_BROKER_PORT, "8883");

        Configuration config = new Configuration();

        assertEquals("broker.example.com", config.brokerHost());
        assertEquals(8883, config.brokerPort());
    }
}
