package org.metamechanists.odysseia.reencarnacion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Ejecuta el borrado integral del progreso del jugador de forma ordenada y segura.
 */
public final class ReencarnacionExecutor {

    private final JavaPlugin plugin;
    private final NamespacedKey prestigeKey;

    public ReencarnacionExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.prestigeKey = new NamespacedKey(plugin, "prestige_level");
    }

    public boolean execute(ReencarnacionSession session) {
        UUID uuid = session.getPlayerUuid();
        String name = session.getPlayerName();

        plugin.getLogger().info("[Reencarnacion] Ejecutando rito para " + name + " (" + uuid + ")");

        // 1. Kick preventivo si esta conectado
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            onlinePlayer.kick(Component.text("§6§l✦ RITO DE REENCARNACIÓN INICIADO ✦\n\n§eTu antiguo ciclo ha concluido. Tu cuerpo y posesiones se desvanecen...\n§7Vuelve a conectarte en unos instantes para renacer."));
        }

        // 2. Regenerar y desproteger todos los claims de ProtectionStones
        try {
            plugin.getLogger().info("[Reencarnacion] Regenerando claims de " + name + " via ProtectionStones...");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ps admin regenplayer " + name + " --delete");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[Reencarnacion] Error ejecutando ps admin regenplayer: " + t.getMessage(), t);
        }

        // 3. Limpiar BentoBox si tiene isla en SkyBlock / OneBlock
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("BentoBox")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bsb admin reset " + name);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ob admin reset " + name);
            }
        } catch (Throwable ignored) {}

        // 4. Limpiar PlayerVaultZ (Survival)
        wipePlayerVaultZ(uuid);

        // 5. Limpiar Bovedas por Modalidad de Odysseia (Islas)
        wipeModalityVaults(uuid);

        // 6. Resetear Economia a 1.000 Dragmas y sBank
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco set " + name + " 1000");
            wipeSBank(uuid);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[Reencarnacion] Error reseteando economia: " + t.getMessage(), t);
        }

        // 7. Resetear Researches de Slimefun
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Slimefun")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "sf researches reset " + name);
            }
        } catch (Throwable ignored) {}

        // 8. Preparar la entrega de la Capsula y el reseteo de inventario para el proximo join
        ReencarnacionManager.getPendingDeliveries().put(uuid, new ArrayList<>(session.getCapsuleItems()));
        ReencarnacionManager.savePendingDeliveries();

        // 9. Anuncio Global de Prestigio
        int prestigeLevel = ReencarnacionManager.getPrestigeLevel(uuid) + 1;
        ReencarnacionManager.setPrestigeLevel(uuid, prestigeLevel);

        Bukkit.broadcast(Component.text("§6§m-----------------------------------------------------"));
        Bukkit.broadcast(Component.text("§e§l         ✦ ¡RITO DE REENCARNACIÓN COMPLETADO! ✦"));
        Bukkit.broadcast(Component.text("§7El jugador §f" + name + " §7ha purificado su alma y ascendido a §6§lReencarnación " + prestigeLevel + "§7!"));
        Bukkit.broadcast(Component.text("§eHa renunciado voluntariamente a todas sus riquezas para forjar una nueva leyenda."));
        Bukkit.broadcast(Component.text("§6§m-----------------------------------------------------"));

        return true;
    }

    private void wipePlayerVaultZ(UUID uuid) {
        File pvDir = new File(plugin.getDataFolder().getParentFile(), "PlayerVaultZ");
        File pvDb = new File(pvDir, "vaults.db");
        if (!pvDb.exists()) return;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + pvDb.getAbsolutePath())) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM vaults WHERE owner_uuid = ?")) {
                ps.setString(1, uuid.toString());
                int deleted = ps.executeUpdate();
                plugin.getLogger().info("[Reencarnacion] Eliminadas " + deleted + " bovedas de PlayerVaultZ para " + uuid);
            }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM vault_pages WHERE vault_id NOT IN (SELECT id FROM vaults)")) {
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Reencarnacion] Error limpiando PlayerVaultZ: " + e.getMessage(), e);
        }
    }

    private void wipeModalityVaults(UUID uuid) {
        File dbFile = new File(plugin.getDataFolder(), "modality-vaults.db");
        if (!dbFile.exists()) return;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM modality_vaults WHERE player_uuid = ?")) {
                ps.setString(1, uuid.toString());
                int deleted = ps.executeUpdate();
                plugin.getLogger().info("[Reencarnacion] Eliminadas " + deleted + " bovedas de modalidad para " + uuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "[Reencarnacion] Error limpiando bovedas de modalidad: " + e.getMessage(), e);
        }
    }

    private void wipeSBank(UUID uuid) {
        File sbankDir = new File(plugin.getDataFolder().getParentFile(), "sBank");
        File sbankDb = new File(sbankDir, "sbank.db");
        if (!sbankDb.exists()) return;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + sbankDb.getAbsolutePath())) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    /**
     * Aplica la limpieza final de inventario y entrega la Caja Sellada del Pasado al conectarse.
     */
    public static void handlePlayerJoin(Player player, List<ItemStack> capsuleItems) {
        // Limpiar inventario y enderchest
        player.getInventory().clear();
        player.getEnderChest().clear();
        player.setLevel(0);
        player.setExp(0);
        player.setTotalExperience(0);

        // Si guardo items en la capsula, empaquetarlos en una Caja Sellada del Pasado
        if (capsuleItems != null && !capsuleItems.isEmpty()) {
            ItemStack box = new ItemStack(Material.PURPLE_SHULKER_BOX);
            ItemMeta meta = box.getItemMeta();
            if (meta instanceof BlockStateMeta bsm && bsm.getBlockState() instanceof ShulkerBox shulker) {
                meta.displayName(Component.text("✦ Caja Sellada del Pasado ✦", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Contiene los recuerdos resguardados", NamedTextColor.GRAY));
                lore.add(Component.text("de tu vida anterior antes de reencarnar.", NamedTextColor.GRAY));
                meta.lore(lore);

                int slot = 0;
                for (ItemStack item : capsuleItems) {
                    if (item != null && item.getType() != Material.AIR && slot < shulker.getInventory().getSize()) {
                        shulker.getInventory().setItem(slot++, item);
                    }
                }
                bsm.setBlockState(shulker);
                box.setItemMeta(bsm);
            }
            player.getInventory().addItem(box);
        }

        // Entregar kit base inicial
        Bukkit.getScheduler().runTaskLater(org.metamechanists.odysseia.Odysseia.getInstance(), () -> {
            if (player.isOnline()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit inicio " + player.getName());
                player.sendMessage(Component.text("§a§l[✦] §7Has renacido. Recibiste tu kit inicial y tu §dCaja Sellada del Pasado§7 con tus recuerdos."));
            }
        }, 20L);
    }
}
