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
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Filtro persistente de chat que reemplaza el script Denizen.
 *
 * La sancion es escalonada y se anuncia en el chat global: un jugador silenciado sin explicacion
 * lo vive como un fallo del servidor y lo reporta como tal.
 */
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
        if (!plugin.getConfig().getBoolean("chat-filter.enabled", true)
                || player.hasPermission("drakes.chatfilter.bypass")) {
            return;
        }
        String message = normalize(PlainTextComponentSerializer.plainText().serialize(event.message()));
        String found = plugin.getConfig().getStringList("chat-filter.words").stream()
                .filter(word -> matches(message, word))
                .findFirst()
                .orElse(null);
        if (found == null) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> warn(player, found));
    }

    /**
     * Coincidencia por palabra completa, no por subcadena.
     *
     * Con {@code contains} cualquier termino corto de la lista censura palabras legitimas que lo
     * contengan. Las entradas con espacios ("hijo de puta") siguen funcionando porque el limite
     * solo se exige en los extremos.
     */
    static boolean matches(String normalizedMessage, String word) {
        String needle = normalize(word);
        if (needle.isBlank()) return false;
        Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(needle) + "(?![\\p{L}\\p{N}])");
        return pattern.matcher(normalizedMessage).find();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int limit = plugin.getConfig().getInt("chat-filter.warns.ban", 7);
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
            sender.sendMessage(color("&eWarns de &f" + args[1] + "&e: &c" + data.getInt(path, 0) + "/" + limit));
        }
        return true;
    }

    private void warn(Player player, String word) {
        String path = "warns." + player.getName().toLowerCase(Locale.ROOT);
        int total = data.getInt(path, 0) + 1;
        data.set(path, total);
        save();

        var config = plugin.getConfig();
        int muteAt = config.getInt("chat-filter.warns.mute", 3);
        int kickAt = config.getInt("chat-filter.warns.kick", 5);
        int banAt = config.getInt("chat-filter.warns.ban", 7);
        String muteTime = config.getString("chat-filter.duracion.mute", "10m");
        String banTime = config.getString("chat-filter.duracion.ban", "1d");

        player.sendMessage(color("&c[!] &eEse mensaje no se envió: lenguaje inapropiado. "
                + "Warn &c" + total + "&e/" + banAt + "."));

        // El staff sí ve la palabra; en el chat global no se repite, para no difundirla.
        Bukkit.getOnlinePlayers().stream()
                .filter(staff -> staff.hasPermission("drakes.staff"))
                .forEach(staff -> staff.sendMessage(color("&8[&cChatFilter&8] &7" + player.getName()
                        + " usó '&c" + word + "&7'. &c" + total + "/" + banAt)));

        if (total >= banAt) {
            announce(player, "&cbaneado por " + banTime, total, banAt);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tempban " + player.getName() + " " + banTime + " Lenguaje inapropiado reiterado");
            data.set(path, 0);
            save();
        } else if (total >= kickAt) {
            announce(player, "&6expulsado", total, banAt);
            player.kickPlayer("[DrakesCraft] Modera tu vocabulario (" + total + "/" + banAt + " warns).");
        } else if (total >= muteAt) {
            announce(player, "&esilenciado por " + muteTime, total, banAt);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "mute " + player.getName() + " " + muteTime + " Lenguaje inapropiado reiterado");
        }
    }

    /**
     * Explica la sancion en el chat global.
     *
     * Sin esto el jugador aparece mudo de golpe y el resto no sabe por que; es la queja que
     * llego como "me mutearon porque dije X".
     */
    private void announce(Player player, String sanction, int total, int limit) {
        if (!plugin.getConfig().getBoolean("chat-filter.anuncio-global", true)) return;
        String message = color("&6DrakesCraft &8· &f" + player.getName() + " &7fue " + sanction
                + " &7por lenguaje inapropiado. &8(warn " + total + "/" + limit + ")");
        Bukkit.broadcastMessage(message);
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

    /**
     * Quita acentos, pasa a minusculas y deshace el leet mas comun.
     * Sin esto "m1erd4" pasaba el filtro sin tocarlo.
     */
    static String normalize(String value) {
        String plain = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(plain.length());
        for (char current : plain.toCharArray()) {
            builder.append(switch (current) {
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '0' -> 'o';
                case '1' -> 'i';
                case '$', '5' -> 's';
                case '7' -> 't';
                default -> current;
            });
        }
        // "mierdaaa" y "mieeerda" son la misma palabra. Se colapsa toda repeticion porque la
        // lista tambien pasa por aqui, asi que ambos lados quedan en la misma forma.
        return builder.toString().replaceAll("(.)\\1+", "$1");
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
