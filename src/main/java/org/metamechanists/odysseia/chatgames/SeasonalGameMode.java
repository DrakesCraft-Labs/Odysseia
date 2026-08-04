package org.metamechanists.odysseia.chatgames;

import java.time.LocalDate;

/** The game changes each calendar week and deliberately restarts its sequence on day one of a month. */
public enum SeasonalGameMode {
    FORGE_MATH("Forja mental"),
    RUNIC_SCRAMBLE("Runas desordenadas"),
    ORACLE_TRIVIA("Oraculo breve"),
    HERMES_REFLEX("Reflejos de Hermes"),
    CARTOGRAPHER_CODE("Codigo cartografo");

    private final String displayName;

    SeasonalGameMode(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }

    public static SeasonalGameMode forDate(LocalDate date) {
        int weekOfMonth = Math.min(values().length - 1, (date.getDayOfMonth() - 1) / 7);
        return values()[weekOfMonth];
    }
}
