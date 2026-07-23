package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

public final class LenadorCommand implements CommandExecutor {

    private final Odysseia plugin;

    public LenadorCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cEste comando solo puede ser ejecutado por un jugador."));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("staff.lumberjack.spawn")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para invocar al Leñador Loco."));
            return true;
        }

        int level = 140;
        if (args.length > 0) {
            try {
                level = Integer.parseInt(args[0]);
                if (level < 140) {
                    level = 140;
                } else if (level > 180) {
                    level = 180;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsa: /lenador [nivel (140-180)]"));
                return true;
            }
        }

        // Run the LevelledMobs console command silently
        String lmCommand = String.format("lm summon 1 vindicator %d atPlayer %s", level, player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), lmCommand);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aInvocaste al &2&lLeñador Loco&a a nivel " + level + "."));
        return true;
    }
}
