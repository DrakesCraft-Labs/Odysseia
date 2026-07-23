package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.BossManager;

import java.util.ArrayList;
import java.util.List;

public final class BossCommand implements CommandExecutor, TabCompleter {

    private final Odysseia plugin;
    private final BossManager bossManager;

    public BossCommand(Odysseia plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.boss.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawnallbosses")) {
            return spawnAllBosses(sender);
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c&lUSO: &e/boss <spawn|spawnall|give> ..."));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("spawnall")) {
            return spawnAllBosses(sender);
        }

        // ── SUBCOMANDO: SPAWN ──
        if (sub.equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede ser ejecutado por jugadores."));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&lUSO: &e/boss spawn <tipo>"));
                return true;
            }

            String bossType = args[1].toLowerCase();
            if (!isValidBossType(bossType)) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&lERROR: &eEl jefe mítico '" + bossType + "' no existe."));
                return true;
            }

            var spawned = bossManager.spawnBoss(bossType, player.getLocation());
            if (spawned != null) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a&l¡ÉXITO! &eHas invocado al jefe mítico " + spawned.getDisplayName() + " &een tu posición."));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cOcurrió un error al invocar al jefe."));
            }
            return true;
        }

        // ── SUBCOMANDO: GIVE (ENTREGAR INVOCADOR) ──
        if (sub.equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&lUSO: &e/boss give <jugador> <tipo>"));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&lERROR: &eEl jugador '" + args[1] + "' no está conectado."));
                return true;
            }

            String bossType = args[2].toLowerCase();
            if (!isValidBossType(bossType)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&lERROR: &eEl jefe mítico '" + bossType + "' no existe."));
                return true;
            }

            org.bukkit.inventory.ItemStack summoner = org.metamechanists.odysseia.items.OdysseyItemManager.createBossSummoner(bossType);
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftovers = target.getInventory().addItem(summoner);
            for (org.bukkit.inventory.ItemStack drop : leftovers.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), drop);
            }

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a&l¡ÉXITO! &eEntregado Invocador de " + bossType.toUpperCase() + " a " + target.getName()));
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&d&l⚡ &e¡Has recibido un &dInvocador de " + bossType.toUpperCase() + "&e! Haz clic derecho para despertarlo."));
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c&lUSO: &e/boss <spawn|spawnall|give> ..."));
        return true;
    }

    /** Invoca todos los jefes en un anillo para evitar entidades superpuestas. */
    private boolean spawnAllBosses(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cEste comando solo puede ser ejecutado por jugadores."));
            return true;
        }

        List<String> bosses = getBossList();
        World world = player.getWorld();
        Location origin = player.getLocation();
        int spawned = 0;
        double radius = 48.0;

        for (int index = 0; index < bosses.size(); index++) {
            double angle = Math.PI * 2.0 * index / bosses.size();
            int x = origin.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z);
            Location spawnLocation = new Location(world, x + 0.5, y + 1.0, z + 0.5);

            if (bossManager.spawnBoss(bosses.get(index), spawnLocation) != null) {
                spawned++;
            }
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6&l[MÍTICO] &eInvocados &f" + spawned + "&e de &f" + bosses.size()
                        + "&e jefes en un anillo de &f48 bloques&e."));
        return true;
    }

    private boolean isValidBossType(String type) {
        return type.equals("circe") || type.equals("polifemo")
                || type.equals("dios_corrupto") || type.equals("dios-corrupto")
                || type.equals("thor") || type.equals("ares")
                || type.equals("hades") || type.equals("poseidon")
                || type.equals("zeus") || type.equals("loki")
                || type.equals("odin") || type.equals("kratos")
                || type.equals("heimdall") || type.equals("hidra")
                || type.equals("cerbero") || type.equals("artemisa")
                || type.equals("tifon") || type.equals("tifón")
                || type.equals("prometeo")
                || type.equals("coloso_end") || type.equals("coloso-end") || type.equals("coloso")
                || type.equals("wither_storm") || type.equals("wither-storm") || type.equals("witherstorm") || type.equals("wither")
                || type.equals("dragon_ancestral") || type.equals("dragon-ancestral") || type.equals("dragon");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("odysseia.boss.admin")) {
            return completions;
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("spawn".startsWith(input)) completions.add("spawn");
            if ("spawnall".startsWith(input)) completions.add("spawnall");
            if ("give".startsWith(input)) completions.add("give");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                String input = args[1].toLowerCase();
                List<String> all = getBossList();
                for (String b : all) {
                    if (b.startsWith(input)) completions.add(b);
                }
            } else if (args[0].equalsIgnoreCase("give")) {
                // Autocompletar jugadores conectados
                String input = args[1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input)) {
                        completions.add(p.getName());
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String input = args[2].toLowerCase();
            List<String> all = getBossList();
            for (String b : all) {
                if (b.startsWith(input)) completions.add(b);
            }
        }

        return completions;
    }

    private List<String> getBossList() {
        return List.of(
                "circe", "polifemo", "dios_corrupto",
                "thor", "ares", "hades", "poseidon", "zeus",
                "loki", "odin", "kratos",
                "heimdall", "hidra", "cerbero", "artemisa", "tifon", "prometeo",
                "coloso_end", "wither_storm", "dragon_ancestral"
        );
    }
}
