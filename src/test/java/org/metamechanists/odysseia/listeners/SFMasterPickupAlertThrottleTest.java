package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SFMasterPickupAlertThrottleTest {

    @Test
    void limitsRepeatedAlertsWithoutChangingTheFirstAlert() {
        Map<String, Long> alerts = new HashMap<>();

        assertTrue(SFMasterWatcherListener.shouldAlertOwnerMismatch(alerts, "player:item", 1_000L));
        assertFalse(SFMasterWatcherListener.shouldAlertOwnerMismatch(alerts, "player:item", 2_999L));
        assertTrue(SFMasterWatcherListener.shouldAlertOwnerMismatch(alerts, "player:item", 3_000L));
    }

    @Test
    void expiresStaleAlertKeysToKeepTheThrottleBounded() {
        Map<String, Long> alerts = new HashMap<>();
        alerts.put("stale", 0L);

        assertTrue(SFMasterWatcherListener.shouldAlertOwnerMismatch(alerts, "fresh", 60_001L));
        assertFalse(alerts.containsKey("stale"));
    }
}
