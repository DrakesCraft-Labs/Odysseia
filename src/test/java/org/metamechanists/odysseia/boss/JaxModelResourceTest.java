package org.metamechanists.odysseia.boss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JaxModelResourceTest {

    @Test
    void shipsTheOriginalDisplayModelAsAFunctionSafeCommand() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/models/jax-model.command")) {
            assertNotNull(stream, "Jax requires its display model resource");
            String command = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();

            assertTrue(command.startsWith("summon block_display "));
            assertFalse(command.startsWith("/"), "Datapack/plugin commands must not use the chat slash prefix");
            assertEquals(14, occurrences(command, "id:\"minecraft:item_display\""));
        }
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }
}
