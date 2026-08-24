package org.metamechanists.odysseia.laboratorio;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxSafetyListenerTest {

    @Test
    void onlyCreativeAndSurvivalAreValidSelfModes() {
        assertEquals(GameMode.CREATIVE, SandboxSafetyListener.requestedMode(new String[]{"gmc"}));
        assertEquals(GameMode.SURVIVAL, SandboxSafetyListener.requestedMode(new String[]{"gms"}));
        assertEquals(GameMode.CREATIVE,
                SandboxSafetyListener.requestedMode(new String[]{"gamemode", "creative"}));
        assertEquals(GameMode.SURVIVAL,
                SandboxSafetyListener.requestedMode(new String[]{"gamemode", "survival"}));
        assertEquals(GameMode.ADVENTURE, SandboxSafetyListener.requestedMode(new String[]{"gma"}));
        assertEquals(GameMode.SPECTATOR, SandboxSafetyListener.requestedMode(new String[]{"gmsp"}));
        assertNull(SandboxSafetyListener.requestedMode(new String[]{"gamemode", "creative", "OtherPlayer"}));
    }

    @Test
    void detectsExplosiveSlimefunIdsWithoutBlockingNormalMachines() {
        List<String> fragments = List.of("EXPLOSIVE", "TNT", "NUKE", "BOMB", "MISSILE", "WARHEAD");
        assertTrue(SandboxSafetyListener.isBlockedItemId("EXPLOSIVE_PICKAXE", fragments));
        assertTrue(SandboxSafetyListener.isBlockedItemId("INFINITY_NUKE", fragments));
        assertTrue(SandboxSafetyListener.isBlockedItemId("nano_missile_launcher", fragments));
        assertFalse(SandboxSafetyListener.isBlockedItemId("ELECTRIC_ORE_GRINDER_3", fragments));
        assertFalse(SandboxSafetyListener.isBlockedItemId("CARGO_MANAGER", fragments));

        List<String> dangerousMachines = List.of("CHUNK_LOADER", "TICK_ACCELERATOR", "TIME_ACCELERATOR");
        assertTrue(SandboxSafetyListener.isBlockedItemId("ANCIENT_CHUNK_LOADER", dangerousMachines));
        assertTrue(SandboxSafetyListener.isBlockedItemId("QUANTUM_TIME_ACCELERATOR", dangerousMachines));
        assertFalse(SandboxSafetyListener.isBlockedItemId("ELECTRIC_FURNACE_3", dangerousMachines));
    }
}
