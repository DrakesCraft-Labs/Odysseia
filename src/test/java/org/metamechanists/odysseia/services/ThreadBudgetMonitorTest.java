package org.metamechanists.odysseia.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadBudgetMonitorTest {

    @Test
    void agrupaLosHilosNumeradosDelPoolAsyncDeBukkit() {
        assertEquals("Craft Scheduler Thread", ThreadBudgetMonitor.familyOf("Craft Scheduler Thread - 407"));
        assertEquals("Craft Scheduler Thread", ThreadBudgetMonitor.familyOf("Craft Scheduler Thread - 92"));
        assertEquals("bStats-Metrics", ThreadBudgetMonitor.familyOf("bStats-Metrics"));
        assertEquals("Netty Epoll IO", ThreadBudgetMonitor.familyOf("Netty Epoll IO #7"));
        assertEquals("pool", ThreadBudgetMonitor.familyOf("pool-3"));
    }

    @Test
    void conservaLosNombresQueNoLlevanIndice() {
        assertEquals("Server thread", ThreadBudgetMonitor.familyOf("Server thread"));
        assertEquals("main", ThreadBudgetMonitor.familyOf("main"));
    }

    @Test
    void toleraNombresVaciosONumericos() {
        assertEquals("(sin nombre)", ThreadBudgetMonitor.familyOf(null));
        assertEquals("(sin nombre)", ThreadBudgetMonitor.familyOf("   "));
        // Un nombre solo de digitos no tiene familia que extraer: se deja tal cual.
        assertEquals("12345", ThreadBudgetMonitor.familyOf("12345"));
    }
}
