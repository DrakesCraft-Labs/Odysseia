package org.metamechanists.odysseia.dragon;

/** Pure safety limits shared by the flight loop and its tests. */
final class DragonFlightPolicy {
    static final double MIN_SPEED = 0.5;
    static final double MAX_SPEED = 1.5;
    private static final double VERTICAL_MARGIN = 8.0;

    private DragonFlightPolicy() {
    }

    static double clampSpeed(double speed) {
        if (!Double.isFinite(speed)) return MIN_SPEED;
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }

    static double clampY(double y, int minHeight, int maxHeight) {
        double minimum = minHeight + VERTICAL_MARGIN;
        double maximum = maxHeight - VERTICAL_MARGIN;
        if (!Double.isFinite(y)) return minimum;
        return Math.max(minimum, Math.min(maximum, y));
    }
}
