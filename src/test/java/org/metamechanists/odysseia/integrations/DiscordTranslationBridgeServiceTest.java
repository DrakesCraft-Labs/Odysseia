package org.metamechanists.odysseia.integrations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DiscordTranslationBridgeServiceTest {

    @Test
    void signsLiveChatIngressInsteadOfSendingApiCredentials() throws IOException {
        Path source = Path.of("src/main/java/org/metamechanists/odysseia/integrations/DiscordTranslationBridgeService.java");
        String code = Files.readString(source);

        assertTrue(code.contains("discord-translator.ingest-secret"));
        assertTrue(code.contains("X-Odysseia-Timestamp"));
        assertTrue(code.contains("X-Odysseia-Signature"));
        assertTrue(code.contains("Mac.getInstance(\"HmacSHA256\")"));
        assertTrue(code.contains("value & 0xff"));
        assertFalse(code.contains("{\\\"player\\\":%s,\\\"message\\\":%s,\\\"rank\\\":%s,\\\"world\\\":%s,\\\"api_key\\\":%s}"));
    }
}
