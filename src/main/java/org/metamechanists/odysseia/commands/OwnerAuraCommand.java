package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.Odysseia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Eliminación administrativa absoluta dentro de un radio controlado. */
public final class OwnerAuraCommand implements CommandExecutor, TabCompleter {

    private static final List<Integer> ALLOWED_RADII = List.of(2, 5, 10, 100);
    private final Odysseia plugin;

    public OwnerAuraCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando necesita la posición de un jugador.");
            return true;
        }
        Integer radius = args.length == 1 ? parseRadius(args[0]) : null;
        if (radius == null) {
            player.sendMessage(ChatColor.RED + "Uso: /auradueño <2|5|10|100>");
            return true;
        }

        Location center = player.getLocation().clone();
        int removed = purge(center, radius, player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> purge(center, radius, player.getUniqueId()));

        player.playSound(center, Sound.ENTITY_WITHER_DEATH, 0.8F, 0.55F);
        player.sendMessage(ChatColor.DARK_RED + "Aura absoluta: " + ChatColor.RED + removed
                + ChatColor.DARK_RED + " entidad(es) eliminadas en " + radius + " bloques.");
        plugin.getLogger().warning("[AuraDueño] " + player.getName() + " eliminó " + removed
                + " entidad(es) en radio " + radius + " desde " + formatLocation(center) + '.');
        return true;
    }

    /** Fuerza muerte de seres vivos y elimina directamente el resto. */
    private int purge(Location center, int radius, UUID executorId) {
        Set<UUID> processed = new HashSet<>();
        int removed = 0;
        for (Entity nearby : new ArrayList<>(center.getWorld().getNearbyEntities(
                center, radius, radius, radius,
                entity -> isInsideSphere(center, entity.getLocation(), radius)))) {
            Entity target = nearby instanceof ComplexEntityPart part ? part.getParent() : nearby;
            if (target.getUniqueId().equals(executorId) || !processed.add(target.getUniqueId())) continue;
            try {
                target.setInvulnerable(false);
                if (target instanceof LivingEntity living) {
                    living.setHealth(0.0);
                } else {
                    target.remove();
                }
                removed++;
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("[AuraDueño] No se pudo eliminar " + target.getType()
                        + " (" + target.getUniqueId() + "): " + exception.getMessage());
            }
        }
        return removed;
    }

    static Integer parseRadius(String raw) {
        try {
            int radius = Integer.parseInt(raw);
            return ALLOWED_RADII.contains(radius) ? radius : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static boolean isInsideSphere(Location center, Location target, int radius) {
        return center.getWorld() == target.getWorld() && center.distanceSquared(target) <= radius * radius;
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName() + ' ' + location.getBlockX() + ','
                + location.getBlockY() + ',' + location.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        return ALLOWED_RADII.stream().map(String::valueOf)
                .filter(radius -> radius.startsWith(args[0])).toList();
    }
}
