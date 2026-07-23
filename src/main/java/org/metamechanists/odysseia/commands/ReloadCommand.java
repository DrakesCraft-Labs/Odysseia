package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.odysseia.Odysseia;

import java.util.List;

/** Comando administrativo para recargar YAML y servicios runtime sin reiniciar el servidor. */
public final class ReloadCommand implements CommandExecutor, TabCompleter {
    private final Odysseia plugin;

    public ReloadCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(color("&6[Odysseia] &7Productos: &f" + plugin.getPurchaseEngineProductCount()
                    + " &8| &7Instance: &f" + plugin.getInstanceId()));
            return true;
        }
        if (!args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(color("&eUso: &f/odysseia reload &7o &f/odysseia status"));
            return true;
        }
        if (!sender.hasPermission("odysseia.reload")) {
            sender.sendMessage(color("&cNo tienes permiso para recargar Odysseia."));
            return true;
        }

        sender.sendMessage(color("&e[Odysseia] &7Recargando config.yml, purchases.yml y servicios..."));
        try {
            List<String> errors = plugin.reloadRuntime();
            if (errors.isEmpty()) {
                sender.sendMessage(color("&a[Odysseia] Recarga completa sin errores."));
                return true;
            }
            sender.sendMessage(color("&6[Odysseia] Recarga aplicada con advertencias:"));
            errors.stream().limit(8).forEach(error -> sender.sendMessage(color("&e- &f" + error)));
            if (errors.size() > 8) {
                sender.sendMessage(color("&e... y " + (errors.size() - 8) + " advertencias más en consola."));
            }
        } catch (Exception error) {
            sender.sendMessage(color("&c[Odysseia] Error al recargar: " + error.getMessage()));
            plugin.getLogger().severe("[Reload] Error al recargar: " + error.getMessage());
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
