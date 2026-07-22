package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Breaks verified vanilla clocks and suppresses unattended movement/fishing farms. */
public final class AutomationGuardListener implements Listener {
    private static final Set<Material> REDSTONE_COMPONENTS = Set.of(
            Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR, Material.OBSERVER,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH);
    private static final Set<Material> TIMING_COMPONENTS = Set.of(
            Material.REPEATER, Material.COMPARATOR, Material.OBSERVER,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH);
    private static final List<Material> BREAK_PRIORITY = List.of(
            Material.REPEATER, Material.COMPARATOR, Material.OBSERVER,
            Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH, Material.REDSTONE_WIRE);
    private static final BlockFace[] ADJACENT = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };

    private final Odysseia plugin;
    private final Map<BlockKey, PulseWindow> pulseWindows = new HashMap<>();
    private final Map<BlockKey, Long> disabledUntil = new HashMap<>();
    private final Map<UUID, ActivityState> activity = new HashMap<>();
    private final BukkitTask cleanupTask;

    public AutomationGuardListener(Odysseia plugin) {
        this.plugin = plugin;
        this.cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredState,
                20L * 300L, 20L * 300L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.redstone.enabled", true)
                || event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) {
            return;
        }
        try {
            inspectPulse(event);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("[AutomationGuard] No se pudo inspeccionar redstone en "
                    + format(event.getBlock().getLocation()) + ": " + exception.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMovement(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)
                || event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        ActivityState state = activity.computeIfAbsent(player.getUniqueId(), ignored -> ActivityState.activeAt(event.getFrom()));
        if (viewChanged(event.getFrom(), event.getTo())) {
            markActive(player, event.getTo());
            return;
        }
        if (samePosition(event.getFrom(), event.getTo())) return;
        if (player.hasPermission("odysseia.automation.bypass")) return;

        long now = System.currentTimeMillis();
        long inactivityLimit = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.inactivity-seconds", 300L), 60L, 3600L) * 1000L;
        double minimumDisplacement = Math.clamp(plugin.getConfig().getDouble(
                "automation-guard.afk.minimum-displacement", 4.0D), 2.0D, 32.0D);
        double displacedSquared = sameWorld(state.anchor(), event.getTo())
                ? state.anchor().distanceSquared(event.getTo()) : Double.MAX_VALUE;
        if (!AutomationGuardPolicy.shouldBlockAfkMotion(now - state.lastActive(), displacedSquared,
                inactivityLimit, minimumDisplacement)) {
            return;
        }

        event.setCancelled(true);
        player.setVelocity(new Vector());
        Entity vehicle = player.getVehicle();
        if (vehicle != null) vehicle.setVelocity(new Vector());
        notifyBlocked(player, state, now, "Movimiento AFK detenido. Mueve la cámara para continuar.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)
                || event.getPlayer().hasPermission("odysseia.automation.bypass")) {
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        ActivityState state = activity.computeIfAbsent(event.getPlayer().getUniqueId(),
                ignored -> ActivityState.activeAt(event.getPlayer().getLocation()));
        long now = System.currentTimeMillis();
        long inactivityLimit = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.inactivity-seconds", 300L), 60L, 3600L) * 1000L;
        if (now - state.lastActive() < inactivityLimit) return;
        event.setCancelled(true);
        notifyBlocked(event.getPlayer(), state, now,
                "Pesca AFK detenida. El autoclicker está permitido, la ausencia no.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        markActive(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activity.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        markActive(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) markActive(player, player.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        markActive(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        markActive(event.getPlayer(), event.getPlayer().getLocation());
    }

    public void shutdown() {
        cleanupTask.cancel();
        pulseWindows.clear();
        disabledUntil.clear();
        activity.clear();
    }

    private void inspectPulse(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        BlockKey key = BlockKey.of(block);
        long now = System.currentTimeMillis();
        if (disabledUntil.getOrDefault(key, 0L) > now) {
            event.setNewCurrent(0);
            return;
        }

        long longWindow = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.redstone.long-window-seconds", 600L), 60L, 3600L) * 1000L;
        long fastWindow = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.redstone.fast-window-seconds", 10L), 2L, 60L) * 1000L;
        PulseWindow pulses = pulseWindows.computeIfAbsent(key, ignored -> new PulseWindow());
        pulses.record(now, longWindow);

        int fastLimit = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.fast-pulse-limit", 12), 4, 100);
        int longLimit = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.long-pulse-limit", 8), 4, 100);
        int fastPulses = pulses.countSince(now - fastWindow);
        if (fastPulses < fastLimit && pulses.size() < longLimit) return;

        ClockStructure structure = inspectClockStructure(block);
        if (!AutomationGuardPolicy.shouldBreakClock(fastPulses, pulses.size(), structure.isClock(),
                fastLimit, longLimit)) {
            return;
        }

        Block target = structure.breakTarget();
        if (target == null) return;
        event.setNewCurrent(0);
        disabledUntil.put(key, now + 15_000L);
        disabledUntil.put(BlockKey.of(target), now + 15_000L);
        boolean broken = target.breakNaturally();
        if (!broken && target.getType() != Material.AIR) target.setType(Material.AIR, false);
        pulseWindows.remove(key);
        notifyClockBroken(target.getLocation(), fastPulses, pulses.size());
    }

    /** Scans at most 64 adjacent vanilla redstone blocks; custom Slimefun blocks are never traversed. */
    private ClockStructure inspectClockStructure(Block origin) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<BlockKey> visited = new HashSet<>();
        List<Block> components = new ArrayList<>();
        queue.add(origin);
        while (!queue.isEmpty() && visited.size() < 64) {
            Block current = queue.removeFirst();
            BlockKey key = BlockKey.of(current);
            if (!visited.add(key) || !REDSTONE_COMPONENTS.contains(current.getType())) continue;
            components.add(current);
            for (BlockFace face : ADJACENT) queue.addLast(current.getRelative(face));
        }
        long timers = components.stream().filter(block -> TIMING_COMPONENTS.contains(block.getType())).count();
        Block target = components.stream()
                .min(Comparator.comparingInt(block -> priority(block.getType())))
                .orElse(null);
        return new ClockStructure(components.size() >= 3 && timers >= 1, target);
    }

    private int priority(Material material) {
        int index = BREAK_PRIORITY.indexOf(material);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private void notifyClockBroken(Location location, int fastPulses, int longPulses) {
        String coordinates = format(location);
        plugin.getLogger().warning("[AutomationGuard] Reloj de redstone desarmado en " + coordinates
                + " (rápidos=" + fastPulses + ", ventana=" + longPulses + ").");
        Component message = Component.text("Reloj automático desarmado para proteger los TPS.", NamedTextColor.RED);
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= 24.0D * 24.0D) player.sendMessage(message);
        }
    }

    private void notifyBlocked(Player player, ActivityState state, long now, String text) {
        long cooldown = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.notification-cooldown-seconds", 30L), 10L, 300L) * 1000L;
        if (now - state.lastNotice() < cooldown) return;
        state.lastNotice(now);
        player.sendActionBar(Component.text(text, NamedTextColor.YELLOW));
    }

    private void markActive(Player player, Location location) {
        activity.put(player.getUniqueId(), ActivityState.activeAt(location));
    }

    private void cleanupExpiredState() {
        long cutoff = System.currentTimeMillis() - 15L * 60L * 1000L;
        pulseWindows.entrySet().removeIf(entry -> entry.getValue().lastSeen() < cutoff);
        disabledUntil.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
    }

    private boolean samePosition(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ();
    }

    private boolean viewChanged(Location from, Location to) {
        return Math.abs(from.getYaw() - to.getYaw()) >= 3.0F || Math.abs(from.getPitch() - to.getPitch()) >= 3.0F;
    }

    private boolean sameWorld(Location first, Location second) {
        return first.getWorld() == second.getWorld();
    }

    private String format(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + ","
                + location.getBlockY() + "," + location.getBlockZ();
    }

    private record BlockKey(UUID world, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record ClockStructure(boolean isClock, Block breakTarget) {
    }

    private static final class PulseWindow {
        private final ArrayDeque<Long> pulses = new ArrayDeque<>();

        void record(long now, long window) {
            pulses.addLast(now);
            while (!pulses.isEmpty() && pulses.getFirst() < now - window) pulses.removeFirst();
        }

        int countSince(long cutoff) {
            int count = 0;
            for (long pulse : pulses) if (pulse >= cutoff) count++;
            return count;
        }

        int size() {
            return pulses.size();
        }

        long lastSeen() {
            return pulses.isEmpty() ? 0L : pulses.getLast();
        }
    }

    private static final class ActivityState {
        private final long lastActive;
        private final Location anchor;
        private long lastNotice;

        private ActivityState(long lastActive, Location anchor) {
            this.lastActive = lastActive;
            this.anchor = anchor;
        }

        static ActivityState activeAt(Location location) {
            return new ActivityState(System.currentTimeMillis(), location.clone());
        }

        long lastActive() {
            return lastActive;
        }

        Location anchor() {
            return anchor;
        }

        long lastNotice() {
            return lastNotice;
        }

        void lastNotice(long value) {
            lastNotice = value;
        }
    }
}
