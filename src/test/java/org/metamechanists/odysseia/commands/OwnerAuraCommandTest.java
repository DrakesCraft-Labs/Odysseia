package org.metamechanists.odysseia.commands;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void purgeIsSingleShotAndExplicitlyExcludesEveryPlayer() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/metamechanists/odysseia/commands/OwnerAuraCommand.java"));

        assertEquals(1, source.split("int removed = purge\\(", -1).length - 1,
                "el comando no debe programar una segunda purga");
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("target instanceof Player"),
                "la purga debe excluir a todos los jugadores, no solo al ejecutor");
    }
}
