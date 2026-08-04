package org.metamechanists.odysseia.chatgames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SeasonalGameModeTest {
    @Test
    void rotatesByWeekAndRestartsAtEachNewMonth() {
        assertEquals(SeasonalGameMode.FORGE_MATH, SeasonalGameMode.forDate(LocalDate.of(2026, 7, 1)));
        assertEquals(SeasonalGameMode.RUNIC_SCRAMBLE, SeasonalGameMode.forDate(LocalDate.of(2026, 7, 8)));
        assertEquals(SeasonalGameMode.ORACLE_TRIVIA, SeasonalGameMode.forDate(LocalDate.of(2026, 7, 15)));
        assertEquals(SeasonalGameMode.HERMES_REFLEX, SeasonalGameMode.forDate(LocalDate.of(2026, 7, 22)));
        assertEquals(SeasonalGameMode.FORGE_MATH, SeasonalGameMode.forDate(LocalDate.of(2026, 8, 1)));
    }
}
