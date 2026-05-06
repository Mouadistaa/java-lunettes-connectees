package fr.miage.nancy.lunettes.usine;

import bernard_flou.Fabricateur;
import fr.miage.nancy.lunettes.events.TypeLunette;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeMapperTest {

    @Test
    void toFabricateurEstReversibleParFromFabricateur() {
        for (TypeLunette type : TypeLunette.values()) {
            Fabricateur.TypeLunette fabType = TypeMapper.toFabricateur(type);
            TypeLunette retour = TypeMapper.fromFabricateur(fabType);

            assertEquals(type, retour,
                    "round-trip cassé pour " + type);
        }
    }

    @Test
    void fromFabricateurEstReversibleParToFabricateur() {
        for (Fabricateur.TypeLunette fabType : Fabricateur.TypeLunette.values()) {
            TypeLunette type = TypeMapper.fromFabricateur(fabType);
            Fabricateur.TypeLunette retour = TypeMapper.toFabricateur(type);

            assertEquals(fabType, retour,
                    "round-trip cassé pour " + fabType);
        }
    }

    @Test
    void lesDeuxEnumsContiennentLeMemeNombreDeValeurs() {
        // vérifie que le typelunette du fabricateur n'a pas évolué
        assertEquals(
                Fabricateur.TypeLunette.values().length,
                TypeLunette.values().length,
                "les deux enums TypeLunette doivent rester de même cardinalité"
        );
    }
}