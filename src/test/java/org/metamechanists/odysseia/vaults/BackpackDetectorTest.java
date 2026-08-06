package org.metamechanists.odysseia.vaults;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La marca que Slimefun deja en el lore de toda mochila.
 *
 * Es el camino de respaldo, el que se usa si Slimefun no esta cargado. Importa que reconozca tanto
 * la mochila recien fabricada (todavia con el marcador sin sustituir) como la ya vinculada, y que
 * no confunda un item cualquiera que hable de un "ID".
 */
class BackpackDetectorTest {

    private static final String GRIS = ChatColor.GRAY.toString();

    @Test
    void reconoceUnaMochilaYaVinculada() {
        assertTrue(BackpackDetector.esLineaDeIdDeMochila(GRIS + "ID: 3f8a11c2-0e5b-4a7d-9c31-77e2a1b0d4f6#12"));
    }

    @Test
    void reconoceUnaMochilaRecienFabricada() {
        // Antes del primer uso Slimefun deja el marcador literal sin sustituir.
        assertTrue(BackpackDetector.esLineaDeIdDeMochila(GRIS + "ID: <ID>"));
    }

    @Test
    void noConfundeOtroTextoQueMencioneUnId() {
        assertFalse(BackpackDetector.esLineaDeIdDeMochila(GRIS + "Canal ID: 3"));
        assertFalse(BackpackDetector.esLineaDeIdDeMochila(GRIS + "Size: 27"));
        assertFalse(BackpackDetector.esLineaDeIdDeMochila("ID: 12"));   // sin el color, no es de Slimefun
    }

    @Test
    void toleraLineasVacias() {
        assertFalse(BackpackDetector.esLineaDeIdDeMochila(null));
        assertFalse(BackpackDetector.esLineaDeIdDeMochila(""));
    }
}
