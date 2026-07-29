package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

/**
 * Guarda el estado antes de un reinicio gestionado externamente.
 * Pterodactyl debe recibir la señal de reinicio mediante su API; el comando
 * vanilla {@code /restart} depende de un script local inexistente en el contenedor.
 */
public final class SafeRestartCommand implements CommandExecutor {

    private final Odysseia plugin;
    public SafeRestartCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof org.bukkit.entity.Player player && !player.isOp() && !player.hasPermission("drakes.admin") && !player.hasPermission("odysseia.admin")) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "No tienes permiso para reiniciar el servidor.");
            return true;
        }

        Bukkit.savePlayers();
        executeConsoleCommand("save-all flush");
        sender.sendMessage(org.bukkit.ChatColor.YELLOW + "Guardado completado. Reinicia DrakesCraft desde Pterodactyl o scheduled_drakescraft_restart.ps1.");
        plugin.getLogger().warning("[Restart] /restart30 no envía /restart dentro de Pterodactyl; usa la API externa.");
        return true;
    }

    private void executeConsoleCommand(String cmd) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception ignored) {
        }
    }
}
