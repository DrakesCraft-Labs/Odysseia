package org.metamechanists.odysseia.listeners;

/** Pure thresholds used by the automation guard and its regression tests. */
public final class AutomationGuardPolicy {
    private AutomationGuardPolicy() {
    }

    public static boolean shouldBreakClock(int fastPulses, int longPulses, boolean clockStructure,
                                           int fastLimit, int longLimit) {
        return clockStructure && (fastPulses >= fastLimit || longPulses >= longLimit);
    }

    public static boolean shouldBlockAfkMotion(long inactiveMillis, double displacedSquared,
                                               long inactivityLimitMillis, double minimumDisplacement) {
        return inactiveMillis >= inactivityLimitMillis
                && displacedSquared >= minimumDisplacement * minimumDisplacement;
    }
}
