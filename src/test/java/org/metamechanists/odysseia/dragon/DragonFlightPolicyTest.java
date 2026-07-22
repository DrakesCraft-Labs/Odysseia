package org.metamechanists.odysseia.dragon;

import org.junit.jupiter.api.Test;
import org.bukkit.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragonFlightPolicyTest {

    @Test
    void clampsUnsafeSpeeds() {
        assertEquals(0.5, DragonFlightPolicy.clampSpeed(-20));
        assertEquals(1.0, DragonFlightPolicy.clampSpeed(1.0));
        assertEquals(1.5, DragonFlightPolicy.clampSpeed(20));
        assertEquals(0.5, DragonFlightPolicy.clampSpeed(Double.NaN));
    }

    @Test
    void keepsFlightInsideBuildHeight() {
        assertEquals(-56.0, DragonFlightPolicy.clampY(-500, -64, 320));
        assertEquals(80.0, DragonFlightPolicy.clampY(80, -64, 320));
        assertEquals(312.0, DragonFlightPolicy.clampY(500, -64, 320));
    }

    @Test
    void leadsAndSmoothsTheVisualDragon() {
        assertEquals(4.0, DragonFlightPolicy.visualLeadDistance(1.0));
        Vector velocity = DragonFlightPolicy.visualVelocity(
                new Vector(1, 0, 0), 1.0, new Vector(2, 0, 0));
        assertEquals(2.1, velocity.getX(), 0.0001);
        assertEquals(0.0, velocity.getY(), 0.0001);
    }

    @Test
    void snapsOnlyAfterARealVisualDesync() {
        assertFalse(DragonFlightPolicy.shouldSnapVisual(144.0));
        assertTrue(DragonFlightPolicy.shouldSnapVisual(144.01));
        assertTrue(DragonFlightPolicy.shouldSnapVisual(Double.NaN));
    }
}
