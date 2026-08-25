package org.metamechanists.odysseia.saori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor de contención y congelamiento de seguridad SAORI.
 * Permite congelar jugadores sospechosos de exploits o ataques durante un tiempo determinado.
 */
public final class SaoriFreezeManager implements Listener, CommandExecutor, TabCompleter {

    public record FreezeRecord(UUID playerUuid, String playerName, Instant expiresAt, String reason, Location originLocation) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public long remainingSeconds() {
            return Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds());
        }
    }

    private final JavaPlugin plugin;
    private final Map<UUID, FreezeRecord> frozenPlayers = new ConcurrentHashMap<>();
    private @Nullable BukkitTask tickerTask;

    public SaoriFreezeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startTicker();
    }

    public void freeze(@NotNull Player player, @NotNull Duration duration, @NotNull String reason) {
        Instant expires = Instant.now().plus(duration);
        FreezeRecord record = new FreezeRecord(player.getUniqueId(), player.getName(), expires, reason, player.getLocation().clone());
        frozenPlayers.put(player.getUniqueId(), record);

        player.closeInventory();

        // Enviar Title y Alerta
        Component mainTitle = Component.text("CONGELADO POR SAORI", NamedTextColor.DARK_RED, TextDecoration.BOLD);
        Component subTitle = Component.text("Cuarentena de seguridad activa (" + (duration.toMinutes()) + "m)", NamedTextColor.YELLOW);
        player.showTitle(Title.title(mainTitle, subTitle, Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(500))));

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED));
        player.sendMessage(Component.text("⚠ [SAORI] PROTOCOLO DE CUARENTENA ACTIVADO ⚠", NamedTextColor.RED, TextDecoration.BOLD));
        player.sendMessage(Component.text("Se ha detectado un patrón de actividad altamente anómalo.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("Motivo: ", NamedTextColor.YELLOW).append(Component.text(reason, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Tus comandos, inventario y movimientos están bloqueados por " + duration.toMinutes() + " minutos.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("SAORI ha registrado un snapshot forense y notificado a Jack.", NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_RED));

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.6f);
        } catch (Exception ignored) {}

        plugin.getLogger().log(Level.WARNING, "[SAORI-FREEZE] Jugador {0} ({1}) CONGELADO por {2}m. Motivo: {3}",
                new Object[]{player.getName(), player.getUniqueId(), duration.toMinutes(), reason});
    }

    public boolean unfreeze(@NotNull UUID uuid, @Nullable CommandSender operator) {
        FreezeRecord removed = frozenPlayers.remove(uuid);
        if (removed != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("✔ [SAORI] Has sido descongelado. Ya puedes moverte y usar comandos.", NamedTextColor.GREEN));
                try {
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
                } catch (Exception ignored) {}
            }
            plugin.getLogger().log(Level.INFO, "[SAORI-FREEZE] Jugador {0} ha sido descongelado por {1}.",
                    new Object[]{removed.playerName(), operator != null ? operator.getName() : "Sistema"});
            return true;
        }
        return false;
    }

    public boolean isFrozen(@NotNull UUID uuid) {
        FreezeRecord record = frozenPlayers.get(uuid);
        if (record == null) return false;
        if (record.isExpired()) {
            frozenPlayers.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(Component.text("✔ [SAORI] Tu periodo de cuarentena ha finalizado.", NamedTextColor.GREEN));
            }
            return false;
        }
        return true;
    }

    public @Nullable FreezeRecord getFreezeRecord(@NotNull UUID uuid) {
        return frozenPlayers.get(uuid);
    }

    private void startTicker() {
        if (tickerTask != null) tickerTask.cancel();
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, FreezeRecord> entry : frozenPlayers.entrySet()) {
                if (entry.getValue().isExpired()) {
                    frozenPlayers.remove(entry.getKey());
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) {
                        p.sendMessage(Component.text("✔ [SAORI] Tu periodo de cuarentena ha finalizado.", NamedTextColor.GREEN));
                    }
                    continue;
                }
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    long remMins = Math.max(1, entry.getValue().remainingSeconds() / 60);
                    player.sendActionBar(Component.text("⛔ CONGELADO POR SAORI · " + remMins + "m restantes", NamedTextColor.RED, TextDecoration.BOLD));
                }
            }
        }, 40L, 40L);
    }

    public void cleanup() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        frozenPlayers.clear();
    }

    // ── EVENTOS DE BLOQUEO ──

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isFrozen(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("⛔ [SAORI] No puedes ejecutar comandos mientras estás bajo cuarentena de seguridad.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Mantiene el estado en frozenPlayers para que si reloguea siga congelado hasta que expire el tiempo
    }

    // ── COMANDO ADMINISTRATIVO /saorifreeze ──

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /saorifreeze <freeze|unfreeze|status> <jugador> [minutos]", NamedTextColor.YELLOW));
            return true;
        }

        String sub = args[0].toLowerCase();
        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (sub.equals("freeze")) {
            if (target == null || !target.isOnline()) {
                sender.sendMessage(Component.text("Jugador no encontrado o desconectado.", NamedTextColor.RED));
                return true;
            }
            int mins = 10;
            if (args.length >= 3) {
                try {
                    mins = Math.max(1, Integer.parseInt(args[2]));
                } catch (NumberFormatException ignored) {}
            }
            freeze(target, Duration.ofMinutes(mins), "Congelamiento manual por " + sender.getName());
            sender.sendMessage(Component.text("✔ Jugador " + target.getName() + " congelado por " + mins + " minutos.", NamedTextColor.GREEN));
            return true;
        }

        if (sub.equals("unfreeze")) {
            UUID targetUuid = target != null ? target.getUniqueId() : null;
            if (targetUuid == null) {
                for (Map.Entry<UUID, FreezeRecord> e : frozenPlayers.entrySet()) {
                    if (e.getValue().playerName().equalsIgnoreCase(targetName)) {
                        targetUuid = e.getKey();
                        break;
                    }
                }
            }
            if (targetUuid != null && unfreeze(targetUuid, sender)) {
                sender.sendMessage(Component.text("✔ Jugador descongelado exitosamente.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("El jugador no figuraba congelado.", NamedTextColor.RED));
            }
            return true;
        }

        if (sub.equals("status")) {
            UUID targetUuid = target != null ? target.getUniqueId() : null;
            FreezeRecord rec = targetUuid != null ? getFreezeRecord(targetUuid) : null;
            if (rec != null && !rec.isExpired()) {
                sender.sendMessage(Component.text("Estado de " + rec.playerName() + ": CONGELADO. Restante: " + (rec.remainingSeconds() / 60) + "m. Motivo: " + rec.reason(), NamedTextColor.GOLD));
            } else {
                sender.sendMessage(Component.text("El jugador " + targetName + " NO está congelado.", NamedTextColor.GREEN));
            }
            return true;
        }

        sender.sendMessage(Component.text("Subcomando desconocido. Usa: freeze, unfreeze, status.", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("freeze", "unfreeze", "status");
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("freeze")) {
            return List.of("5", "10", "15", "30", "60");
        }
        return Collections.emptyList();
    }
}
