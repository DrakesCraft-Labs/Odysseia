package org.metamechanists.odysseia.listeners;

/** Pure thresholds used by the automation guard and its regression tests. */
public final class AutomationGuardPolicy {
    public enum ClockAction {
        ALLOW,
        THROTTLE,
        BREAK
    }

    private AutomationGuardPolicy() {
    }

    public static ClockAction evaluateClock(int fastPulses, int longPulses, boolean clockStructure,
                                            int fastLimit, int longLimit, int priorViolations,
                                            int violationsBeforeBreak) {
        if (!clockStructure || (fastPulses < fastLimit && longPulses < longLimit)) {
            return ClockAction.ALLOW;
        }
        return priorViolations + 1 >= Math.max(1, violationsBeforeBreak)
                ? ClockAction.BREAK : ClockAction.THROTTLE;
    }

    public static boolean shouldBlockAfkMotion(long inactiveMillis, double displacedSquared,
                                               long inactivityLimitMillis, double minimumDisplacement) {
        return inactiveMillis >= inactivityLimitMillis
                && displacedSquared >= minimumDisplacement * minimumDisplacement;
    }

    public static long calculateQuarantineSeconds(int strikes, long strike1, long strike2, long strike3) {
        if (strikes <= 1) return strike1;
        if (strikes == 2) return strike2;
        return strike3;
    }

    public static boolean isQuarantined(long now, long quarantineUntil) {
        return now < quarantineUntil;
    }

    public static boolean shouldResetStrikes(long now, long lastKickTime, long decayMillis) {
        return lastKickTime > 0 && (now - lastKickTime >= decayMillis);
    }

    public static boolean isGenuineLookChange(float yawFrom, float yawTo, float pitchFrom, float pitchTo) {
        return Math.abs(yawFrom - yawTo) >= 15.0F || Math.abs(pitchFrom - pitchTo) >= 10.0F;
    }

    public static boolean shouldKickUnverifiedJoin(long now, long joinTime, int actionsObserved,
                                                  long verificationLimitMillis, int requiredActions) {
        return (now - joinTime >= verificationLimitMillis) && (actionsObserved < requiredActions);
    }

    public static boolean shouldKickEvasion(long inactiveMillis, long evasionLimitMillis) {
        return inactiveMillis >= evasionLimitMillis;
    }
}
