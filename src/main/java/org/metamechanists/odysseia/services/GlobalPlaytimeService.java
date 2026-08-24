package org.metamechanists.odysseia.services;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Persistent wall-clock playtime shared across inventory modalities. */
public final class GlobalPlaytimeService implements Listener, AutoCloseable {

    private static final long SAVE_INTERVAL_TICKS = 20L * 300L;

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Map<UUID, Long> totals = new HashMap<>();
    private final Map<UUID, Long> sessionCheckpoints = new HashMap<>();
    private BukkitTask ticker;
    private BukkitTask saver;
    private boolean dirty;

    public GlobalPlaytimeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "global-playtime.yml");
        load();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            seedFromCurrentStatistic(player);
            sessionCheckpoints.put(player.getUniqueId(), now);
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        saver = Bukkit.getScheduler().runTaskTimer(plugin, this::saveIfDirty,
                SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        seedFromCurrentStatistic(player);
        sessionCheckpoints.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // InvSwitcher applies the destination statistic around this event. One tick later we can
        // use it as a historical lower bound without ever decreasing the global counter.
        Bukkit.getScheduler().runTask(plugin, () -> seedFromCurrentStatistic(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        update(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        sessionCheckpoints.remove(event.getPlayer().getUniqueId());
        saveIfDirty();
    }

    public long millis(UUID playerId) {
        if (sessionCheckpoints.containsKey(playerId)) {
            update(playerId, System.currentTimeMillis());
        }
        return totals.getOrDefault(playerId, 0L);
    }

    public String formatted(UUID playerId) {
        return formatDuration(millis(playerId));
    }

    static String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1_000L);
        long days = seconds / 86_400L;
        long hours = seconds % 86_400L / 3_600L;
        long minutes = seconds % 3_600L / 60L;
        long remainingSeconds = seconds % 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + remainingSeconds + "s";
        return remainingSeconds + "s";
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player.getUniqueId(), now);
        }
    }

    private void update(UUID playerId, long now) {
        Long previous = sessionCheckpoints.put(playerId, now);
        if (previous == null || now <= previous) return;
        totals.merge(playerId, now - previous, Long::sum);
        dirty = true;
    }

    private void seedFromCurrentStatistic(Player player) {
        long statisticMillis = Math.max(0L, player.getStatistic(Statistic.PLAY_ONE_MINUTE)) * 50L;
        long current = totals.getOrDefault(player.getUniqueId(), 0L);
        if (statisticMillis > current) {
            totals.put(player.getUniqueId(), statisticMillis);
            dirty = true;
        }
    }

    private void load() {
        if (!storageFile.isFile()) return;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(storageFile);
        var section = data.getConfigurationSection("players");
        if (section == null) return;
        for (String rawUuid : section.getKeys(false)) {
            try {
                totals.put(UUID.fromString(rawUuid), Math.max(0L, section.getLong(rawUuid)));
            } catch (IllegalArgumentException error) {
                plugin.getLogger().warning("[Playtime] UUID inválido ignorado: " + rawUuid);
            }
        }
    }

    private void saveIfDirty() {
        if (!dirty) return;
        YamlConfiguration data = new YamlConfiguration();
        totals.forEach((uuid, millis) -> data.set("players." + uuid, millis));
        File temporary = new File(storageFile.getParentFile(), storageFile.getName() + ".tmp");
        try {
            data.save(temporary);
            try {
                Files.move(temporary.toPath(), storageFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE, "[Playtime] No se pudo guardar el contador global", error);
        }
    }

    @Override
    public void close() {
        tick();
        if (ticker != null) ticker.cancel();
        if (saver != null) saver.cancel();
        saveIfDirty();
    }
}
