package org.metamechanists.odysseia.dragon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
