package org.metamechanists.odysseia.vaults;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

/**
 * De donde sale el item que un clic meteria dentro de la boveda.
 *
 * Hay mas formas de meter algo en un inventario de las que parece, y cada una deja el item en un
 * sitio distinto del evento. Si el guard solo mira {@code getCursor()} se le cuelan el shift-click,
 * la tecla de numero y el intercambio con la mano secundaria, que es justo lo que probaria alguien
 * que quiere saltarselo.
 *
 * Esta clase no toca Bukkit mas alla de los dos enums, asi que se puede comprobar sin servidor.
 */
public final class VaultClickPolicy {

    /** Donde buscar el item que entraria a la boveda. */
    public enum Origen {
        /** Lo que el jugador lleva en el cursor. */
        CURSOR,
        /** El item del propio slot pulsado, en el inventario del jugador (shift-click). */
        SLOT_PULSADO,
        /** El slot de la barra rapida indicado por {@code getHotbarButton()}. */
        HOTBAR,
        /** El item de la mano secundaria. */
        MANO_SECUNDARIA,
        /** Este clic no mete nada en la boveda. */
        NINGUNO
    }

    private VaultClickPolicy() {
    }

    /**
     * @param rawSlot  slot pulsado en coordenadas de la vista completa
     * @param topSize  tamaño del inventario de arriba (la boveda)
     * @param click    tipo de clic
     * @param action   accion que Bukkit calculo para el clic
     * @return de donde sacar el item a revisar, o {@link Origen#NINGUNO}
     */
    public static Origen origen(int rawSlot, int topSize, ClickType click, InventoryAction action) {
        boolean enLaBoveda = rawSlot >= 0 && rawSlot < topSize;

        // Shift-click desde el inventario del jugador: el item viaja aunque se pulse abajo.
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return enLaBoveda ? Origen.NINGUNO : Origen.SLOT_PULSADO;
        }

        // El resto de formas exigen pulsar dentro de la boveda.
        if (!enLaBoveda) return Origen.NINGUNO;

        if (click == ClickType.NUMBER_KEY) return Origen.HOTBAR;
        if (click == ClickType.SWAP_OFFHAND) return Origen.MANO_SECUNDARIA;

        return switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> Origen.CURSOR;
            default -> Origen.NINGUNO;
        };
    }
}
