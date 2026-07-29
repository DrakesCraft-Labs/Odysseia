package org.metamechanists.odysseia.boss.arena;

import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.BossManager;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import net.milkbowl.vault.economy.Economy;

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
        Bukkit.getScheduler().runTaskTimer(plugin, this::enforceArenaBounds, 20L, 20L);
    }

    /** Result of a paid arena creation attempt, including a player-safe failure reason. */
    public record StartResult(BossArenaSession session, double feePerPlayer, String error) {
        public boolean started() { return session != null; }
    }

    /** Starts a public, paid arena. */
    public StartResult start(String type, Collection<Player> players, boolean group) {
        return startInternal(type, players, group, true);
    }

    /** Starts a no-cost arena for controlled staff testing. */
    public StartResult startForced(String type, Collection<Player> players) {
        return startInternal(type, players, players.size() > 1, false);
    }

    /** Starts a solo arena paid by a consumed summoner rather than Vault currency. */
    public StartResult startWithSummoner(String type, Player player) {
        return startInternal(type, List.of(player), false, false);
    }

    private StartResult startInternal(String type, Collection<Player> players, boolean group, boolean chargeEntry) {
        if (players.isEmpty()) return failed("No hay jugadores para esta arena.");
        if (!bosses.supportsBossType(type)) return failed("Ese jefe no existe o está desactivado.");
        World world = arenaWorld();
        if (world == null) return failed("No se pudo preparar el mundo de jefes.");
        if (players.stream().anyMatch(player -> byPlayer.containsKey(player.getUniqueId()))) {
            return failed("Un integrante ya está en otra arena.");
        }
        int cell = reserveCell();
        Location center = new Location(world, cell * CELL_SIZE + 0.5D, 65D, 0.5D);
        buildFloor(world, center);
        OdysseyBoss boss;
        try {
            boss = bosses.spawnBoss(type, center.clone().add(0, 1, 0), false);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("[BossArena] Falló la creación de " + type + ": " + exception.getMessage());
            occupiedCells.remove(cell);
            clearFloor(center);
            return failed("La arena falló antes de cobrar la entrada.");
        }
        if (boss == null) {
            occupiedCells.remove(cell);
            clearFloor(center);
            return failed("La creación del jefe fue cancelada antes de cobrar la entrada.");
        }
        EntryCharge charge = chargeEntry ? chargeEntry(type, players) : EntryCharge.free();
        if (!charge.success()) {
            rollbackSpawn(boss, players, cell, center, EntryCharge.free());
            return failed(charge.error());
        }
        try {
            if (group) boss.applyArenaPowerMultiplier(5.0D);
            Set<UUID> ids = new LinkedHashSet<>();
            for (Player player : players) {
                if (!player.teleport(center.clone().add(0, 1, 12))) {
                    throw new IllegalStateException("No se pudo teletransportar a " + player.getName());
                }
                ids.add(player.getUniqueId());
                byPlayer.put(player.getUniqueId(), boss.getEntity().getUniqueId());
                player.setFallDistance(0);
            }
            double challengeMultiplier = targetedHealthMultiplier(ids);
            if (challengeMultiplier > 1.0D) {
                boss.applyArenaHealthMultiplier(challengeMultiplier);
                for (Player player : players) {
                    player.sendMessage("§5[BossArena] §dDesafío de élite activo: §fx"
                            + String.format(java.util.Locale.ROOT, "%.0f", challengeMultiplier)
                            + " §dde vida efectiva.");
                }
            }
            BossArenaSession session = new BossArenaSession(UUID.randomUUID(), boss.getEntity().getUniqueId(), type,
                    center, group, Set.copyOf(ids), System.currentTimeMillis());
            byBoss.put(session.bossId(), session);
            String notice = "§6[BossArena] §e" + players.iterator().next().getName() + " desafía a §c" + boss.getDisplayName()
                    + "§e. Usa §f/bosswarp spectate " + players.iterator().next().getName() + " §epara mirar.";
            for (Player nearby : world.getPlayers()) {
                if (nearby.getLocation().distanceSquared(center) <= 256.0D * 256.0D) nearby.sendMessage(notice);
            }
            return new StartResult(session, charge.feePerPlayer(), "");
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("[BossArena] Rollback de arena " + type + ": " + exception.getMessage());
            rollbackSpawn(boss, players, cell, center, charge);
            return failed("La arena no pudo inicializarse. Tu entrada fue reembolsada.");
        }
    }

    /** Removes every partial arena side effect before an entry can be refunded. */
    private void rollbackSpawn(OdysseyBoss boss, Collection<Player> players, int cell, Location center, EntryCharge charge) {
        byBoss.remove(boss.getEntity().getUniqueId());
        for (Player player : players) byPlayer.remove(player.getUniqueId(), boss.getEntity().getUniqueId());
        bosses.removeBoss(boss.getEntity().getUniqueId(), null);
        occupiedCells.remove(cell);
        clearFloor(center);
        charge.refund();
    }

    /** Price shown to players before they enter, excluding a configured staff bypass. */
    public double entryFee(String type) {
        return BossArenaPricing.feeFor(plugin.getConfig().getConfigurationSection("boss-arena.entry-fees"), type);
    }

    private StartResult failed(String error) {
        return new StartResult(null, 0.0D, error);
    }

    private EntryCharge chargeEntry(String type, Collection<Player> players) {
        double fee = entryFee(type);
        if (fee <= 0.0D) return EntryCharge.free();
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            return EntryCharge.failed("La economía no está disponible; no se cobró ninguna entrada.");
        }
        Economy economy = registration.getProvider();
        String bypass = plugin.getConfig().getString("boss-arena.entry-fees.free-permission", "");
        ArrayList<Player> charged = new ArrayList<>();
        for (Player player : players) {
            if (!bypass.isBlank() && player.hasPermission(bypass)) continue;
            if (!economy.has(player, fee)) {
                return EntryCharge.failed("§c" + player.getName() + " no tiene §e"
                        + formatFee(fee) + " Dragmas§c para entrar.");
            }
        }
        for (Player player : players) {
            if (!bypass.isBlank() && player.hasPermission(bypass)) continue;
            if (!economy.withdrawPlayer(player, fee).transactionSuccess()) {
                refund(economy, charged, fee);
                return EntryCharge.failed("No se pudo cobrar la entrada. Todo cobro previo fue reembolsado.");
            }
            charged.add(player);
            plugin.getLogger().info("[BossArena] Entrada cobrada: " + player.getName() + " -> "
                    + type + " por " + formatFee(fee) + " Dragmas.");
        }
        return new EntryCharge(economy, charged, fee, "");
    }

    private void refund(Economy economy, Collection<Player> players, double fee) {
        for (Player player : players) economy.depositPlayer(player, fee);
    }

    private double targetedHealthMultiplier(Set<UUID> participants) {
        var profiles = plugin.getConfig().getConfigurationSection("boss-arena.targeted-challenges.players");
        if (profiles == null) return 1.0D;
        double multiplier = 1.0D;
        for (UUID participant : participants) {
            multiplier = Math.max(multiplier, profiles.getDouble(participant.toString() + ".health-multiplier", 1.0D));
        }
        return Math.clamp(multiplier, 1.0D, 1_000_000.0D);
    }

    private static String formatFee(double fee) {
        return String.format(java.util.Locale.ROOT, "%,.0f", fee);
    }

    private static final class EntryCharge {
        private final Economy economy;
        private final Collection<Player> charged;
        private final double feePerPlayer;
        private final String error;
        private boolean refunded;

        private EntryCharge(Economy economy, Collection<Player> charged, double feePerPlayer, String error) {
            this.economy = economy;
            this.charged = charged;
            this.feePerPlayer = feePerPlayer;
            this.error = error;
        }

        static EntryCharge free() { return new EntryCharge(null, List.of(), 0.0D, ""); }
        static EntryCharge failed(String error) { return new EntryCharge(null, List.of(), 0.0D, error); }
        boolean success() { return error.isEmpty(); }
        String error() { return error; }
        double feePerPlayer() { return feePerPlayer; }
        void refund() {
            if (refunded || economy == null) return;
            refunded = true;
            for (Player player : charged) economy.depositPlayer(player, feePerPlayer);
        }
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

    /** Keeps flying and pathfinding-heavy bosses inside their assigned arena cell. */
    private void enforceArenaBounds() {
        for (BossArenaSession session : byBoss.values()) {
            var entity = Bukkit.getEntity(session.bossId());
            if (entity == null || !entity.isValid()) {
                continue;
            }
            Location center = session.center();
            Location current = entity.getLocation();
            double dx = current.getX() - center.getX();
            double dz = current.getZ() - center.getZ();
            boolean outside = dx * dx + dz * dz > 42D * 42D
                    || current.getY() < center.getY() - 8D || current.getY() > center.getY() + 58D;
            if (!outside) {
                continue;
            }
            entity.teleport(center.clone().add(0, 2, 0));
            entity.setVelocity(entity.getVelocity().zero());
            plugin.getLogger().info("[BossArena] Contención aplicada a " + session.bossType() + " en arena " + session.id() + ".");
        }
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
