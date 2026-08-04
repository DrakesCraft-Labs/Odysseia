package org.metamechanists.odysseia.chatgames;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Compact, calendar-rotated chat games with a guaranteed revealed answer after an unanswered timeout. */
public final class SeasonalChatGamesService implements Listener {
    private final JavaPlugin plugin;
    private final SeasonalChallengeFactory challenges = new SeasonalChallengeFactory();
    private final AtomicReference<ActiveChallenge> active = new AtomicReference<>();
    private volatile long nextGameAt;

    public SeasonalChatGamesService(JavaPlugin plugin) { this.plugin = plugin; }

    public void resetSchedule() {
        active.set(null);
        nextGameAt = System.currentTimeMillis() + intervalMillis();
        if (plugin.getConfig().getBoolean("chatgames.enabled", true)) {
            plugin.getLogger().info("[ChatGames] Motor semanal activo; proximo juego en " + (intervalMillis() / 60000L) + " minutos aprox.");
        }
    }

    public void tick() {
        if (!plugin.getConfig().getBoolean("chatgames.enabled", true)) return;
        long now = System.currentTimeMillis();
        ActiveChallenge current = active.get();
        if (current != null && now >= current.deadline()) {
            if (active.compareAndSet(current, null)) timeout(current.challenge());
            return;
        }
        if (current != null || now < nextGameAt) return;
        if (Bukkit.getOnlinePlayers().size() < Math.max(1, plugin.getConfig().getInt("chatgames.minimum-players", 2))) {
            nextGameAt = now + 60000L;
            return;
        }
        start(now);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        ActiveChallenge current = active.get();
        if (current == null || System.currentTimeMillis() >= current.deadline() || !current.challenge().matches(event.getMessage())) return;
        if (!active.compareAndSet(current, null)) return;
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, () -> win(event.getPlayer(), current.challenge()));
    }

    private void start(long now) {
        ZoneId zone = configuredZone();
        SeasonalGameMode mode = SeasonalGameMode.forDate(LocalDate.now(zone));
        ChatGameChallenge challenge = challenges.create(mode);
        long timeoutSeconds = Math.max(10L, plugin.getConfig().getLong("chatgames.timeout-seconds", 35L));
        active.set(new ActiveChallenge(challenge, now + timeoutSeconds * 1000L));
        broadcast("&8[&6Juego&8] &eSemana " + (((LocalDate.now(zone).getDayOfMonth() - 1) / 7) + 1) + " - &f" + mode.displayName() + "&8: &f" + challenge.prompt());
        broadcast("&8[&6Juego&8] &7Responde en &e" + timeoutSeconds + "s&7. Premio: &aXP &7+ &6Dragmas&7.");
    }

    private void win(Player player, ChatGameChallenge challenge) {
        for (String command : plugin.getConfig().getStringList("chatgames.reward-commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
        }
        broadcast("&8[&6Juego&8] &a" + player.getName() + " resolvio &f" + challenge.mode().displayName() + "&a. Premio entregado.");
        scheduleNext();
    }

    private void timeout(ChatGameChallenge challenge) {
        broadcast("&8[&6Juego&8] &cSin respuesta. &7Solucion: &e" + challenge.answer() + "&7. " + challenge.explanation());
        scheduleNext();
    }

    private void scheduleNext() { nextGameAt = System.currentTimeMillis() + intervalMillis(); }
    private long intervalMillis() {
        int min = Math.max(1, plugin.getConfig().getInt("chatgames.min-interval-minutes", 12));
        int max = Math.max(min, plugin.getConfig().getInt("chatgames.max-interval-minutes", 18));
        return ThreadLocalRandom.current().nextLong(min, (long) max + 1L) * 60000L;
    }
    private ZoneId configuredZone() {
        try { return ZoneId.of(plugin.getConfig().getString("chatgames.timezone", "America/Santiago")); }
        catch (Exception ignored) { return ZoneId.of("America/Santiago"); }
    }
    private void broadcast(String message) { Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message)); }

    private record ActiveChallenge(ChatGameChallenge challenge, long deadline) { }
}
