package org.metamechanists.odysseia.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Invulnerabilidad administrativa independiente de /god de Essentials.
 *
 * Se controla en el propio evento de daño y también con la bandera nativa de Bukkit, de modo que
 * los bosses y plugins que respetan la API no puedan atravesarlo. No persiste entre reinicios para
 * evitar que una cuenta de staff quede accidentalmente inmortal tras mantenimiento.
 */
public final class UltraGodCommand implements CommandExecutor, TabCompleter, Listener {

    private static final String USE_PERMISSION = "odysseia.ultragod";
    private static final String OTHERS_PERMISSION = "odysseia.ultragod.others";
    private final Set<UUID> protectedPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(USE_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar /ultragod.");
            return true;
        }

        Player target = sender instanceof Player player ? player : null;
        Boolean requestedState = null;
        if (args.length > 0) {
            String first = args[0].toLowerCase(Locale.ROOT);
            if (first.equals("on") || first.equals("activar")) requestedState = true;
            else if (first.equals("off") || first.equals("desactivar")) requestedState = false;
            else if (!first.equals("toggle")) target = Bukkit.getPlayerExact(args[0]);
        }
        if (args.length > 1) {
            if (!sender.hasPermission(OTHERS_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "No puedes cambiar el estado de otros jugadores.");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
        } else if (target != null && target != sender && !sender.hasPermission(OTHERS_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No puedes cambiar el estado de otros jugadores.");
            return true;
        }
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Uso: /ultragod [on|off|toggle] [jugador]");
            return true;
        }

        boolean enabled = requestedState != null ? requestedState : !isUltraGod(target);
        setUltraGod(target, enabled);
        String state = enabled ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO";
        target.sendMessage(ChatColor.DARK_AQUA + "[UltraGod] " + state
                + ChatColor.GRAY + " - protección administrativa total.");
        if (!sender.equals(target)) sender.sendMessage(ChatColor.AQUA + "UltraGod de " + target.getName() + ": " + state);
        return true;
    }

    /** Cancels all Bukkit damage before it can be escalated by boss abilities or other listeners. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isUltraGod(player)) {
            event.setCancelled(true);
            event.setDamage(0.0D);
        }
    }

    public boolean isUltraGod(Player player) {
        return protectedPlayers.contains(player.getUniqueId());
    }

    public void setUltraGod(Player player, boolean enabled) {
        if (enabled) {
            protectedPlayers.add(player.getUniqueId());
            player.setInvulnerable(true);
            player.setFallDistance(0.0F);
        } else if (protectedPlayers.remove(player.getUniqueId())) {
            // Solo retiramos la bandera que este modo encendió, nunca manipulamos /god de Essentials.
            player.setInvulnerable(false);
        }
    }

    /** Releases Bukkit's flag when Odysseia is disabled or reloaded. */
    public void disableAll() {
        for (UUID uuid : protectedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(false);
        }
        protectedPlayers.clear();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(USE_PERMISSION)) return List.of();
        if (args.length == 1) {
            List<String> values = new ArrayList<>(List.of("on", "off", "toggle"));
            if (sender.hasPermission(OTHERS_PERMISSION)) Bukkit.getOnlinePlayers().forEach(player -> values.add(player.getName()));
            return values.stream().filter(value -> value.toLowerCase(Locale.ROOT)
                    .startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && sender.hasPermission(OTHERS_PERMISSION)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT)
                            .startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
