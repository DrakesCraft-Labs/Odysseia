package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModalityStorageGuardListenerTest {

    @Test
    void extractsCommandLabelsIncludingNamespacedForms() {
        assertEquals("pv", ModalityStorageGuardListener.label("pv"));
        assertEquals("pv", ModalityStorageGuardListener.label("PV"));
        assertEquals("pv", ModalityStorageGuardListener.label("playervaultz:pv"));
        assertEquals("vault", ModalityStorageGuardListener.label("PlayerVaultZ:Vault"));
    }
}
