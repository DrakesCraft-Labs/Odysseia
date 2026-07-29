package org.metamechanists.odysseia.listeners;

import java.util.Locale;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.metamechanists.odysseia.Odysseia;

/**
 * Short maintenance window used by the external Pterodactyl restart flow.
 * It prevents players from leaving items inside transactional menus while
 * preserving ordinary movement and combat until shutdown.
 */
public final class MaintenanceGuardListener implements Listener {
    private static final Set<Material> RISKY_BLOCKS = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.ENDER_CHEST, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
            Material.GRINDSTONE, Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
            Material.ENCHANTING_TABLE, Material.SMITHING_TABLE, Material.STONECUTTER,
            Material.CARTOGRAPHY_TABLE, Material.LOOM, Material.BREWING_STAND,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER);

    private final Odysseia plugin;
    private volatile long activeUntilMillis;

    public MaintenanceGuardListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    public void begin(long seconds) {
        long bounded = Math.max(10L, Math.min(seconds, 3600L));
        activeUntilMillis = System.currentTimeMillis() + bounded * 1000L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("odysseia.maintenance.bypass")
                    && player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING
                    && player.getOpenInventory().getTopInventory().getType() != InventoryType.PLAYER) {
                player.closeInventory();
            }
        }
        Bukkit.broadcastMessage("§6[DrakesCraft] §eReinicio programado en " + bounded
                + " segundos. Se bloquearon temporalmente máquinas, cofres y menús de procesamiento.");
        plugin.getLogger().info("[Maintenance] Ventana segura iniciada por " + bounded + " segundos.");
    }

    public void cancel() {
        activeUntilMillis = 0L;
        Bukkit.broadcastMessage("§6[DrakesCraft] §aLa ventana de mantenimiento fue cancelada.");
    }

    public boolean isActive() {
        return remainingSeconds() > 0L;
    }

    public long remainingSeconds() {
        return Math.max(0L, (activeUntilMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && shouldBlock(player)
                && event.getInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && shouldBlock(player)
                && event.getView().getTopInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && shouldBlock(player)
                && event.getView().getTopInventory().getType() != InventoryType.PLAYER) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || event.getClickedBlock() == null
                || !shouldBlock(event.getPlayer())) {
            return;
        }
        Material material = event.getClickedBlock().getType();
        String name = material.name().toUpperCase(Locale.ROOT);
        if (RISKY_BLOCKS.contains(material) || name.endsWith("_SHULKER_BOX")) {
            event.setCancelled(true);
            warn(event.getPlayer());
        }
    }

    private boolean shouldBlock(Player player) {
        return isActive() && !player.hasPermission("odysseia.maintenance.bypass");
    }

    private void warn(Player player) {
        player.sendActionBar("§eReinicio en " + remainingSeconds() + "s: guarda tus objetos y espera.");
    }
}
