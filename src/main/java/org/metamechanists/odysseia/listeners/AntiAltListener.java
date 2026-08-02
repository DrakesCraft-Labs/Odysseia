package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.metamechanists.odysseia.Odysseia;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Enforces a configurable concurrent-account limit while allowing explicit shared-IP exemptions. */
public final class AntiAltListener implements Listener {
    private static final long PENDING_TTL_MILLIS = 20_000L;
    private final Odysseia plugin;
    private final Map<UUID, PendingLogin> pending = new HashMap<>();

    public AntiAltListener(Odysseia plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST)
    public synchronized void onLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("anti-alt.enabled", true)) return;
        InetAddress address = event.getAddress();
        if (address == null) return;
        String ip = address.getHostAddress();
        if (new HashSet<>(plugin.getConfig().getStringList("anti-alt.exempt-addresses")).contains(ip)) return;
        expirePending(System.currentTimeMillis());
        int maximum = Math.clamp(plugin.getConfig().getInt("anti-alt.max-concurrent-per-ip", 2), 1, 5);
        int active = (int) Bukkit.getOnlinePlayers().stream().filter(player -> player.getAddress() != null
                && player.getAddress().getAddress() != null && ip.equals(player.getAddress().getAddress().getHostAddress())).count();
        int waiting = (int) pending.entrySet().stream().filter(entry -> !entry.getKey().equals(event.getPlayer().getUniqueId()))
                .filter(entry -> entry.getValue().address().equals(ip)).count();
        if (AntiAltPolicy.shouldReject(active + waiting, maximum)) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, Component.text("Máximo " + maximum + " cuentas activas por red. Si comparten conexión, contacta al staff."));
            plugin.getLogger().warning("[AntiAlt] Conexión rechazada: " + event.getPlayer().getName() + " desde " + ip);
            return;
        }
        pending.put(event.getPlayer().getUniqueId(), new PendingLogin(ip, System.currentTimeMillis()));
    }

    @EventHandler public synchronized void onJoin(PlayerJoinEvent event) { pending.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public synchronized void onQuit(PlayerQuitEvent event) { pending.remove(event.getPlayer().getUniqueId()); }

    private void expirePending(long now) { pending.entrySet().removeIf(entry -> now - entry.getValue().createdAt() > PENDING_TTL_MILLIS); }
    private record PendingLogin(String address, long createdAt) { }
}
