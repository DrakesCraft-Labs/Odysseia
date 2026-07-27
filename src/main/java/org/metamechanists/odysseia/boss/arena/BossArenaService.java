package org.metamechanists.odysseia.boss.arena;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.BossManager;
import org.metamechanists.odysseia.boss.OdysseyBoss;

/** Owns isolated, non-destructive boss fights in reserved cells of boss_arena. */
public final class BossArenaService implements Listener {
    private static final int CELL_SIZE = 256;
    private final Odysseia plugin;
    private final BossManager bosses;
    private final Map<UUID, BossArenaSession> byBoss = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> byPlayer = new ConcurrentHashMap<>();
    private final Set<Integer> occupiedCells = ConcurrentHashMap.newKeySet();

    public BossArenaService(Odysseia plugin, BossManager bosses) {
        this.plugin = plugin;
        this.bosses = bosses;
    }

    public BossArenaSession start(String type, Collection<Player> players, boolean group) {
        if (players.isEmpty()) return null;
        World world = arenaWorld();
        if (world == null || players.stream().anyMatch(player -> byPlayer.containsKey(player.getUniqueId()))) return null;
        int cell = reserveCell();
        Location center = new Location(world, cell * CELL_SIZE + 0.5D, 65D, 0.5D);
        buildFloor(world, center);
        OdysseyBoss boss = bosses.spawnBoss(type, center.clone().add(0, 1, 0), false);
        if (boss == null) { occupiedCells.remove(cell); return null; }
        if (group) boss.applyArenaPowerMultiplier(5.0D);
        Set<UUID> ids = new LinkedHashSet<>();
        for (Player player : players) {
            ids.add(player.getUniqueId());
            byPlayer.put(player.getUniqueId(), boss.getEntity().getUniqueId());
            player.teleport(center.clone().add(0, 1, 12));
            player.setFallDistance(0);
        }
        BossArenaSession session = new BossArenaSession(UUID.randomUUID(), boss.getEntity().getUniqueId(), type,
                center, group, Set.copyOf(ids), System.currentTimeMillis());
        byBoss.put(session.bossId(), session);
        String notice = "§6[BossArena] §e" + players.iterator().next().getName() + " desafía a §c" + boss.getDisplayName()
                + "§e. Usa §f/bosswarp spectate " + players.iterator().next().getName() + " §epara mirar.";
        for (Player nearby : world.getPlayers()) {
            if (nearby.getLocation().distanceSquared(center) <= 256.0D * 256.0D) nearby.sendMessage(notice);
        }
        return session;
    }

    public boolean spectate(Player viewer, Player participant) {
        UUID boss = byPlayer.get(participant.getUniqueId());
        BossArenaSession session = boss == null ? null : byBoss.get(boss);
        if (session == null) return false;
        viewer.setGameMode(GameMode.SPECTATOR);
        viewer.teleport(session.center().clone().add(0, 18, 0));
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!byPlayer.containsKey(event.getEntity().getUniqueId())) return;
        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID boss = byPlayer.get(event.getPlayer().getUniqueId());
        BossArenaSession session = boss == null ? null : byBoss.get(boss);
        if (session != null) event.setRespawnLocation(session.center().clone().add(0, 1, 12));
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        BossArenaSession session = byBoss.remove(event.getEntity().getUniqueId());
        if (session == null) return;
        event.getDrops().clear();
        for (UUID playerId : session.participants()) {
            byPlayer.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) reward(player, session.group());
        }
        occupiedCells.remove((int) Math.floor(session.center().getX() / CELL_SIZE));
        Bukkit.getScheduler().runTaskLater(plugin, () -> clearFloor(session.center()), 20L * 15L);
    }

    private World arenaWorld() {
        World world = Bukkit.getWorld("boss_arena");
        if (world != null) return world;
        return Bukkit.createWorld(new WorldCreator("boss_arena").type(WorldType.FLAT).generateStructures(false));
    }
    private int reserveCell() { for (int i = 0; ; i++) if (occupiedCells.add(i)) return i; }
    private void buildFloor(World world, Location center) {
        int y = center.getBlockY() - 1;
        for (int x = -48; x <= 48; x++) for (int z = -48; z <= 48; z++)
            world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z).setType(Material.DEEPSLATE_TILES, false);
    }
    private void clearFloor(Location center) {
        World world = center.getWorld();
        int y = center.getBlockY() - 1;
        for (int x = -48; x <= 48; x++) for (int z = -48; z <= 48; z++)
            world.getBlockAt(center.getBlockX() + x, y, center.getBlockZ() + z).setType(Material.AIR, false);
    }

    /** Rewards never use world drops, avoiding grave and arena duplication paths. */
    private void reward(Player player, boolean group) {
        int xp = group ? 450 : 250;
        int emeralds = group ? 16 : 8;
        player.giveExp(xp);
        player.getInventory().addItem(new ItemStack(Material.EMERALD, emeralds));
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 50) player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, group ? 3 : 1));
        if (roll < 20) player.getInventory().addItem(new ItemStack(Material.DIAMOND, group ? 4 : 2));
        player.sendMessage("§a[BossArena] Victoria: §e" + xp + " XP §ay recompensas directas recibidas.");
    }
}
