package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationGuardPolicyTest {
    @Test
    void clockRequiresBothRepeatedPulsesAndAClockStructure() {
        assertEquals(AutomationGuardPolicy.ClockAction.ALLOW, AutomationGuardPolicy.evaluateClock(50, 50, false, 12, 8, 0, 1));
        assertEquals(AutomationGuardPolicy.ClockAction.ALLOW, AutomationGuardPolicy.evaluateClock(3, 3, true, 12, 8, 0, 1));
        assertEquals(AutomationGuardPolicy.ClockAction.BREAK, AutomationGuardPolicy.evaluateClock(12, 4, true, 12, 8, 0, 1));
        assertEquals(AutomationGuardPolicy.ClockAction.BREAK, AutomationGuardPolicy.evaluateClock(2, 8, true, 12, 8, 0, 1));
    }

    @Test
    void afkMotionRequiresInactivityAndMeaningfulDisplacement() {
        assertFalse(AutomationGuardPolicy.shouldBlockAfkMotion(299_000L, 100.0D, 300_000L, 4.0D));
        assertFalse(AutomationGuardPolicy.shouldBlockAfkMotion(300_000L, 15.9D, 300_000L, 4.0D));
        assertTrue(AutomationGuardPolicy.shouldBlockAfkMotion(300_000L, 16.0D, 300_000L, 4.0D));
    }
}
