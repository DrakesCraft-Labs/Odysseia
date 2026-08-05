package org.metamechanists.odysseia.dragon;

import org.bukkit.util.Vector;

/** Pure safety limits shared by the flight loop and its tests. */
final class DragonFlightPolicy {
    static final double MIN_SPEED = 0.5;
    /** Tope duro. Se subio de 1.5 para que el acelerón con sprint se note de verdad. */
    static final double MAX_SPEED = 2.2;
    /** Multiplicadores de sprint y sneak sobre la velocidad de crucero. */
    static final double BOOST = 1.7;
    static final double BRAKE = 0.45;
    private static final double VERTICAL_MARGIN = 8.0;
    private static final double VISUAL_LEAD_TICKS = 4.0;
    private static final double VISUAL_FOLLOW_GAIN = 0.55;
    private static final double VISUAL_MAX_SPEED = 3.0;
    private static final double VISUAL_SNAP_DISTANCE_SQUARED = 144.0;

    private DragonFlightPolicy() {
    }

    static double clampSpeed(double speed) {
        if (!Double.isFinite(speed)) return MIN_SPEED;
        return Math.max(MIN_SPEED, Math.min(MAX_SPEED, speed));
    }

    /**
     * Velocidad efectiva segun lo que esta haciendo el jinete.
     *
     * Volar solo con la mirada se sentia inerte: la velocidad era fija por variante y solo se
     * cambiaba con un comando. Correr acelera, agacharse frena, y pulsar ambos vuelve al crucero.
     */
    static double throttle(double cruise, boolean sprinting, boolean sneaking) {
        double base = Double.isFinite(cruise) ? cruise : MIN_SPEED;
        if (sprinting == sneaking) return clampSpeed(base);
        return clampSpeed(sprinting ? base * BOOST : base * BRAKE);
    }

    static double clampY(double y, int minHeight, int maxHeight) {
        double minimum = minHeight + VERTICAL_MARGIN;
        double maximum = maxHeight - VERTICAL_MARGIN;
        if (!Double.isFinite(y)) return minimum;
        return Math.max(minimum, Math.min(maximum, y));
    }

    static double visualLeadDistance(double speed) {
        return clampSpeed(speed) * VISUAL_LEAD_TICKS;
    }

    /** Keeps the model ahead of client interpolation without moving the rider. */
    static Vector visualVelocity(Vector direction, double speed, Vector correction) {
        Vector velocity = direction.clone().normalize().multiply(clampSpeed(speed));
        velocity.add(correction.clone().multiply(VISUAL_FOLLOW_GAIN));
        if (velocity.lengthSquared() > VISUAL_MAX_SPEED * VISUAL_MAX_SPEED) {
            velocity.normalize().multiply(VISUAL_MAX_SPEED);
        }
        return velocity;
    }

    static boolean shouldSnapVisual(double distanceSquared) {
        return !Double.isFinite(distanceSquared) || distanceSquared > VISUAL_SNAP_DISTANCE_SQUARED;
    }
}
