package org.metamechanists.odysseia.integrations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiscordTranslationBridgeServiceTest {

    @Test
    void usesDirectTranslatorWithoutTheRetiredWebChatFeed() throws IOException {
        Path source = Path.of("src/main/java/org/metamechanists/odysseia/integrations/DiscordTranslationBridgeService.java");
        String code = Files.readString(source);

        assertTrue(code.contains("translate.drakescraft.cl"));
        assertFalse(code.contains("discord-translator.ingest-secret"));
        assertFalse(code.contains("/api/chat/ingest"));
        assertFalse(code.contains("X-Odysseia-Timestamp"));
        assertFalse(code.contains("X-Odysseia-Signature"));
        assertFalse(code.contains("Mac.getInstance(\"HmacSHA256\")"));
    }
}
