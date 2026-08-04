package org.metamechanists.odysseia.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.metamechanists.odysseia.Odysseia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Renders ProtectionStones limits client-side without changing world blocks. */
public final class ProtectionBorderListener implements Listener {

    private final Odysseia plugin;
    private final Map<UUID, RenderedBorder> rendered = new HashMap<>();
    private final Method regionFromLocation;
    private final Method regionIsOwner;
    private final Method regionIsMember;
    private final Method regionGetId;
    private final Method regionGetWorld;
    private final Method regionGetWorldGuard;
    private boolean loggedUnavailable;

    public ProtectionBorderListener(Odysseia plugin) {
        this.plugin = plugin;
        Method from = null;
        Method owner = null;
        Method member = null;
        Method id = null;
        Method world = null;
        Method worldGuard = null;
        try {
            Class<?> region = Class.forName("dev.espi.protectionstones.PSRegion");
            from = region.getMethod("fromLocation", Location.class);
            owner = region.getMethod("isOwner", UUID.class);
            member = region.getMethod("isMember", UUID.class);
            id = region.getMethod("getId");
            world = region.getMethod("getWorld");
            worldGuard = region.getMethod("getWGRegion");
        } catch (ReflectiveOperationException error) {
            logUnavailable(error);
        }
        regionFromLocation = from;
        regionIsOwner = owner;
        regionIsMember = member;
        regionGetId = id;
        regionGetWorld = world;
        regionGetWorldGuard = worldGuard;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        refresh(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(event.getPlayer(), true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) return;
        refresh(event.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockChange(BlockPlaceEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(event.getPlayer(), true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(event.getPlayer(), true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnclaim(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().trim().toLowerCase();
        if (command.equals("/ps unclaim") || command.startsWith("/ps unclaim ")) {
            clear(event.getPlayer());
            plugin.getServer().getScheduler().runTask(plugin, () -> refresh(event.getPlayer(), true));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clear(event.getPlayer());
        rendered.remove(event.getPlayer().getUniqueId());
    }

    private void refresh(Player player, boolean force) {
        if (!plugin.getConfig().getBoolean("protection-border.enabled", true) || !available()) {
            clear(player);
            return;
        }

        try {
            Object region = regionFromLocation.invoke(null, player.getLocation());
            if (region == null || !canSee(player, region)) {
                clear(player);
                return;
            }

            String regionKey = regionGetWorld.invoke(region).toString() + ":" + regionGetId.invoke(region);
            int y = player.getLocation().getBlockY();
            RenderedBorder previous = rendered.get(player.getUniqueId());
            if (!force && previous != null && previous.regionKey.equals(regionKey) && previous.y == y) return;

            clear(player);
            List<Location> border = buildBorder(region, y);
            if (border.isEmpty()) return;
            BlockData data = Material.matchMaterial(plugin.getConfig().getString(
                "protection-border.material", "LIGHT_BLUE_STAINED_GLASS")).createBlockData();
            List<Location> shown = new ArrayList<>();
            for (Location location : border) {
                if (!location.getWorld().getBlockAt(location).getType().isAir()) continue;
                player.sendBlockChange(location, data);
                shown.add(location);
            }
            rendered.put(player.getUniqueId(), new RenderedBorder(regionKey, y, shown));
        } catch (ReflectiveOperationException | RuntimeException error) {
            logUnavailable(error);
            clear(player);
        }
    }

    private List<Location> buildBorder(Object region, int y) throws ReflectiveOperationException {
        Object worldGuardRegion = regionGetWorldGuard.invoke(region);
        if (worldGuardRegion == null) return List.of();
        Method minMethod = worldGuardRegion.getClass().getMethod("getMinimumPoint");
        Method maxMethod = worldGuardRegion.getClass().getMethod("getMaximumPoint");
        Object min = minMethod.invoke(worldGuardRegion);
        Object max = maxMethod.invoke(worldGuardRegion);
        Method x = min.getClass().getMethod("getBlockX");
        Method z = min.getClass().getMethod("getBlockZ");
        Method maxX = max.getClass().getMethod("getBlockX");
        Method maxZ = max.getClass().getMethod("getBlockZ");
        int minX = (int) x.invoke(min);
        int minZ = (int) z.invoke(min);
        int endX = (int) maxX.invoke(max);
        int endZ = (int) maxZ.invoke(max);
        int limit = Math.max(64, plugin.getConfig().getInt("protection-border.max-blocks", 2048));
        List<Location> result = new ArrayList<>(Math.min(limit, 2048));
        Object worldObject = regionGetWorld.invoke(region);
        org.bukkit.World world = (org.bukkit.World) worldObject;
        for (int currentX = minX; currentX <= endX && result.size() < limit; currentX++) {
            result.add(new Location(world, currentX, y, minZ));
            if (endZ != minZ && result.size() < limit) result.add(new Location(world, currentX, y, endZ));
        }
        for (int currentZ = minZ + 1; currentZ < endZ && result.size() < limit; currentZ++) {
            result.add(new Location(world, minX, y, currentZ));
            if (endX != minX && result.size() < limit) result.add(new Location(world, endX, y, currentZ));
        }
        return result;
    }

    private boolean canSee(Player player, Object region) throws ReflectiveOperationException {
        if (player.hasPermission(plugin.getConfig().getString("protection-border.staff-permission", "odysseia.protection.border"))) return true;
        UUID id = player.getUniqueId();
        return (boolean) regionIsOwner.invoke(region, id) || (boolean) regionIsMember.invoke(region, id);
    }

    private void clear(Player player) {
        RenderedBorder previous = rendered.remove(player.getUniqueId());
        if (previous == null) return;
        for (Location location : previous.locations) {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
            player.sendBlockChange(location, location.getBlock().getBlockData());
        }
    }

    private boolean available() {
        return regionFromLocation != null && regionIsOwner != null && regionIsMember != null
            && regionGetId != null && regionGetWorld != null && regionGetWorldGuard != null;
    }

    private static boolean sameBlock(Location first, Location second) {
        return first.getWorld() == second.getWorld() && first.getBlockX() == second.getBlockX()
            && first.getBlockY() == second.getBlockY() && first.getBlockZ() == second.getBlockZ();
    }

    private void logUnavailable(Throwable error) {
        if (!loggedUnavailable) {
            loggedUnavailable = true;
            plugin.getLogger().warning("[ProtectionBorder] Integración visual no disponible: " + error.getMessage());
        }
    }

    private record RenderedBorder(String regionKey, int y, List<Location> locations) {}
}
