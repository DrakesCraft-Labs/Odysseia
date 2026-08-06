package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.vaults.BackpackDetector;
import org.metamechanists.odysseia.vaults.VaultClickPolicy;
import org.metamechanists.odysseia.vaults.VaultInventory;

/**
 * Impide meter mochilas de Slimefun dentro de una boveda.
 *
 * Lo levanto Chagui68 el 2026-08-06: una mochila no guarda sus items dentro del item, guarda un ID.
 * En una boveda ocupa un slot y da acceso a todo su contenido. Metiendo 27 mochilas en una shulker
 * y la shulker en la boveda, un solo slot pasa a valer cientos de stacks.
 *
 * Cubre las dos bovedas del servidor: las de PlayerVaultZ en el survival y las de modalidad de
 * Odysseia en las islas. PlayerVaultZ no es nuestro, asi que se reconoce por su holder; si algun
 * dia cambia de paquete, el guard deja de cubrirlo pero no rompe nada, y el aviso al arrancar lo
 * deja claro.
 */
public final class VaultBackpackGuardListener implements Listener {

    /** Holder de las bovedas de PlayerVaultZ. Se compara por nombre: no dependemos del plugin. */
    private static final String HOLDER_PLAYERVAULTZ = "com.rugzy.playervaultz.core.vault.VaultHolder";
    private static final String BYPASS = "odysseia.bovedas.mochilas-bypass";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final String mensaje;
    private final BackpackDetector detector;

    public VaultBackpackGuardListener(JavaPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("bovedas.bloquear-mochilas.enabled", true);
        this.mensaje = config.getString("bovedas.bloquear-mochilas.mensaje",
                "&6DrakesCraft &8· &7Las &emochilas de Slimefun&7 no entran en la boveda: "
                        + "guardan un ID, no los items, y una sola valdria por cientos de slots. "
                        + "&7Usa la mochila directamente o un cofre.");
        this.detector = new BackpackDetector(
                config.getBoolean("bovedas.bloquear-mochilas.revisar-contenedores", true),
                config.getInt("bovedas.bloquear-mochilas.profundidad-maxima", 3));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!enabled || !(event.getWhoClicked() instanceof Player player)) return;

        Inventory arriba = event.getView().getTopInventory();
        if (!esBoveda(arriba) || player.hasPermission(BYPASS)) return;

        ItemStack candidato = switch (VaultClickPolicy.origen(
                event.getRawSlot(), arriba.getSize(), event.getClick(), event.getAction())) {
            case CURSOR -> event.getCursor();
            case SLOT_PULSADO -> event.getCurrentItem();
            case HOTBAR -> event.getHotbarButton() < 0 ? null
                    : player.getInventory().getItem(event.getHotbarButton());
            case MANO_SECUNDARIA -> player.getInventory().getItemInOffHand();
            case NINGUNO -> null;
        };

        if (detector.contieneMochila(candidato)) {
            event.setCancelled(true);
            avisar(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!enabled || !(event.getWhoClicked() instanceof Player player)) return;

        Inventory arriba = event.getView().getTopInventory();
        if (!esBoveda(arriba) || player.hasPermission(BYPASS)) return;
        if (!detector.contieneMochila(event.getOldCursor())) return;

        // Arrastrar reparte el stack entre varios slots: basta con que uno caiga en la boveda.
        for (int slot : event.getRawSlots()) {
            if (slot < arriba.getSize()) {
                event.setCancelled(true);
                avisar(player);
                return;
            }
        }
    }

    /** True si el inventario de arriba es una boveda, sea de PlayerVaultZ o de Odysseia. */
    private static boolean esBoveda(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder == null) return false;
        return holder instanceof VaultInventory
                || holder.getClass().getName().equals(HOLDER_PLAYERVAULTZ);
    }

    private void avisar(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensaje));
        plugin.getLogger().fine("[Bovedas] Mochila bloqueada al entrar a la boveda de " + player.getName());
    }
}
