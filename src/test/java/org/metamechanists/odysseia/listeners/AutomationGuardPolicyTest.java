package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationGuardPolicyTest {
    @Test
    void clockRequiresBothRepeatedPulsesAndAClockStructure() {
        assertFalse(AutomationGuardPolicy.shouldBreakClock(50, 50, false, 12, 8));
        assertFalse(AutomationGuardPolicy.shouldBreakClock(3, 3, true, 12, 8));
        assertTrue(AutomationGuardPolicy.shouldBreakClock(12, 4, true, 12, 8));
        assertTrue(AutomationGuardPolicy.shouldBreakClock(2, 8, true, 12, 8));
    }

    @Test
    void afkMotionRequiresInactivityAndMeaningfulDisplacement() {
        assertFalse(AutomationGuardPolicy.shouldBlockAfkMotion(299_000L, 100.0D, 300_000L, 4.0D));
        assertFalse(AutomationGuardPolicy.shouldBlockAfkMotion(300_000L, 15.9D, 300_000L, 4.0D));
        assertTrue(AutomationGuardPolicy.shouldBlockAfkMotion(300_000L, 16.0D, 300_000L, 4.0D));
    }
}
