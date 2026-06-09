package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

public final class PapaDeMarCommand implements CommandExecutor {

    private final Odysseia plugin;

    public PapaDeMarCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.papademar")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para usar este comando."));
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cJugador no encontrado."));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cUsa /papademar <jugador> desde consola."));
            return true;
        }

        ItemStack potato = plugin.createPapaDeMarItem();
        target.getInventory().addItem(potato);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aEntregaste la &6&l✦ Papa de mar ✦ &aa " + target.getName() + "."));
        if (target != sender) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eRecibiste la &6&l✦ Papa de mar ✦&e."));
        }

        return true;
    }
}
