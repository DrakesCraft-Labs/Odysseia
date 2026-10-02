package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationListenerTest {

    @Test
    void acceptsOnlyConfiguredDiscordWebhookEndpoints() {
        assertFalse(ModerationListener.isValidModerationWebhook(null));
        assertFalse(ModerationListener.isValidModerationWebhook(""));
        assertFalse(ModerationListener.isValidModerationWebhook("REPLACE_ME"));
        assertFalse(ModerationListener.isValidModerationWebhook("http://discord.com/api/webhooks/1/x"));
        assertTrue(ModerationListener.isValidModerationWebhook(
                "https://discord.com/api/webhooks/123456789012345678/token"));
    }
}
