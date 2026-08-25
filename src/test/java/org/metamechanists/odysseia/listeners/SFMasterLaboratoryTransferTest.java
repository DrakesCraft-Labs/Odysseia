package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SFMasterLaboratoryTransferTest {

    @Test
    void permitsOnlyAutomationWhoseTwoEndsAreInsideLaboratory() {
        assertTrue(SFMasterWatcherListener.allowsLaboratoryInventoryMove("laboratorio", "laboratorio"));
        assertFalse(SFMasterWatcherListener.allowsLaboratoryInventoryMove("laboratorio", "world"));
        assertFalse(SFMasterWatcherListener.allowsLaboratoryInventoryMove("world", "laboratorio"));
        assertFalse(SFMasterWatcherListener.allowsLaboratoryInventoryMove(null, "laboratorio"));
    }

    @Test
    void recognizesTheLaboratoryWorldWithoutCaseSensitivity() {
        assertTrue(SFMasterWatcherListener.isLaboratoryWorld("Laboratorio"));
        assertFalse(SFMasterWatcherListener.isLaboratoryWorld("oneblock_world"));
    }
}
