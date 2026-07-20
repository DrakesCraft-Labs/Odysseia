package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreCommandGuardListenerTest {
    @Test
    void blocksLegacyStoreCommandsIncludingNamespacedVariants() {
        assertTrue(StoreCommandGuardListener.isLegacyStoreCommand("/shop"));
        assertTrue(StoreCommandGuardListener.isLegacyStoreCommand("/ultimateshop"));
        assertTrue(StoreCommandGuardListener.isLegacyStoreCommand("/odysseia:drakestienda"));
        assertTrue(StoreCommandGuardListener.isLegacyStoreCommand("/tiendamateriales decoracion"));
    }

    @Test
    void leavesTheCanonicalAndUnrelatedCommandsAvailable() {
        assertFalse(StoreCommandGuardListener.isLegacyStoreCommand("/tienda"));
        assertFalse(StoreCommandGuardListener.isLegacyStoreCommand("/jobs browse"));
        assertFalse(StoreCommandGuardListener.isLegacyStoreCommand("/pw"));
        assertFalse(StoreCommandGuardListener.isLegacyStoreCommand("tienda"));
    }
}
