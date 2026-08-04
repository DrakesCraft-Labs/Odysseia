package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Owner-only meteor impacts with bounded, protection-aware terrain damage. */
public final class MeteorCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final Method regionFromLocation;

    public MeteorCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        Method method = null;
        try {
            method = Class.forName("dev.espi.protectionstones.PSRegion")
                .getMethod("fromLocation", Location.class);
        } catch (ReflectiveOperationException ignored) {
            plugin.getLogger().warning("[Meteoritos] ProtectionStones no está disponible; se bloqueará la destrucción por seguridad.");
        }
        regionFromLocation = method;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)
            || !player.getName().equalsIgnoreCase("JackStar6677")
            || !player.hasPermission("odysseia.meteor.admin")) {
            sender.sendMessage(ChatColor.RED + "Solo el dueño puede invocar meteoritos.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "Uso: /meteorito <pequeno|pesado|void|lluvia> [cantidad] [radio]");
            return true;
        }

        MeteorType type = MeteorType.parse(args[0]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Tipo inválido. Usa pequeño, pesado, void o lluvia.");
            return true;
        }
        int amount = parseBounded(args, 1, type.defaultAmount, 1, 16);
        int radius = parseBounded(args, 2, type.defaultRadius, 8, 64);
        if (type == MeteorType.RAIN) {
            amount = Math.min(amount, 16);
        }
        launch(player, type, amount, radius);
        player.sendMessage(ChatColor.DARK_RED + "☄ " + ChatColor.GOLD + "Se invocaron " + amount
            + " meteorito(s) " + type.display + ChatColor.GOLD + ". Protecciones intactas.");
        return true;
    }

    private void launch(Player owner, MeteorType type, int amount, int radius) {
        World world = owner.getWorld();
        Location origin = owner.getLocation().clone();
        for (int index = 0; index < amount; index++) {
            long delay = index * 6L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                double distance = ThreadLocalRandom.current().nextDouble(2, radius + 1);
                Location impact = origin.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
                impact.setY(Math.min(world.getMaxHeight() - 8, Math.max(world.getMinHeight() + 8,
                    world.getHighestBlockYAt(impact) + 1)));
                spawnMeteor(world, impact, type);
                Bukkit.getScheduler().runTaskLater(plugin, () -> impact(world, impact, type), 20L);
            }, delay);
        }
    }

    private void spawnMeteor(World world, Location impact, MeteorType type) {
        Location sky = impact.clone().add(0, 28, 0);
        FallingBlock meteor = world.spawnFallingBlock(sky, type.material.createBlockData());
        meteor.setDropItem(false);
        meteor.setHurtEntities(false);
        meteor.setVelocity(new Vector(0, -1.2, 0));
        world.spawnParticle(Particle.FLAME, sky, 45, 1.2, 1.2, 1.2, 0.05);
        world.playSound(sky, Sound.ENTITY_BLAZE_SHOOT, 1.4f, 0.55f);
    }

    private void impact(World world, Location center, MeteorType type) {
        world.spawnParticle(Particle.EXPLOSION, center, 4, 1.0, 0.5, 1.0, 0.0);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 70, type.craterRadius, 1.0, type.craterRadius, 0.02);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, type.pitch);
        if (regionFromLocation == null) return;

        int radius = type.craterRadius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance > radius || ThreadLocalRandom.current().nextDouble() > 0.86) continue;
                int depth = Math.max(1, (int) Math.round((radius - distance) * type.depthFactor));
                for (int y = 0; y < depth; y++) {
                    Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() - y - 1, center.getBlockZ() + z);
                    if (!safeToBreak(block)) continue;
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private boolean safeToBreak(Block block) {
        if (block.getType().isAir() || block.getType() == Material.BEDROCK
            || block.getType() == Material.END_PORTAL || block.getType() == Material.END_PORTAL_FRAME) return false;
        try {
            return regionFromLocation.invoke(null, block.getLocation()) == null;
        } catch (ReflectiveOperationException | RuntimeException error) {
            return false;
        }
    }

    private static int parseBounded(String[] args, int index, int fallback, int min, int max) {
        if (args.length <= index) return fallback;
        try {
            return Math.clamp(Integer.parseInt(args[index]), min, max);
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("pequeno", "pesado", "void", "lluvia");
        if (args.length == 2) return List.of("1", "4", "8", "16");
        if (args.length == 3) return List.of("16", "32", "48", "64");
        return List.of();
    }

    private enum MeteorType {
        SMALL("pequeño", Material.MAGMA_BLOCK, 1, 24, 5, 1.0, 0.8f),
        HEAVY("pesado", Material.OBSIDIAN, 1, 32, 10, 1.35, 0.6f),
        VOID("void", Material.BLACK_CONCRETE, 1, 40, 13, 1.7, 0.45f),
        RAIN("lluvia", Material.NETHERRACK, 8, 48, 7, 1.1, 0.7f);

        private final String display;
        private final Material material;
        private final int defaultAmount;
        private final int defaultRadius;
        private final int craterRadius;
        private final double depthFactor;
        private final float pitch;

        MeteorType(String display, Material material, int defaultAmount, int defaultRadius, int craterRadius,
                   double depthFactor, float pitch) {
            this.display = display;
            this.material = material;
            this.defaultAmount = defaultAmount;
            this.defaultRadius = defaultRadius;
            this.craterRadius = craterRadius;
            this.depthFactor = depthFactor;
            this.pitch = pitch;
        }

        private static MeteorType parse(String value) {
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.equals("pequeno") || normalized.equals("small")) return SMALL;
            if (normalized.equals("pesado") || normalized.equals("heavy") || normalized.equals("gigante")) return HEAVY;
            if (normalized.equals("void") || normalized.equals("corrupto")) return VOID;
            if (normalized.equals("lluvia") || normalized.equals("storm")) return RAIN;
            return null;
        }
    }
}
