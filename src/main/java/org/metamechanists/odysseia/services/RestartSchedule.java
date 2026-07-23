package org.metamechanists.odysseia.services;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Calculates wall-clock restart dates without depending on the Bukkit runtime. */
public final class RestartSchedule {
    private RestartSchedule() {
    }

    /**
     * Returns the next configured restart strictly after {@code now}.
     * The special day value {@code DAILY} schedules every calendar day.
     */
    public static ZonedDateTime next(ZonedDateTime now, String configuredDay, int hour, int minute) {
        if (now == null) {
            throw new IllegalArgumentException("now no puede ser null");
        }

        int safeHour = Math.max(0, Math.min(23, hour));
        int safeMinute = Math.max(0, Math.min(59, minute));
        String normalizedDay = configuredDay == null
                ? "MONDAY"
                : configuredDay.trim().toUpperCase(Locale.ROOT);

        ZonedDateTime candidate = now.withHour(safeHour).withMinute(safeMinute).withSecond(0).withNano(0);
        if ("DAILY".equals(normalizedDay)) {
            return candidate.isAfter(now) ? candidate : candidate.plusDays(1);
        }

        DayOfWeek targetDay = DayOfWeek.valueOf(normalizedDay);
        candidate = candidate.with(TemporalAdjusters.nextOrSame(targetDay));
        return candidate.isAfter(now) ? candidate : candidate.with(TemporalAdjusters.next(targetDay));
    }
}
