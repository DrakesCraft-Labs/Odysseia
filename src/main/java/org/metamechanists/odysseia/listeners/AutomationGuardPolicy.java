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
}
