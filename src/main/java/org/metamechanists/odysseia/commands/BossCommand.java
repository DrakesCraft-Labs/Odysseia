package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
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

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede ser ejecutado por jugadores."));
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("spawn")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lUSO: &e/boss spawn <circe|polifemo|dios_corrupto>"));
            return true;
        }

        String bossType = args[1].toLowerCase();
        if (!bossType.equals("circe") && !bossType.equals("polifemo") && !bossType.equals("dios_corrupto") && !bossType.equals("dios-corrupto")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lERROR: &eEl jefe místico '" + bossType + "' no existe. Usa: circe, polifemo o dios_corrupto."));
            return true;
        }

        var spawned = bossManager.spawnBoss(bossType, player.getLocation());
        if (spawned != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&l¡ÉXITO! &eHas invocado al jefe místico " + spawned.getDisplayName() + " &een tu posición."));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cOcurrió un error al invocar al jefe."));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("odysseia.boss.admin")) {
            return completions;
        }

        if (args.length == 1) {
            if ("spawn".startsWith(args[0].toLowerCase())) {
                completions.add("spawn");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            String input = args[1].toLowerCase();
            if ("circe".startsWith(input)) {
                completions.add("circe");
            }
            if ("polifemo".startsWith(input)) {
                completions.add("polifemo");
            }
            if ("dios_corrupto".startsWith(input) || "dios-corrupto".startsWith(input)) {
                completions.add("dios_corrupto");
            }
        }

        return completions;
    }
}
