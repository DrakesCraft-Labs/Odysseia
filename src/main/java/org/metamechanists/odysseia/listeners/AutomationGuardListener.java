package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Protege TPS frenando relojes de redstone y suprime granjas automatizadas en ausencia.
 * Incluye un sistema inteligente Anti-AFK con cuarentena de reconexión y verificación de actividad
 * para neutralizar bucles de scripts de reconexión y mecanismos de evasión pasivos.
 */
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
    private final Map<BlockKey, ViolationWindow> violationWindows = new HashMap<>();
    private final Map<UUID, ActivityState> activity = new ConcurrentHashMap<>();
    private final Map<UUID, AfkRecord> afkRecords = new ConcurrentHashMap<>();
    private final BukkitTask watchdogTask;

    public AutomationGuardListener(Odysseia plugin) {
        this.plugin = plugin;
        migrateLegacyRedstoneDefaults();
        // El watchdog corre cada 10 segundos (200 ticks) para limpiar expiraciones y vigilar reconexiones ociosas
        this.watchdogTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runPeriodicWatchdog,
                20L * 10L, 20L * 10L);
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
            if (AutomationGuardPolicy.isGenuineLookChange(
                    event.getFrom().getYaw(), event.getTo().getYaw(),
                    event.getFrom().getPitch(), event.getTo().getPitch())) {
                noteAction(player, 1);
            }
            return;
        }
        if (samePosition(event.getFrom(), event.getTo())) return;
        if (hasAfkBypass(player)) return;

        long now = System.currentTimeMillis();
        long inactivityLimit = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.inactivity-seconds", 300L), 60L, 3600L) * 1000L;
        double minimumDisplacement = Math.clamp(plugin.getConfig().getDouble(
                "automation-guard.afk.minimum-displacement", 4.0D), 2.0D, 32.0D);
        double displacedSquared = sameWorld(state.anchor(), event.getTo())
                ? state.anchor().distanceSquared(event.getTo()) : Double.MAX_VALUE;

        long inactiveTime = now - state.lastActive();
        if (player.getVehicle() == null && AutomationGuardPolicy.isMeaningfulMovement(displacedSquared, 2.0D)) {
            noteAction(player, 1);
        }

        if (!AutomationGuardPolicy.shouldBlockAfkMotion(inactiveTime, displacedSquared,
                inactivityLimit, minimumDisplacement)) {
            return;
        }

        // Si lleva un tiempo excesivo atrapado en un mecanismo pasivo (agua, vagoneta, pistón) sin mover cámara
        long evasionLimit = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.evasion-kick-seconds", 900L), 300L, 3600L) * 1000L;
        if (AutomationGuardPolicy.shouldKickEvasion(inactiveTime, evasionLimit)) {
            player.kick(Component.text()
                    .append(Component.text("§6[DrakesCraft · Anti-AFK]\n\n", NamedTextColor.GOLD))
                    .append(Component.text("§cExpulsado por inactividad en mecanismo pasivo de evasión.\n", NamedTextColor.RED))
                    .append(Component.text("§7Por favor interactúa activamente con el juego.\n\n", NamedTextColor.GRAY))
                    .append(Component.text("§8──────────────────────────────────────────────────\n\n", NamedTextColor.DARK_GRAY))
                    .append(Component.text("§cKicked for inactivity in an AFK evasion mechanism.\n", NamedTextColor.RED))
                    .append(Component.text("§7Please interact actively with the game.", NamedTextColor.GRAY))
                    .build());
            recordAfkKick(player.getUniqueId(), player.getName(), "mecanismo pasivo de evasión");
            return;
        }

        event.setCancelled(true);
        player.setVelocity(new Vector());
        Entity vehicle = player.getVehicle();
        if (vehicle != null) vehicle.setVelocity(new Vector());
        notifyBlocked(player, state, now, "Movimiento AFK pausado · Mueve la cámara / AFK motion paused · Move camera");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)
                || hasAfkBypass(event.getPlayer())) {
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
                "Pesca AFK pausada · Ausencia no permitida / AFK fishing paused");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (hasAfkBypass(player)) {
            return;
        }

        String reason = PlainTextComponentSerializer.plainText().serialize(event.reason()).toLowerCase(Locale.ROOT);
        if (reason.contains("inactiv") || reason.contains("afk") || reason.contains("ausencia")
                || reason.contains("echado por estar inactivo")) {
            recordAfkKick(player.getUniqueId(), player.getName(), "inactividad");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.reconnect-quarantine.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        if (hasAfkBypass(player)) {
            return;
        }

        AfkRecord record = afkRecords.get(player.getUniqueId());
        if (record == null) return;

        long now = System.currentTimeMillis();
        if (AutomationGuardPolicy.isQuarantined(now, record.quarantineUntil())) {
            long remainingSec = Math.max(1L, (record.quarantineUntil() - now) / 1000L);
            Component message = Component.text()
                    .append(Component.text("§6[DrakesCraft · Anti-AFK Guard]\n\n", NamedTextColor.GOLD))
                    .append(Component.text("§cDesconectado recientemente por inactividad (AFK).\n", NamedTextColor.RED))
                    .append(Component.text("§7Para mantener el rendimiento y evitar granjas desatendidas,\n", NamedTextColor.GRAY))
                    .append(Component.text("§7debes esperar un momento antes de volver a ingresar.\n", NamedTextColor.GRAY))
                    .append(Component.text("§fReconexión disponible en: §b" + remainingSec + "s §8| §7Aviso #" + record.strikes() + "\n", NamedTextColor.WHITE))
                    .append(Component.text("§ePor favor regresa cuando estés activo para jugar.\n\n", NamedTextColor.YELLOW))
                    .append(Component.text("§8──────────────────────────────────────────────────\n\n", NamedTextColor.DARK_GRAY))
                    .append(Component.text("§cRecently disconnected due to inactivity (AFK).\n", NamedTextColor.RED))
                    .append(Component.text("§7To maintain server stability and prevent unattended farming,\n", NamedTextColor.GRAY))
                    .append(Component.text("§7there is a short cooldown before you can reconnect.\n", NamedTextColor.GRAY))
                    .append(Component.text("§fReconnect available in: §b" + remainingSec + "s §8| §7Strike #" + record.strikes() + "\n", NamedTextColor.WHITE))
                    .append(Component.text("§ePlease return when you are back at your keyboard to play.", NamedTextColor.YELLOW))
                    .build();
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, message);
            plugin.getLogger().info("[Anti-AFK] Reconexión bloqueada para " + player.getName()
                    + " (quedan " + remainingSec + "s de cuarentena, strike " + record.strikes() + ").");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        markActive(player, player.getLocation());

        if (hasAfkBypass(player)) {
            return;
        }

        AfkRecord record = afkRecords.get(player.getUniqueId());
        if (record != null && AutomationGuardPolicy.shouldVerifyJoin(record.strikes())) {
            record.setVerifying(true);
            record.setJoinTime(System.currentTimeMillis());
            record.setActionsObserved(0);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activity.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        noteAction(event.getPlayer(), 2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            noteAction(player, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) noteAction(player, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        noteAction(event.getPlayer(), 1);
    }

    public void shutdown() {
        watchdogTask.cancel();
        pulseWindows.clear();
        disabledUntil.clear();
        violationWindows.clear();
        activity.clear();
        afkRecords.clear();
    }

    private void recordAfkKick(UUID uuid, String name, String detail) {
        long now = System.currentTimeMillis();
        long decayMillis = plugin.getConfig().getLong(
                "automation-guard.afk.reconnect-quarantine.strike-decay-hours", 2L) * 3600L * 1000L;

        AfkRecord record = afkRecords.computeIfAbsent(uuid, ignored -> new AfkRecord());
        if (AutomationGuardPolicy.shouldResetStrikes(now, record.lastKickTime(), decayMillis)) {
            record.setStrikes(0);
        }

        // Debounce: evitar doble registro de strike si se recibe más de un evento de kick en <5s
        if (AutomationGuardPolicy.shouldIgnoreDuplicateKick(now, record.lastKickTime(), 5000L)) {
            return;
        }

        record.setStrikes(record.strikes() + 1);
        record.setLastKickTime(now);
        record.setVerifying(false);

        long s1 = plugin.getConfig().getLong("automation-guard.afk.reconnect-quarantine.strike1-seconds", 0L);
        long s2 = plugin.getConfig().getLong("automation-guard.afk.reconnect-quarantine.strike2-seconds", 180L);
        long s3 = plugin.getConfig().getLong("automation-guard.afk.reconnect-quarantine.strike3-seconds", 600L);
        long quarantineSec = AutomationGuardPolicy.calculateQuarantineSeconds(record.strikes(), s1, s2, s3);
        record.setQuarantineUntil(quarantineSec > 0 ? now + (quarantineSec * 1000L) : 0L);

        plugin.getLogger().warning("[Anti-AFK] " + name + " registrado por inactividad (" + detail
                + ", aviso #" + record.strikes() + "). Cuarentena de reconexión: " + quarantineSec + "s.");
    }

    public static boolean hasAfkBypass(Player player) {
        if (player == null) return false;
        return player.hasPermission("odysseia.automation.bypass")
                || player.hasPermission("odysseia.afk.bypass")
                || player.hasPermission("drakescraft.afk.unlocked")
                || player.hasPermission("essentials.afk.kickexempt");
    }

    private void noteAction(Player player, int count) {
        markActive(player, player.getLocation());
        if (hasAfkBypass(player)) {
            return;
        }
        AfkRecord record = afkRecords.get(player.getUniqueId());
        if (record != null && record.verifying()) {
            record.addAction(count);
            int requiredActions = Math.clamp(plugin.getConfig().getInt(
                    "automation-guard.afk.reconnect-quarantine.required-actions", 3), 1, 10);
            if (record.actionsObserved() >= requiredActions) {
                record.setVerifying(false);
                player.sendActionBar(Component.text("✔ Presencia activa verificada · ¡Buen juego! / Active presence confirmed!", NamedTextColor.GREEN));
            }
        }
    }

    private void runPeriodicWatchdog() {
        cleanupExpiredState();
        checkIdleWatchdog();
    }

    private void checkIdleWatchdog() {
        if (!plugin.getConfig().getBoolean("automation-guard.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.enabled", true)
                || !plugin.getConfig().getBoolean("automation-guard.afk.reconnect-quarantine.enabled", true)) {
            return;
        }

        long now = System.currentTimeMillis();
        long verificationLimit = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.afk.reconnect-quarantine.verification-seconds", 300L), 60L, 1800L) * 1000L;
        int requiredActions = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.afk.reconnect-quarantine.required-actions", 3), 1, 10);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasAfkBypass(player)) {
                continue;
            }
            AfkRecord record = afkRecords.get(player.getUniqueId());
            if (record != null && record.verifying()) {
                if (AutomationGuardPolicy.shouldKickUnverifiedJoin(now, record.joinTime(),
                        record.actionsObserved(), verificationLimit, requiredActions)) {
                    record.setVerifying(false);
                    player.kick(Component.text()
                            .append(Component.text("§6[DrakesCraft · Anti-AFK]\n\n", NamedTextColor.GOLD))
                            .append(Component.text("§cDesconectado por inactividad prolongada tras reconectar.\n", NamedTextColor.RED))
                            .append(Component.text("§7Por favor regresa e interactúa activamente con el juego cuando estés disponible.\n\n", NamedTextColor.GRAY))
                            .append(Component.text("§8──────────────────────────────────────────────────\n\n", NamedTextColor.DARK_GRAY))
                            .append(Component.text("§cDisconnected due to prolonged inactivity after reconnecting.\n", NamedTextColor.RED))
                            .append(Component.text("§7Please return and actively play when you are available.", NamedTextColor.GRAY))
                            .build());
                    recordAfkKick(player.getUniqueId(), player.getName(), "inactividad tras reconexión");
                }
            }
        }
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
                "automation-guard.redstone.long-window-seconds", 120L), 30L, 3600L) * 1000L;
        long fastWindow = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.redstone.fast-window-seconds", 10L), 2L, 60L) * 1000L;
        PulseWindow pulses = pulseWindows.computeIfAbsent(key, ignored -> new PulseWindow());
        pulses.record(now, longWindow);

        int fastLimit = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.fast-pulse-limit", 40), 8, 400);
        int longLimit = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.long-pulse-limit", 180), 20, 4000);
        int fastPulses = pulses.countSince(now - fastWindow);
        if (fastPulses < fastLimit && pulses.size() < longLimit) return;

        ClockStructure structure = inspectClockStructure(block);
        if (!structure.isClock() || (fastPulses < fastLimit && pulses.size() < longLimit)) {
            return;
        }

        Block target = structure.breakTarget();
        if (target == null) return;
        BlockKey violationKey = BlockKey.of(target);
        long violationWindow = Math.clamp(plugin.getConfig().getLong(
                "automation-guard.redstone.violation-window-seconds", 600L), 60L, 3600L) * 1000L;
        int strikesBeforeBreak = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.violations-before-break", 3), 1, 10);
        ViolationWindow violations = violationWindows.computeIfAbsent(violationKey, ignored -> new ViolationWindow());
        int priorViolations = violations.count(now, violationWindow);
        AutomationGuardPolicy.ClockAction action = AutomationGuardPolicy.evaluateClock(
                fastPulses, pulses.size(), structure.isClock(), fastLimit, longLimit,
                priorViolations, strikesBeforeBreak);
        if (action == AutomationGuardPolicy.ClockAction.ALLOW) return;

        int strike = violations.record(now, violationWindow);
        if (action == AutomationGuardPolicy.ClockAction.THROTTLE) {
            long suppressDuration = Math.clamp(plugin.getConfig().getLong(
                    "automation-guard.redstone.suppression-seconds", 5L), 1L, 30L) * 1000L;
            disabledUntil.put(key, now + suppressDuration);
            event.setNewCurrent(0);
            notifyClockThrottled(target.getLocation(), strike, strikesBeforeBreak);
            return;
        }

        target.setType(Material.AIR, true);
        disabledUntil.remove(key);
        violationWindows.remove(violationKey);
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
        int minimumComponents = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.minimum-components", 3), 3, 16);
        int minimumTimers = Math.clamp(plugin.getConfig().getInt(
                "automation-guard.redstone.minimum-timing-components", 2), 1, 8);
        Block target = components.stream()
                .min(Comparator.comparingInt(block -> priority(block.getType())))
                .orElse(null);
        return new ClockStructure(components.size() >= minimumComponents && timers >= minimumTimers, target);
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

    private void notifyClockThrottled(Location location, int strike, int strikesBeforeBreak) {
        String coordinates = format(location);
        plugin.getLogger().warning("[AutomationGuard] Reloj de redstone pausado en " + coordinates
                + " (aviso " + strike + "/" + strikesBeforeBreak + ").");
        Component message = Component.text("Reloj rápido pausado por unos segundos. Aviso "
                + strike + "/" + strikesBeforeBreak + ".", NamedTextColor.YELLOW);
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
        if (location == null) return;
        activity.compute(player.getUniqueId(), (uuid, state) -> {
            if (state == null) {
                return ActivityState.activeAt(location);
            }
            state.markActive(location);
            return state;
        });
    }

    private void cleanupExpiredState() {
        long now = System.currentTimeMillis();
        long cutoff = now - 15L * 60L * 1000L;
        pulseWindows.entrySet().removeIf(entry -> entry.getValue().lastSeen() < cutoff);
        disabledUntil.entrySet().removeIf(entry -> entry.getValue() < now);
        violationWindows.entrySet().removeIf(entry -> entry.getValue().lastSeen() < cutoff);

        // Limpiar registros AFK expirados de jugadores que no están conectados y llevan más de 4 horas sin actividad
        long afkCleanupCutoff = now - 4L * 3600L * 1000L;
        afkRecords.entrySet().removeIf(entry -> {
            Player p = Bukkit.getPlayer(entry.getKey());
            return (p == null || !p.isOnline()) && entry.getValue().lastKickTime() < afkCleanupCutoff
                    && entry.getValue().quarantineUntil() < now;
        });
    }

    /** Replaces the original destructive defaults only when the full legacy tuple is still present. */
    private void migrateLegacyRedstoneDefaults() {
        if (plugin.getConfig().getLong("automation-guard.redstone.fast-window-seconds", 10L) != 10L
                || plugin.getConfig().getInt("automation-guard.redstone.fast-pulse-limit", 12) != 12
                || plugin.getConfig().getLong("automation-guard.redstone.long-window-seconds", 600L) != 600L
                || plugin.getConfig().getInt("automation-guard.redstone.long-pulse-limit", 8) != 8) {
            return;
        }
        plugin.getConfig().set("automation-guard.redstone.fast-pulse-limit", 40);
        plugin.getConfig().set("automation-guard.redstone.long-window-seconds", 120);
        plugin.getConfig().set("automation-guard.redstone.long-pulse-limit", 180);
        plugin.saveConfig();
        plugin.getLogger().info("[AutomationGuard] Umbrales legacy migrados al modo gradual de tres avisos.");
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

    private static final class ViolationWindow {
        private int count;
        private long firstSeen;
        private long lastSeen;

        int count(long now, long window) {
            resetIfExpired(now, window);
            return count;
        }

        int record(long now, long window) {
            resetIfExpired(now, window);
            if (count == 0) firstSeen = now;
            lastSeen = now;
            return ++count;
        }

        long lastSeen() {
            return lastSeen;
        }

        private void resetIfExpired(long now, long window) {
            if (count > 0 && now - firstSeen > window) {
                count = 0;
                firstSeen = 0L;
                lastSeen = 0L;
            }
        }
    }

    private static final class ActivityState {
        private volatile long lastActive;
        private volatile Location anchor;
        private volatile long lastNotice;

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

        void markActive(Location location) {
            this.lastActive = System.currentTimeMillis();
            this.anchor = location.clone();
        }

        Location anchor() {
            return anchor;
        }

        long lastNotice() {
            return lastNotice;
        }

        void lastNotice(long value) {
            this.lastNotice = value;
        }
    }

    public static final class AfkRecord {
        private volatile int strikes;
        private volatile long lastKickTime;
        private volatile long quarantineUntil;
        private volatile boolean verifying;
        private volatile long joinTime;
        private final AtomicInteger actionsObserved = new AtomicInteger(0);

        public int strikes() { return strikes; }
        public void setStrikes(int strikes) { this.strikes = strikes; }
        public long lastKickTime() { return lastKickTime; }
        public void setLastKickTime(long lastKickTime) { this.lastKickTime = lastKickTime; }
        public long quarantineUntil() { return quarantineUntil; }
        public void setQuarantineUntil(long quarantineUntil) { this.quarantineUntil = quarantineUntil; }
        public boolean verifying() { return verifying; }
        public void setVerifying(boolean verifying) { this.verifying = verifying; }
        public long joinTime() { return joinTime; }
        public void setJoinTime(long joinTime) { this.joinTime = joinTime; }
        public int actionsObserved() { return actionsObserved.get(); }
        public void setActionsObserved(int val) { actionsObserved.set(val); }
        public void addAction(int count) { actionsObserved.addAndGet(count); }
    }
}
