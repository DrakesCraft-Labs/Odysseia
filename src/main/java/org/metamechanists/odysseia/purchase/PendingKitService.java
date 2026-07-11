package org.metamechanists.odysseia.purchase;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/** Cola nativa para rangos y kits comprados mientras el jugador está offline. */
public final class PendingKitService implements Listener, CommandExecutor {
    private final Odysseia plugin;
    private final KitDeliveryService kits;
    private final File file;
    private final YamlConfiguration data;

    public PendingKitService(Odysseia plugin) {
        this.plugin = plugin;
        this.kits = new KitDeliveryService(plugin);
        this.file = new File(plugin.getDataFolder(), "pending-kits.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        importLegacyQueue();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 2) {
            sender.sendMessage("Uso: /odysseiapendingkit <jugador> <kit>");
            return true;
        }
        String nick = args[0];
        String kit = args[1].toLowerCase(Locale.ROOT);
        if (plugin.getConfig().getConfigurationSection("kits." + kit) == null) {
            sender.sendMessage("Kit desconocido: " + kit);
            return true;
        }
        String transaction = "pending-" + UUID.randomUUID();
        data.set("pending." + nick + ".kit", kit);
        data.set("pending." + nick + ".transaction", transaction);
        save();
        Player online = Bukkit.getPlayerExact(nick);
        if (online != null) deliver(online);
        sender.sendMessage("[PendingKits] " + kit + " encolado para " + nick + ".");
        return true;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> deliver(event.getPlayer()), 20L);
    }

    private void deliver(Player player) {
        String root = findPlayerRoot(player);
        if (root == null) return;
        String kit = data.getString(root + ".kit");
        String transaction = data.getString(root + ".transaction", "pending-" + UUID.randomUUID());
        if (kit == null) return;
        ActionResult result = kits.deliver(player, kit, transaction);
        if (result.status() == ActionResult.Status.COMPLETED) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " parent addtemp " + kit + " 30d accumulate");
            data.set(root, null);
            save();
            player.sendMessage("§6[DrakesCraft] §eTu compra fue entregada correctamente.");
        } else {
            plugin.getLogger().warning("[PendingKits] Entrega pendiente para " + player.getName() + ": " + result.detail());
        }
    }

    private String findPlayerRoot(Player player) {
        ConfigurationSection section = data.getConfigurationSection("pending");
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(player.getName())) return "pending." + key;
        }
        return null;
    }

    private void importLegacyQueue() {
        if (file.exists()) return;
        File legacy = new File(plugin.getDataFolder().getParentFile(), "Denizen/pending_kits.yml");
        if (!legacy.isFile()) return;
        YamlConfiguration old = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection pending = old.getConfigurationSection("pending");
        if (pending == null) return;
        for (String nick : pending.getKeys(false)) {
            for (String raw : old.getStringList("pending." + nick)) {
                String marker = "drakes_claim_kit def:" + nick + "|";
                int index = raw.toLowerCase(Locale.ROOT).indexOf(marker.toLowerCase(Locale.ROOT));
                if (index < 0) continue;
                data.set("pending." + nick + ".kit", raw.substring(index + marker.length()).trim().toLowerCase(Locale.ROOT));
                data.set("pending." + nick + ".transaction", "denizen-import-" + UUID.randomUUID());
            }
        }
        save();
        plugin.getLogger().info("[SUCCESS] Cola de kits importada desde Denizen.");
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[ERROR] No se pudo guardar pending-kits.yml", exception);
        }
    }
}
