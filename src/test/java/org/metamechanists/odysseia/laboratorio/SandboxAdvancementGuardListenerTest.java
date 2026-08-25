package org.metamechanists.odysseia.laboratorio;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxAdvancementGuardListenerTest {

    @Test
    void blocksOnlyConfiguredLaboratoryWorldsCaseInsensitively() {
        Set<String> laboratoryWorlds = Set.of("laboratorio", "laboratorio_nether");

        assertTrue(SandboxAdvancementGuardListener.isSandboxWorld("laboratorio", laboratoryWorlds));
        assertTrue(SandboxAdvancementGuardListener.isSandboxWorld("LABORATORIO_NETHER", laboratoryWorlds));
        assertFalse(SandboxAdvancementGuardListener.isSandboxWorld("world", laboratoryWorlds));
        assertFalse(SandboxAdvancementGuardListener.isSandboxWorld(null, laboratoryWorlds));
    }
}
