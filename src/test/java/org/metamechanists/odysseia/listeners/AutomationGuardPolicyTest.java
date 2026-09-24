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

    @Test
    void quarantineSecondsScaleWithStrikes() {
        assertEquals(180L, AutomationGuardPolicy.calculateQuarantineSeconds(1, 180L, 600L, 1800L));
        assertEquals(600L, AutomationGuardPolicy.calculateQuarantineSeconds(2, 180L, 600L, 1800L));
        assertEquals(1800L, AutomationGuardPolicy.calculateQuarantineSeconds(3, 180L, 600L, 1800L));
        assertEquals(1800L, AutomationGuardPolicy.calculateQuarantineSeconds(4, 180L, 600L, 1800L));
    }

    @Test
    void quarantineStatusCheck() {
        long now = 100_000L;
        assertTrue(AutomationGuardPolicy.isQuarantined(now, 100_001L));
        assertFalse(AutomationGuardPolicy.isQuarantined(now, 100_000L));
        assertFalse(AutomationGuardPolicy.isQuarantined(now, 99_999L));
        assertFalse(AutomationGuardPolicy.isQuarantined(now, 0L));
    }

    @Test
    void verifyJoinRequiresAtLeastTwoStrikes() {
        assertFalse(AutomationGuardPolicy.shouldVerifyJoin(0));
        assertFalse(AutomationGuardPolicy.shouldVerifyJoin(1));
        assertTrue(AutomationGuardPolicy.shouldVerifyJoin(2));
        assertTrue(AutomationGuardPolicy.shouldVerifyJoin(3));
    }

    @Test
    void meaningfulMovementCheck() {
        assertFalse(AutomationGuardPolicy.isMeaningfulMovement(3.9D, 2.0D));
        assertTrue(AutomationGuardPolicy.isMeaningfulMovement(4.0D, 2.0D));
        assertTrue(AutomationGuardPolicy.isMeaningfulMovement(9.0D, 2.0D));
    }

    @Test
    void strikesDecayAfterDecayWindow() {
        long decay = 2L * 3600L * 1000L;
        long lastKick = 1_000_000L;
        assertFalse(AutomationGuardPolicy.shouldResetStrikes(lastKick + decay - 1, lastKick, decay));
        assertTrue(AutomationGuardPolicy.shouldResetStrikes(lastKick + decay, lastKick, decay));
        assertTrue(AutomationGuardPolicy.shouldResetStrikes(lastKick + decay + 1000, lastKick, decay));
    }

    @Test
    void lookChangeDetection() {
        assertFalse(AutomationGuardPolicy.isGenuineLookChange(0.0f, 5.0f, 0.0f, 4.0f));
        assertTrue(AutomationGuardPolicy.isGenuineLookChange(0.0f, 8.5f, 0.0f, 0.0f));
        assertTrue(AutomationGuardPolicy.isGenuineLookChange(0.0f, 0.0f, 0.0f, 6.5f));
    }

    @Test
    void unverifiedJoinKickCondition() {
        long join = 1_000_000L;
        long timeout = 300_000L;
        assertFalse(AutomationGuardPolicy.shouldKickUnverifiedJoin(join + 200_000L, join, 0, timeout, 3));
        assertFalse(AutomationGuardPolicy.shouldKickUnverifiedJoin(join + 301_000L, join, 3, timeout, 3));
        assertTrue(AutomationGuardPolicy.shouldKickUnverifiedJoin(join + 301_000L, join, 2, timeout, 3));
    }

    @Test
    void afkEvasionCondition() {
        assertFalse(AutomationGuardPolicy.shouldKickEvasion(899_000L, 900_000L));
        assertTrue(AutomationGuardPolicy.shouldKickEvasion(900_000L, 900_000L));
    }

    @Test
    void duplicateKickDebounceCheck() {
        long lastKick = 1_000_000L;
        long debounce = 5_000L;
        assertFalse(AutomationGuardPolicy.shouldIgnoreDuplicateKick(lastKick, 0L, debounce));
        assertTrue(AutomationGuardPolicy.shouldIgnoreDuplicateKick(lastKick + 100L, lastKick, debounce));
        assertTrue(AutomationGuardPolicy.shouldIgnoreDuplicateKick(lastKick + 4_999L, lastKick, debounce));
        assertFalse(AutomationGuardPolicy.shouldIgnoreDuplicateKick(lastKick + 5_000L, lastKick, debounce));
        assertFalse(AutomationGuardPolicy.shouldIgnoreDuplicateKick(lastKick + 10_000L, lastKick, debounce));
    }
}
