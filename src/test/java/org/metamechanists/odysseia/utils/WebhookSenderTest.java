package org.metamechanists.odysseia.utils;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSenderTest {

    @Test
    void trackedSendRejectsInvalidUrlWithoutNetworkRequest() throws Exception {
        assertFalse(WebhookSender.sendAsyncTracked(null, "http://127.0.0.1/hook", "{}")
                .get(1, TimeUnit.SECONDS));
    }

    @Test
    void onlyHttp2xxCountsAsConfirmedDelivery() {
        assertTrue(WebhookSender.isSuccessfulResponseCode(200));
        assertTrue(WebhookSender.isSuccessfulResponseCode(204));
        assertFalse(WebhookSender.isSuccessfulResponseCode(199));
        assertFalse(WebhookSender.isSuccessfulResponseCode(300));
        assertFalse(WebhookSender.isSuccessfulResponseCode(429));
        assertFalse(WebhookSender.isSuccessfulResponseCode(500));
    }
}
