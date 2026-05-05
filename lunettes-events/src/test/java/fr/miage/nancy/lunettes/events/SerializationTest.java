package fr.miage.nancy.lunettes.events;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest {

    // Tests round-trips

    @Test
    void roundTripCommandeMonoType() {
        Commande commande = new Commande(
                UUID.randomUUID(),
                Map.of(TypeLunette.BANANA, 3)
        );

        byte[] payload = Serializer.serializeCommande(commande);
        Map<TypeLunette, Integer> lignes = Deserializer.deserializeCommandeLignes(payload);

        assertEquals(commande.lignes(), lignes);
    }

    @Test
    void roundTripCommandeMultiTypes() {
        Commande commande = new Commande(
                UUID.randomUUID(),
                Map.of(
                        TypeLunette.BANANA, 2,
                        TypeLunette.CLAUDE, 1,
                        TypeLunette.LE_CHAT, 4
                )
        );

        byte[] payload = Serializer.serializeCommande(commande);
        Map<TypeLunette, Integer> lignes = Deserializer.deserializeCommandeLignes(payload);

        assertEquals(commande.lignes(), lignes);
    }

    @Test
    void serializeCommandeOmetLesTypesAvecQuantiteZero() {
        Commande commande = new Commande(
                UUID.randomUUID(),
                Map.of(
                        TypeLunette.BANANA, 0,
                        TypeLunette.CLAUDE, 2
                )
        );

        byte[] payload = Serializer.serializeCommande(commande);
        Map<TypeLunette, Integer> lignes = Deserializer.deserializeCommandeLignes(payload);

        // Seul CLAUDE doit subsister dans le payload sérialisé
        assertEquals(1, lignes.size());
        assertEquals(2, lignes.get(TypeLunette.CLAUDE));
    }

    @Test
    void roundTripDelivery() {
        List<LunetteProduite> livraison = List.of(
                new LunetteProduite(TypeLunette.BANANA, "BA-5XK2J-A1B2C3D4"),
                new LunetteProduite(TypeLunette.BANANA, "BA-9PQ1L-77AB22FF"),
                new LunetteProduite(TypeLunette.CLAUDE, "CL-Z8K2P-DEAD1234")
        );

        byte[] payload = Serializer.serializeDelivery(livraison);
        List<LunetteProduite> deserialized = Deserializer.deserializeDelivery(payload);

        assertEquals(livraison, deserialized);
    }

    @Test
    void roundTripText() {
        String message = "fabricateur indisponible";

        byte[] payload = Serializer.serializeText(message);
        String deserialized = Deserializer.deserializeText(payload);

        assertEquals(message, deserialized);
    }

    @Test
    void payloadEstEncodeEnUtf8() {
        // Caractère non-ASCII pour vérifier que l'encodage est bien préservé
        String message = "fabricateur indisponible — réessayez plus tard";

        byte[] payload = Serializer.serializeText(message);
        String deserialized = Deserializer.deserializeText(payload);

        assertEquals(message, deserialized);
        // Vérification supplémentaire : le tiret long prend bien 3 octets en UTF-8
        assertTrue(payload.length > message.length());
    }

    // Cas d'erreur du Deserializer Commande

    @Test
    void payloadCommandeVideEstRejete() {
        byte[] payload = new byte[0];

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeCommandeLignes(payload));
    }

    @Test
    void payloadCommandeAvecTypeInconnuEstRejete() {
        byte[] payload = "INCONNU;3".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeCommandeLignes(payload));
    }

    @Test
    void payloadCommandeAvecQuantiteNonNumeriqueEstRejete() {
        byte[] payload = "BANANA;abc".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeCommandeLignes(payload));
    }

    @Test
    void payloadCommandeMalFormeEstRejete() {
        // Une ligne avec un seul champ, le séparateur est manquant
        byte[] payload = "BANANA".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeCommandeLignes(payload));
    }

    @Test
    void payloadCommandeAvecTypeDupliqueEstRejete() {
        byte[] payload = "BANANA;2\nBANANA;1".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeCommandeLignes(payload));
    }

    // Cas d'erreur du Deserializer Livraison

    @Test
    void payloadDeliveryVideEstRejete() {
        byte[] payload = new byte[0];

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeDelivery(payload));
    }

    @Test
    void payloadDeliveryAvecTypeInconnuEstRejete() {
        byte[] payload = "INCONNU;BA-5XK2J-A1B2C3D4".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeDelivery(payload));
    }

    @Test
    void payloadDeliveryAvecNumeroSerieVideEstRejete() {
        byte[] payload = "BANANA;".getBytes(MqttFormat.CHARSET);

        assertThrows(MalformedPayloadException.class,
                () -> Deserializer.deserializeDelivery(payload));
    }
}