package org.metamechanists.odysseia.vaults;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;
import org.metamechanists.odysseia.vaults.VaultClickPolicy.Origen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las formas de meter un item en una boveda.
 *
 * Un guard que solo mire el cursor deja pasar el shift-click, la tecla de numero y el intercambio
 * con la mano secundaria, que es exactamente lo que probaria alguien que quiere saltarselo. Estas
 * pruebas fijan las cuatro puertas.
 */
class VaultClickPolicyTest {

    private static final int BOVEDA = 54;   // seis filas

    @Test
    void dejarElItemDelCursorEnLaBoveda() {
        assertEquals(Origen.CURSOR,
                VaultClickPolicy.origen(10, BOVEDA, ClickType.LEFT, InventoryAction.PLACE_ALL));
        assertEquals(Origen.CURSOR,
                VaultClickPolicy.origen(10, BOVEDA, ClickType.RIGHT, InventoryAction.PLACE_ONE));
        assertEquals(Origen.CURSOR,
                VaultClickPolicy.origen(10, BOVEDA, ClickType.LEFT, InventoryAction.SWAP_WITH_CURSOR));
    }

    @Test
    void shiftClickDesdeElInventarioDelJugador() {
        // Se pulsa ABAJO (slot >= 54) pero el item viaja hacia arriba igual.
        assertEquals(Origen.SLOT_PULSADO,
                VaultClickPolicy.origen(60, BOVEDA, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    @Test
    void shiftClickDentroDeLaBovedaSacaElItem_noLoMete() {
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(5, BOVEDA, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    @Test
    void teclaDeNumeroSobreUnSlotDeLaBoveda() {
        assertEquals(Origen.HOTBAR,
                VaultClickPolicy.origen(3, BOVEDA, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void intercambioConLaManoSecundaria() {
        assertEquals(Origen.MANO_SECUNDARIA,
                VaultClickPolicy.origen(3, BOVEDA, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void sacarDeLaBovedaNoSeRevisa() {
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(3, BOVEDA, ClickType.LEFT, InventoryAction.PICKUP_ALL));
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(3, BOVEDA, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR));
    }

    @Test
    void moverseDentroDelPropioInventarioNoTocaLaBoveda() {
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(60, BOVEDA, ClickType.LEFT, InventoryAction.PLACE_ALL));
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(70, BOVEDA, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void unSlotFueraDeLaVistaNoCuenta() {
        // Bukkit usa -999 para el clic fuera de la ventana.
        assertEquals(Origen.NINGUNO,
                VaultClickPolicy.origen(-999, BOVEDA, ClickType.LEFT, InventoryAction.PLACE_ALL));
    }

    @Test
    void bloqueaSoloTransaccionesAtomicasPeligrosas() {
        assertTrue(VaultClickPolicy.esAccionInsegura(InventoryAction.COLLECT_TO_CURSOR));
        assertTrue(VaultClickPolicy.esAccionInsegura(InventoryAction.CLONE_STACK));
        assertTrue(VaultClickPolicy.esAccionInsegura(InventoryAction.UNKNOWN));

        assertFalse(VaultClickPolicy.esAccionInsegura(InventoryAction.PICKUP_ALL));
        assertFalse(VaultClickPolicy.esAccionInsegura(InventoryAction.PLACE_ALL));
        assertFalse(VaultClickPolicy.esAccionInsegura(InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertFalse(VaultClickPolicy.esAccionInsegura(InventoryAction.HOTBAR_SWAP));
    }
}
