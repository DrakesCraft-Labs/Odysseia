package org.metamechanists.odysseia.kits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterKitPolicyTest {

    @Test
    void enrollsOnlyARealFirstJoinOrAnInterruptedPendingDelivery() {
        assertTrue(StarterKitPolicy.shouldEnroll(false, false, false, false));
        assertTrue(StarterKitPolicy.shouldEnroll(true, true, false, false));
        assertFalse(StarterKitPolicy.shouldEnroll(true, false, false, false));
    }

    @Test
    void neverRepeatsDeliveredOrClaimedKitsIncludingForOperators() {
        assertFalse(StarterKitPolicy.shouldEnroll(false, true, true, false));
        assertFalse(StarterKitPolicy.shouldEnroll(false, true, false, true));
    }
}
