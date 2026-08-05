package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalityStorageGuardListenerTest {

    private static final List<List<String>> BLOCKED = List.of(
            ModalityStorageGuardListener.tokens("ah"),
            ModalityStorageGuardListener.tokens("crazyauctions"),
            ModalityStorageGuardListener.tokens("team echest"));

    @Test
    void extractsCommandLabelsIncludingNamespacedForms() {
        assertEquals("pv", ModalityStorageGuardListener.label("pv"));
        assertEquals("pv", ModalityStorageGuardListener.label("PV"));
        assertEquals("pv", ModalityStorageGuardListener.label("playervaultz:pv"));
        assertEquals("vault", ModalityStorageGuardListener.label("PlayerVaultZ:Vault"));
    }

    @Test
    void tokenizesNamespaceOnlyInTheFirstToken() {
        assertEquals(List.of("team", "echest"), ModalityStorageGuardListener.tokens("BetterTeams:Team ECHEST"));
        assertEquals(List.of("ah"), ModalityStorageGuardListener.tokens("  ah  "));
    }

    @Test
    void blocksRootCommandsWithAndWithoutArguments() {
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ah")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ah sell 100")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("crazyauctions:ah")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("CrazyAuctions")));
    }

    @Test
    void blocksOnlyTheStorageSubcommandOfTeam() {
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team echest")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team ECHEST extra")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team chat hola")));
    }

    @Test
    void doesNotBlockUnrelatedCommandsThatSharePrefix() {
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ahorcado")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("spawn")));
    }
}
