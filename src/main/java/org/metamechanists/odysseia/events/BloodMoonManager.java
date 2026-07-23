package org.metamechanists.odysseia.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.Odysseia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bounded Blood Moon event. Only its tagged zombies can damage terrain and the
 * protection lookup fails closed, so a missing claim integration never causes grief.
 */
public final class BloodMoonManager implements Listener {

    private final Odysseia plugin;
    private final NamespacedKey hordeZombieKey;
    private final Map<UUID, MoonState> activeMoons = new HashMap<>();
    private final Set<Long> evaluatedNights = new HashSet<>();
    private final Method regionFromLocation;
    private final Method slimefunGetById;
    private final Method slimefunGetItem;
    private BukkitTask monitorTask;
    private BukkitTask hordeTask;
    private BukkitTask blockTask;
    private boolean protectionWarningLogged;
    private boolean slimefunWarningLogged;

    public BloodMoonManager(Odysseia plugin) {
        this.plugin = plugin;
        this.hordeZombieKey = new NamespacedKey(plugin, "blood_moon_horde");
        this.regionFromLocation = findProtectionLookup();
        Method getById = null;
        Method getItem = null;
        try {
            Class<?> type = Class.forName("com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem");
            getById = type.getMethod("getById", String.class);
            getItem = type.getMethod("getItem");
        } catch (ReflectiveOperationException ignored) {
            // Slimefun is optional. Loot simply remains disabled if it is absent.
        }
        this.slimefunGetById = getById;
        this.slimefunGetItem = getItem;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("blood-moon.enabled", true)) {
            return;
        }
        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkForNewMoon, 100L, 100L);
        long waveInterval = Math.max(30L, plugin.getConfig().getLong("blood-moon.wave-interval-seconds", 45L)) * 20L;
        hordeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::advanceHordes, waveInterval, waveInterval);
        blockTask = Bukkit.getScheduler().runTaskTimer(plugin, this::breakSoftBlocks, 40L, 40L);
    }

    public boolean forceStart(World world) {
        if (world == null || activeMoons.containsKey(world.getUID())) {
            return false;
        }
        beginMoon(world);
        return true;
    }

    public boolean stop(World world) {
        MoonState state = activeMoons.remove(world.getUID());
        if (state == null) {
            return false;
        }
        removeHorde(state);
        state.world().setStorm(state.wasStorming());
        state.world().setThundering(state.wasThundering());
        announce(world, "&8&l[LUNA DE SANGRE] &7La horda se ha retirado con el amanecer.");
        return true;
    }

    public boolean isActive(World world) {
        return world != null && activeMoons.containsKey(world.getUID());
    }

    public void shutdown() {
        if (monitorTask != null) monitorTask.cancel();
        if (hordeTask != null) hordeTask.cancel();
        if (blockTask != null) blockTask.cancel();
        activeMoons.values().forEach(this::removeHorde);
        activeMoons.clear();
    }

    private Method findProtectionLookup() {
        try {
            return Class.forName("dev.espi.protectionstones.PSRegion").getMethod("fromLocation", Location.class);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private void checkForNewMoon() {
        for (World world : Bukkit.getWorlds()) {
            if (!isAllowedWorld(world)) continue;
            if (activeMoons.containsKey(world.getUID())) {
                if (world.getTime() >= 23000L || world.getTime() < 12000L) {
                    stop(world);
                }
                continue;
            }
            if (world.getTime() < 13000L || world.getTime() > 13500L) continue;
            long nightKey = world.getFullTime() / 24000L;
            if (!evaluatedNights.add((world.getUID().getMostSignificantBits() ^ nightKey))) continue;
            if (ThreadLocalRandom.current().nextDouble() <= chance("blood-moon.chance", 0.18D)) {
                beginMoon(world);
            }
        }
        // Keeps the event memory bounded even on long-running worlds.
        if (evaluatedNights.size() > 128) evaluatedNights.clear();
    }

    private void beginMoon(World world) {
        activeMoons.put(world.getUID(), new MoonState(world));
        world.setStorm(true);
        world.setThundering(true);
        announce(world, "&4&l[LUNA DE SANGRE] &cLa noche se tiñe de rojo. Resistan las hordas.");
    }

    private void advanceHordes() {
        for (MoonState state : new ArrayList<>(activeMoons.values())) {
            World world = state.world();
            if (world.getTime() >= 23000L || world.getTime() < 12000L) {
                stop(world);
                continue;
            }
            if (state.wave() >= maxWaves()) continue;
            spawnWave(state);
        }
    }

    private void spawnWave(MoonState state) {
        state.incrementWave();
        int mobs = Math.max(1, plugin.getConfig().getInt("blood-moon.zombies-per-player", 5));
        int maxPerWave = Math.max(1, plugin.getConfig().getInt("blood-moon.max-zombies-per-wave", 18));
        int spawned = 0;
        for (Player player : state.world().getPlayers()) {
            if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            for (int i = 0; i < mobs && spawned < maxPerWave; i++) {
                Location location = findSpawn(player);
                if (location == null || isProtected(location)) continue;
                Zombie zombie = (Zombie) state.world().spawnEntity(location, EntityType.ZOMBIE);
                zombie.getPersistentDataContainer().set(hordeZombieKey, PersistentDataType.BYTE, (byte) 1);
                zombie.setCanPickupItems(false);
                zombie.setRemoveWhenFarAway(true);
                zombie.setTarget(player);
                zombie.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED)
                        .setBaseValue(Math.min(0.36D, zombie.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED).getBaseValue() * 1.18D));
                state.zombies().add(zombie.getUniqueId());
                spawned++;
            }
        }
        announce(state.world(), "&4&l[LUNA DE SANGRE] &cOleada " + state.wave() + "/" + maxWaves() + " &7(" + spawned + " zombis)");
        if (state.wave() == maxWaves() && plugin.getConfig().getBoolean("blood-moon.final-boss.enabled", true)) {
            spawnFinalBoss(state);
        }
    }

    private void spawnFinalBoss(MoonState state) {
        List<Player> players = state.world().getPlayers().stream()
                .filter(player -> !player.isDead() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR).toList();
        if (players.isEmpty()) return;
        Player anchor = players.get(ThreadLocalRandom.current().nextInt(players.size()));
        Location location = findSpawn(anchor);
        if (location == null || isProtected(location)) return;
        String id = plugin.getConfig().getString("blood-moon.final-boss.id", "dios_corrupto");
        if (plugin.getBossManager().spawnBoss(id, location) != null) {
            announce(state.world(), "&4&l[LUNA DE SANGRE] &cLa última horda ha traído al heraldo " + id + ".");
        }
    }

    private Location findSpawn(Player player) {
        World world = player.getWorld();
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0D);
            double distance = ThreadLocalRandom.current().nextDouble(20.0D, 34.0D);
            int x = player.getLocation().getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getLocation().getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (location.getBlock().isPassable() && location.clone().add(0, 1, 0).getBlock().isPassable()) return location;
        }
        return null;
    }

    private void breakSoftBlocks() {
        if (!plugin.getConfig().getBoolean("blood-moon.zombie-block-breaking.enabled", true)) return;
        for (MoonState state : activeMoons.values()) {
            state.zombies().removeIf(uuid -> {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                return !(entity instanceof Zombie zombie) || zombie.isDead() || !zombie.isValid();
            });
            for (UUID uuid : state.zombies()) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
                if (!(entity instanceof Zombie zombie) || zombie.getTarget() == null) continue;
                Block block = zombie.getLocation().getBlock();
                if (block.isPassable()) block = block.getRelative(zombie.getFacing());
                if (!canBreak(block)) continue;
                block.getWorld().spawnParticle(org.bukkit.Particle.BLOCK, block.getLocation().add(0.5D, 0.5D, 0.5D),
                        14, 0.2D, 0.2D, 0.2D, block.getBlockData());
                block.setType(Material.AIR, false);
            }
        }
    }

    private boolean canBreak(Block block) {
        if (block.getType().isAir() || !allowedBreakMaterials().contains(block.getType())) return false;
        return !isProtected(block.getLocation());
    }

    private Set<Material> allowedBreakMaterials() {
        Set<Material> result = new HashSet<>();
        for (String entry : plugin.getConfig().getStringList("blood-moon.zombie-block-breaking.allowed-materials")) {
            Material material = Material.matchMaterial(entry);
            if (material != null) result.add(material);
        }
        return result;
    }

    private boolean isProtected(Location location) {
        if (regionFromLocation == null) {
            if (!protectionWarningLogged) {
                protectionWarningLogged = true;
                plugin.getLogger().warning("[BloodMoon] ProtectionStones no disponible: se bloquea la rotura de bloques por seguridad.");
            }
            return true;
        }
        try {
            return regionFromLocation.invoke(null, location) != null;
        } catch (ReflectiveOperationException exception) {
            if (!protectionWarningLogged) {
                protectionWarningLogged = true;
                plugin.getLogger().warning("[BloodMoon] No se pudo consultar ProtectionStones: se bloquea la rotura de bloques.");
            }
            return true;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHordeDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)
                || !zombie.getPersistentDataContainer().has(hordeZombieKey, PersistentDataType.BYTE)) return;
        if (ThreadLocalRandom.current().nextDouble() > chance("blood-moon.slimefun-loot.chance", 0.10D)) return;
        ItemStack loot = createSlimefunLoot();
        if (loot != null) event.getDrops().add(loot);
    }

    private ItemStack createSlimefunLoot() {
        if (slimefunGetById == null || slimefunGetItem == null) return null;
        List<String> ids = plugin.getConfig().getStringList("blood-moon.slimefun-loot.item-ids");
        if (ids.isEmpty()) return null;
        String id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
        try {
            Object slimefunItem = slimefunGetById.invoke(null, id);
            if (slimefunItem == null) return null;
            ItemStack item = (ItemStack) slimefunGetItem.invoke(slimefunItem);
            return item == null ? null : item.clone();
        } catch (ReflectiveOperationException exception) {
            if (!slimefunWarningLogged) {
                slimefunWarningLogged = true;
                plugin.getLogger().warning("[BloodMoon] No se pudo generar loot Slimefun: " + exception.getMessage());
            }
            return null;
        }
    }

    private boolean isAllowedWorld(World world) {
        List<String> worlds = plugin.getConfig().getStringList("blood-moon.worlds");
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    private int maxWaves() {
        return Math.max(1, plugin.getConfig().getInt("blood-moon.waves", 3));
    }

    private double chance(String path, double fallback) {
        return Math.clamp(plugin.getConfig().getDouble(path, fallback), 0.0D, 1.0D);
    }

    private void removeHorde(MoonState state) {
        state.zombies().forEach(uuid -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) entity.remove();
        });
    }

    private void announce(World world, String message) {
        String formatted = ChatColor.translateAlternateColorCodes('&', message);
        for (Player player : world.getPlayers()) player.sendMessage(formatted);
    }

    private static final class MoonState {
        private final World world;
        private final Set<UUID> zombies = new HashSet<>();
        private final boolean wasStorming;
        private final boolean wasThundering;
        private int wave;

        private MoonState(World world) {
            this.world = world;
            this.wasStorming = world.hasStorm();
            this.wasThundering = world.isThundering();
        }

        private World world() {
            return world;
        }

        private Set<UUID> zombies() {
            return zombies;
        }

        private int wave() {
            return wave;
        }

        private boolean wasStorming() {
            return wasStorming;
        }

        private boolean wasThundering() {
            return wasThundering;
        }

        private void incrementWave() {
            wave++;
        }
    }
}
