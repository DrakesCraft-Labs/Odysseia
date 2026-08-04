package org.metamechanists.odysseia.modalities;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalityTest {

    private static Modality skyblock() {
        return new Modality("skyblock", "&bSkyBlock", "", "SNOW_BLOCK", "skyblock", List.of("bskyblock_world"));
    }

    @Test
    void coversTheNetherAndEndOfItsWorld() {
        Modality skyblock = skyblock();
        assertTrue(skyblock.matches("bskyblock_world"));
        assertTrue(skyblock.matches("bskyblock_world_nether"));
        assertTrue(skyblock.matches("bskyblock_world_the_end"));
        assertTrue(skyblock.matches("BSkyBlock_World"), "el nombre del mundo no debe ser sensible a mayusculas");
    }

    @Test
    void doesNotSwallowSurvivalOrOtherModalityWorlds() {
        Modality skyblock = skyblock();
        assertFalse(skyblock.matches("world"));
        assertFalse(skyblock.matches("world_nether"));
        assertFalse(skyblock.matches("oneblock_world"));
        assertFalse(skyblock.matches("world_galactifun_mars"));
        assertFalse(skyblock.matches("drakes_bosses"));
        assertFalse(skyblock.matches(null));
    }

    @Test
    void doesNotMatchOnAPlainPrefixWithoutSeparator() {
        // "bskyblock_worldia" no es un mundo de la modalidad aunque empiece igual.
        assertFalse(skyblock().matches("bskyblock_worldia"));
    }
}
