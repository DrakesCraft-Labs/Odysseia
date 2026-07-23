package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.events.BloodMoonManager;

/** Admin-only switch for controlled Blood Moon testing and emergency shutdown. */
public final class BloodMoonCommand implements CommandExecutor {

    private final BloodMoonManager bloodMoon;

    public BloodMoonCommand(BloodMoonManager bloodMoon) {
        this.bloodMoon = bloodMoon;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.bloodmoon.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Este comando debe ejecutarse dentro del juego.");
            return true;
        }
        String action = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (action) {
            case "start" -> sender.sendMessage(bloodMoon.forceStart(player.getWorld())
                    ? ChatColor.GREEN + "Luna de Sangre iniciada."
                    : ChatColor.YELLOW + "Ya hay una Luna de Sangre activa en este mundo.");
            case "stop" -> sender.sendMessage(bloodMoon.stop(player.getWorld())
                    ? ChatColor.GREEN + "Luna de Sangre detenida y horda retirada."
                    : ChatColor.YELLOW + "No hay una Luna de Sangre activa en este mundo.");
            default -> sender.sendMessage(ChatColor.GOLD + "Luna de Sangre: "
                    + (bloodMoon.isActive(player.getWorld()) ? ChatColor.RED + "ACTIVA" : ChatColor.GREEN + "inactiva")
                    + ChatColor.GRAY + " | /bloodmoon <start|stop|status>");
        }
        return true;
    }
}
