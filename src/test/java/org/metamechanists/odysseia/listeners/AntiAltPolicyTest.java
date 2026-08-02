package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiAltPolicyTest {
    @Test
    void allowsConfiguredSharedHouseholdCapacityOnly() {
        assertFalse(AntiAltPolicy.shouldReject(1, 2));
        assertTrue(AntiAltPolicy.shouldReject(2, 2));
        assertTrue(AntiAltPolicy.shouldReject(1, 1));
    }
}
