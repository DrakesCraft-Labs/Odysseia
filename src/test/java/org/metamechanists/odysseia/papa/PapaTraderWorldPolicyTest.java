package org.metamechanists.odysseia.papa;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PapaTraderWorldPolicyTest {

    private static final List<String> WORLDS = List.of("world", "world_nether", "world_the_end");

    @Test
    void aceptaUnicamenteMundosExactosDeSlimefun() {
        assertTrue(PapaTraderMenu.mundoPermitido("world", WORLDS));
        assertTrue(PapaTraderMenu.mundoPermitido("WORLD_NETHER", WORLDS));
        assertFalse(PapaTraderMenu.mundoPermitido("SpawnWarps", WORLDS));
        assertFalse(PapaTraderMenu.mundoPermitido("laboratorio", WORLDS));
        assertFalse(PapaTraderMenu.mundoPermitido("world_laboratorio", WORLDS));
    }
}
