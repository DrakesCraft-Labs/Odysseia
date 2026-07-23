package org.metamechanists.odysseia.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class RestartScheduleTest {
    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

    @Test
    void schedulesTodayWhenDailyTimeIsStillAhead() {
        ZonedDateTime now = ZonedDateTime.of(2026, 7, 23, 4, 30, 0, 0, SANTIAGO);

        assertEquals(
                ZonedDateTime.of(2026, 7, 23, 5, 0, 0, 0, SANTIAGO),
                RestartSchedule.next(now, "DAILY", 5, 0));
    }

    @Test
    void schedulesTomorrowWhenDailyTimeAlreadyPassed() {
        ZonedDateTime now = ZonedDateTime.of(2026, 7, 23, 5, 0, 0, 0, SANTIAGO);

        assertEquals(
                ZonedDateTime.of(2026, 7, 24, 5, 0, 0, 0, SANTIAGO),
                RestartSchedule.next(now, "daily", 5, 0));
    }

    @Test
    void keepsWeeklySchedulesAvailable() {
        ZonedDateTime thursday = ZonedDateTime.of(2026, 7, 23, 12, 0, 0, 0, SANTIAGO);

        assertEquals(
                ZonedDateTime.of(2026, 7, 27, 5, 0, 0, 0, SANTIAGO),
                RestartSchedule.next(thursday, "MONDAY", 5, 0));
    }

    @Test
    void rejectsUnknownDays() {
        ZonedDateTime now = ZonedDateTime.of(2026, 7, 23, 12, 0, 0, 0, SANTIAGO);

        assertThrows(IllegalArgumentException.class, () -> RestartSchedule.next(now, "FUNDAY", 5, 0));
    }
}
