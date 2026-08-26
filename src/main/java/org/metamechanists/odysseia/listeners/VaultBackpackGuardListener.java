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
    private static final String HOLDER_PLAYERVAULTZ = "com.rugzy.playervaultz.ui.VaultGUI";
    private static final String BYPASS = "odysseia.bovedas.mochilas-bypass";

    private final JavaPlugin plugin;
    private final boolean backpacksEnabled;
    private final String mensaje;
    private final String mensajeAccionInsegura;
    private final BackpackDetector detector;

    public VaultBackpackGuardListener(JavaPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        this.backpacksEnabled = config.getBoolean("bovedas.bloquear-mochilas.enabled", true);
        this.mensaje = config.getString("bovedas.bloquear-mochilas.mensaje",
                "&6DrakesCraft &8· &7Las &emochilas de Slimefun&7 no entran en la boveda: "
                        + "guardan un ID, no los items, y una sola valdria por cientos de slots. "
                        + "&7Usa la mochila directamente o un cofre.");
        this.mensajeAccionInsegura = config.getString("bovedas.mensaje-accion-insegura",
                "&6DrakesCraft &8· &7Por seguridad usa &eclics simples&7 dentro de /pv. "
                        + "Shift, doble clic, teclas de barra, Q y arrastre estan bloqueados.");
        this.detector = new BackpackDetector(
                config.getBoolean("bovedas.bloquear-mochilas.revisar-contenedores", true),
                config.getInt("bovedas.bloquear-mochilas.profundidad-maxima", 3));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory arriba = event.getView().getTopInventory();
        if (!esBoveda(arriba)) return;

        if (VaultClickPolicy.esAccionInsegura(event.getAction())) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensajeAccionInsegura));
            plugin.getLogger().warning("[Bovedas] Accion atomica insegura bloqueada en /pv para "
                    + player.getName() + ": " + event.getAction());
            return;
        }

        if (!backpacksEnabled || player.hasPermission(BYPASS)) return;

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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory arriba = event.getView().getTopInventory();
        if (!esBoveda(arriba)) return;

        // Un drag reparte un stack entre varios slots como una sola transaccion. Las GUIs
        // virtuales no pueden persistirla de forma atomica ante cierre o cambio de mundo.
        for (int slot : event.getRawSlots()) {
            if (slot < arriba.getSize()) {
                event.setCancelled(true);
                if (backpacksEnabled && !player.hasPermission(BYPASS)
                        && detector.contieneMochila(event.getOldCursor())) {
                    avisar(player);
                } else {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensajeAccionInsegura));
                }
                plugin.getLogger().warning("[Bovedas] Arrastre atomico bloqueado en /pv para "
                        + player.getName());
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
