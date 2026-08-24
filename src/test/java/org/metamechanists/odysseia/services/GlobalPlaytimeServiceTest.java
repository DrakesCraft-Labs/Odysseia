package org.metamechanists.odysseia.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalPlaytimeServiceTest {

    @Test
    void formatsPersistentDurationsCompactly() {
        assertEquals("0s", GlobalPlaytimeService.formatDuration(0));
        assertEquals("4m 58s", GlobalPlaytimeService.formatDuration(298_000));
        assertEquals("3h 7m", GlobalPlaytimeService.formatDuration((3 * 3_600L + 7 * 60L) * 1_000L));
        assertEquals("2d 5h", GlobalPlaytimeService.formatDuration((2 * 86_400L + 5 * 3_600L) * 1_000L));
    }
}
