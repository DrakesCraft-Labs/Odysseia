package org.metamechanists.odysseia.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OwnerAuraCommandTest {

    @Test
    void acceptsOnlyExplicitSafeRadii() {
        assertEquals(2, OwnerAuraCommand.parseRadius("2"));
        assertEquals(5, OwnerAuraCommand.parseRadius("5"));
        assertEquals(10, OwnerAuraCommand.parseRadius("10"));
        assertEquals(100, OwnerAuraCommand.parseRadius("100"));
        assertNull(OwnerAuraCommand.parseRadius("1"));
        assertNull(OwnerAuraCommand.parseRadius("500"));
        assertNull(OwnerAuraCommand.parseRadius("todo"));
    }
}
