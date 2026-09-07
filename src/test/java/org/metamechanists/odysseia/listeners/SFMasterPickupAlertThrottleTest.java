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

    @Test
    void collapsesConsoleNoticesFromManyItemsIntoOneLinePerPlayerAndWindow() {
        Map<String, Long> logs = new HashMap<>();

        assertTrue(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "player", 1_000L));
        // Distintas entidades del mismo jugador dentro de la ventana: la consola no se repite.
        assertFalse(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "player", 3_000L));
        assertFalse(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "player", 60_999L));
        assertTrue(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "player", 61_000L));
    }

    @Test
    void logThrottleIsPerPlayerAndBounded() {
        Map<String, Long> logs = new HashMap<>();
        logs.put("viejo", 0L);

        assertTrue(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "otro", 1_000L));
        assertTrue(SFMasterWatcherListener.shouldLogOwnerMismatch(logs, "nuevo", 60_001L));
        assertFalse(logs.containsKey("viejo"));
    }
}
