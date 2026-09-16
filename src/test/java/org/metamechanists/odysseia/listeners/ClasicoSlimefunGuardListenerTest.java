package org.metamechanists.odysseia.listeners;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasicoSlimefunGuardListenerTest {

    @Test
    void reconoceNombresDeMundoClasico() {
        ClasicoSlimefunGuardListener guard = new ClasicoSlimefunGuardListener(null, null);
        assertTrue(guard.isClasicoName("clasico"));
        assertTrue(guard.isClasicoName("clasico_nether"));
        assertTrue(guard.isClasicoName("clasico_the_end"));
        assertFalse(guard.isClasicoName("world"));
        assertFalse(guard.isClasicoName("laboratorio"));
        assertFalse(guard.isClasicoName("bskyblock_world"));
        assertFalse(guard.isClasicoName(null));
    }

    @Test
    void itemNuloNoEsSlimefun() {
        ClasicoSlimefunGuardListener guard = new ClasicoSlimefunGuardListener(null, null);
        assertFalse(guard.isSlimefunOrCustomItem(null));
    }
}
