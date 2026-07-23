package org.metamechanists.odysseia.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.logging.Level;

/** Filtro persistente de chat que reemplaza el script Denizen. */
public final class ChatFilterListener implements Listener, CommandExecutor {
    private final Odysseia plugin;
    private final File file;
    private final YamlConfiguration data;

    public ChatFilterListener(Odysseia plugin) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), "chat-warnings.yml");
        data = YamlConfiguration.loadConfiguration(file);
        importLegacyWarnings();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("chat-filter.enabled", true) || player.hasPermission("drakes.chatfilter.bypass")) return;
        String message = normalize(PlainTextComponentSerializer.plainText().serialize(event.message()));
        String found = plugin.getConfig().getStringList("chat-filter.words").stream()
            .filter(word -> message.contains(normalize(word))).findFirst().orElse(null);
        if (found == null) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> warn(player, found));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2 || !(args[0].equalsIgnoreCase("ver") || args[0].equalsIgnoreCase("limpiar"))) {
            sender.sendMessage(color("&eUso: &f/dwarn <ver|limpiar> <jugador>"));
            return true;
        }
        String path = "warns." + args[1].toLowerCase(Locale.ROOT);
        if (args[0].equalsIgnoreCase("limpiar")) {
            data.set(path, 0);
            save();
            sender.sendMessage(color("&aWarns limpiados."));
        } else {
            sender.sendMessage(color("&eWarns de &f" + args[1] + "&e: &c" + data.getInt(path, 0) + "/7"));
        }
        return true;
    }

    private void warn(Player player, String word) {
        String path = "warns." + player.getName().toLowerCase(Locale.ROOT);
        int total = data.getInt(path, 0) + 1;
        data.set(path, total);
        save();
        player.sendMessage(color("&c[!] &eLenguaje inapropiado. Warn &c" + total + "&e/7."));
        Bukkit.getOnlinePlayers().stream().filter(staff -> staff.hasPermission("drakes.staff"))
            .forEach(staff -> staff.sendMessage(color("&8[&cChatFilter&8] &7" + player.getName() + " usó '&c" + word + "&7'. &c" + total + "/7")));
        if (total >= 7) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempban " + player.getName() + " 1d Lenguaje inapropiado reiterado");
            data.set(path, 0);
            save();
        } else if (total >= 5) {
            player.kickPlayer("[DrakesCraft] Modera tu vocabulario (" + total + "/7 warns).");
        } else if (total >= 3) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mute " + player.getName() + " 1h Lenguaje inapropiado reiterado");
        }
    }

    private void importLegacyWarnings() {
        if (file.exists()) return;
        File legacy = new File(plugin.getDataFolder().getParentFile(), "Denizen/chat_warns.yml");
        if (!legacy.isFile()) return;
        YamlConfiguration old = YamlConfiguration.loadConfiguration(legacy);
        if (old.isConfigurationSection("warns")) data.set("warns", old.getConfigurationSection("warns"));
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[ERROR] No se pudo guardar chat-warnings.yml", exception);
        }
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
