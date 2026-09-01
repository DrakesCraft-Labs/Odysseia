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

    /**
     * Igual que {@link #esAccionInsegura(InventoryAction)}, pero mirando donde se pulso.
     *
     * Tirar al suelo con Q o cambiar un item de sitio con la tecla de numero DENTRO del inventario
     * del propio jugador no toca la boveda ni cruza nada: se resuelve entero abajo. Bloquearlo solo
     * porque la boveda esta abierta no protegia ningun item y si rompia el juego normal --el log de
     * produccion se llenaba de HOTBAR_SWAP y DROP_ALL_SLOT de jugadores que no estaban haciendo
     * nada raro--. Las que si cruzan (shift-click y doble clic) siguen bloqueadas desde los dos
     * lados, y el cursor fuera de la ventana tambien, porque puede venir cargado de la boveda.
     *
     * @param rawSlot slot pulsado en coordenadas de la vista; negativo si es fuera de la ventana
     * @param topSize tamaño del inventario de arriba (la boveda)
     */
    public static boolean esAccionInsegura(InventoryAction action, int rawSlot, int topSize) {
        if (!esAccionInsegura(action)) return false;
        // rawSlot < topSize cubre tanto la boveda como el -999 de "fuera de la ventana".
        if (rawSlot < topSize) return true;
        return switch (action) {
            case DROP_ALL_SLOT, DROP_ONE_SLOT, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> false;
            default -> true;
        };
    }

    /**
     * Indica si Bukkit intentaria resolver varios slots como una unica transaccion no serializable.
     * Los sorters normales conservan PICKUP, PLACE, MOVE y SWAP; solo se rechazan las operaciones
     * que pueden trabajar sobre una instantanea obsoleta de una GUI virtual.
     *
     * Sin contexto de slot es la respuesta conservadora. Para decidir sobre un clic concreto usa
     * {@link #esAccionInsegura(InventoryAction, int, int)}.
     */
    public static boolean esAccionInsegura(InventoryAction action) {
        return action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.CLONE_STACK
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.HOTBAR_MOVE_AND_READD
                || action == InventoryAction.DROP_ALL_SLOT
                || action == InventoryAction.DROP_ONE_SLOT
                || action == InventoryAction.DROP_ALL_CURSOR
                || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.UNKNOWN;
    }
}
