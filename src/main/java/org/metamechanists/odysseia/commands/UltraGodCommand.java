package org.metamechanists.odysseia.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
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
    private static final String TITAN_PERMISSION = "odysseia.ultragod.titan";
    private final JavaPlugin plugin;
    private final NamespacedKey titanCooldownKey;
    private final Set<UUID> protectedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> titanProtectedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, BukkitTask> titanExpiryTasks = new ConcurrentHashMap<>();

    public UltraGodCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.titanCooldownKey = new NamespacedKey(plugin, "titan_ultragod_cooldown");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(USE_PERMISSION) && sender.hasPermission(TITAN_PERMISSION)) {
            return handleTitanCommand(sender, args);
        }
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

    /** Activates the paid Titan variant without exposing staff targeting or permanent immunity. */
    private boolean handleTitanCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "El UltraGod Titán solo puede usarse dentro del juego.");
            return true;
        }
        if (args.length > 1 || (args.length == 1 && !Set.of("on", "off", "toggle", "activar", "desactivar")
                .contains(args[0].toLowerCase(Locale.ROOT)))) {
            sender.sendMessage(ChatColor.RED + "Uso: /ultragod [on|off|toggle]");
            return true;
        }

        String action = args.length == 0 ? "toggle" : args[0].toLowerCase(Locale.ROOT);
        boolean enable = action.equals("on") || action.equals("activar")
                || (action.equals("toggle") && !isUltraGod(player));
        if (!enable) {
            disableTitanProtection(player, true);
            return true;
        }
        if (isUltraGod(player)) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[Titán Caos] Tu singularidad ya está activa.");
            return true;
        }

        long now = System.currentTimeMillis();
        long availableAt = player.getPersistentDataContainer()
                .getOrDefault(titanCooldownKey, PersistentDataType.LONG, 0L);
        if (availableAt > now) {
            long seconds = Math.max(1L, (availableAt - now + 999L) / 1000L);
            player.sendMessage(ChatColor.RED + "[Titán Caos] La singularidad se recompone en "
                    + formatDuration(seconds) + ".");
            return true;
        }

        int durationSeconds = Math.max(5, plugin.getConfig().getInt("ultragod.titan.duration-seconds", 45));
        int cooldownMinutes = Math.max(1, plugin.getConfig().getInt("ultragod.titan.cooldown-minutes", 30));
        player.getPersistentDataContainer().set(
                titanCooldownKey, PersistentDataType.LONG, now + cooldownMinutes * 60_000L);
        titanProtectedPlayers.add(player.getUniqueId());
        setUltraGod(player, true);
        player.sendTitle(ChatColor.DARK_PURPLE + "ORIGEN SIN FORMA",
                ChatColor.LIGHT_PURPLE + "Invulnerabilidad absoluta · " + durationSeconds + "s", 10, 50, 15);
        player.sendMessage(ChatColor.DARK_PURPLE + "[Titán Caos] " + ChatColor.LIGHT_PURPLE
                + "La realidad deja de reconocerte durante " + durationSeconds + " segundos.");

        BukkitTask previous = titanExpiryTasks.remove(player.getUniqueId());
        if (previous != null) previous.cancel();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player online = Bukkit.getPlayer(player.getUniqueId());
            if (online != null) disableTitanProtection(online, false);
            else titanProtectedPlayers.remove(player.getUniqueId());
            titanExpiryTasks.remove(player.getUniqueId());
        }, durationSeconds * 20L);
        titanExpiryTasks.put(player.getUniqueId(), task);
        return true;
    }

    private void disableTitanProtection(Player player, boolean manual) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = titanExpiryTasks.remove(uuid);
        if (task != null) task.cancel();
        if (!titanProtectedPlayers.remove(uuid)) {
            if (manual) player.sendMessage(ChatColor.GRAY + "[Titán Caos] La singularidad ya estaba inactiva.");
            return;
        }
        setUltraGod(player, false);
        player.sendMessage(ChatColor.DARK_PURPLE + "[Titán Caos] " + ChatColor.GRAY
                + "La realidad vuelve a alcanzarte.");
    }

    private String formatDuration(long totalSeconds) {
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }

    /** Cancels all Bukkit damage before it can be escalated by boss abilities or other listeners. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isUltraGod(player)) {
            event.setCancelled(true);
            event.setDamage(0.0D);
        }
    }

    /** Prevents Bukkit's invulnerable flag from surviving a paid player's disconnect. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (titanProtectedPlayers.contains(event.getPlayer().getUniqueId())) {
            disableTitanProtection(event.getPlayer(), false);
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
        titanExpiryTasks.values().forEach(BukkitTask::cancel);
        titanExpiryTasks.clear();
        titanProtectedPlayers.clear();
        for (UUID uuid : protectedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.setInvulnerable(false);
        }
        protectedPlayers.clear();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(USE_PERMISSION) && !sender.hasPermission(TITAN_PERMISSION)) return List.of();
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
