package org.metamechanists.odysseia.listeners;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalityBackGuardListenerTest {

    @Test
    void recognizesBackCommandsAndAliases() {
        assertTrue(ModalityBackGuardListener.isBackCommand("/back"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/eback"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/return"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/ereturn"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/essentials:back"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/odysseia:back"));
        assertTrue(ModalityBackGuardListener.isBackCommand("/back player"));
    }

    @Test
    void ignoresUnrelatedCommands() {
        assertFalse(ModalityBackGuardListener.isBackCommand("/spawn"));
        assertFalse(ModalityBackGuardListener.isBackCommand("/home"));
        assertFalse(ModalityBackGuardListener.isBackCommand("/warp"));
        assertFalse(ModalityBackGuardListener.isBackCommand("back"));
        assertFalse(ModalityBackGuardListener.isBackCommand(null));
        assertFalse(ModalityBackGuardListener.isBackCommand(""));
    }

    @Test
    void isolatesLocationsStrictlyByModality() {
        ModalityBackGuardListener listener = new ModalityBackGuardListener(null, null);
        UUID playerId = UUID.randomUUID();

        Location clasicoLoc = new Location(null, 100, 64, 200);
        Location survivalLoc = new Location(null, 500, 70, -300);

        // Registrar ubicación en clásico
        listener.recordLocation(playerId, "clasico", clasicoLoc);

        Location retrievedClasico = listener.getLastLocation(playerId, "clasico");
        assertNotNull(retrievedClasico);
        assertEquals(100, retrievedClasico.getX());
        assertNull(listener.getLastLocation(playerId, "survival"));

        // Registrar ubicación en survival
        listener.recordLocation(playerId, "survival", survivalLoc);

        Location retrievedSurvival = listener.getLastLocation(playerId, "survival");
        assertNotNull(retrievedSurvival);
        assertEquals(500, retrievedSurvival.getX());

        // Asegurar que clásico no fue sobreescrito ni contaminado
        assertEquals(100, listener.getLastLocation(playerId, "clasico").getX());
    }
}
