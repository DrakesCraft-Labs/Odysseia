package org.metamechanists.odysseia.listeners;

import io.papermc.paper.advancement.AdvancementDisplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ModalityAdvancementListenerTest {

    @Test
    void cadaTipoTieneUnAnuncioLegible() {
        for (AdvancementDisplay.Frame type : AdvancementDisplay.Frame.values()) {
            assertFalse(ModalityAdvancementListener.phrase(type).isBlank());
        }
    }
}
