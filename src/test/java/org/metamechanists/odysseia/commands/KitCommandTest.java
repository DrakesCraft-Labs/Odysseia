package org.metamechanists.odysseia.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KitCommandTest {

    @Test
    void resolvesEnglishAndSpanishStarterAliases() {
        assertEquals("inicial", KitCommand.resolveKitAlias("initial"));
        assertEquals("inicial", KitCommand.resolveKitAlias("Initial"));
        assertEquals("inicial", KitCommand.resolveKitAlias("starter"));
        assertEquals("inicial", KitCommand.resolveKitAlias("start"));
        assertEquals("inicial", KitCommand.resolveKitAlias("inicio"));
        assertEquals("inicial", KitCommand.resolveKitAlias("inicial"));
        assertEquals("inicial", KitCommand.resolveKitAlias("  INITIAL  "));
    }

    @Test
    void preservesOtherKitNames() {
        assertEquals("zeus", KitCommand.resolveKitAlias("zeus"));
        assertEquals("hercules", KitCommand.resolveKitAlias("hercules"));
        assertEquals("minero", KitCommand.resolveKitAlias("minero"));
        assertNull(KitCommand.resolveKitAlias(null));
    }
}
