package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.StoreManager;

public final class ReloadCommand implements CommandExecutor {

    private final Odysseia plugin;

    public ReloadCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permiso para recargar Odysseia."));
            return true;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e[Odysseia] &7Recargando configuración..."));

        try {
            // Reload main config from disk
            plugin.reloadConfig();

            // Restart StoreManager with updated config
            StoreManager.stop();
            if (plugin.getConfig().getBoolean("store.enabled", true)) {
                StoreManager.start(plugin);
            }

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[Odysseia] &aConfiguración recargada correctamente."));
            plugin.getLogger().info("Configuración recargada por: " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Odysseia] Error al recargar: " + e.getMessage()));
            plugin.getLogger().severe("Error al recargar configuración: " + e.getMessage());
        }

        return true;
    }
}
