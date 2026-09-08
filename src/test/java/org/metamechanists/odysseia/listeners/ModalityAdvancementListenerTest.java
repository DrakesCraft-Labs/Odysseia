package org.metamechanists.odysseia.listeners;

import io.papermc.paper.advancement.AdvancementDisplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalityAdvancementListenerTest {

    @Test
    void cadaTipoTieneUnAnuncioLegible() {
        for (AdvancementDisplay.Frame type : AdvancementDisplay.Frame.values()) {
            assertFalse(ModalityAdvancementListener.phrase(type).isBlank());
        }
    }

    @Test
    void distingueAvancesVanillaDeLosDePlugins() {
        assertFalse(ModalityAdvancementListener.esAvanceExterno("minecraft"));
        assertTrue(ModalityAdvancementListener.esAvanceExterno("slimefun"));
        assertTrue(ModalityAdvancementListener.esAvanceExterno("cultivation"));
    }
}
