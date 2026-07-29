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
        if (args[0].equalsIgnoreCase("vipalerts")) {
            if (!sender.hasPermission("odysseia.reload")) {
                sender.sendMessage(color("&cNo tienes permiso."));
                return true;
            }
            sender.sendMessage(color("&e[Odysseia] &7Generando y enviando reporte de caducidad VIP a Discord..."));
            boolean ok = plugin.getVipExpiryAlertService() != null && plugin.getVipExpiryAlertService().sendVipExpiryReport();
            if (ok) {
                sender.sendMessage(color("&a[Odysseia] Reporte de caducidad VIP enviado exitosamente a Discord."));
            } else {
                sender.sendMessage(color("&c[Odysseia] No se pudo enviar el reporte. Verifica discord.webhook-url en config.yml."));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("sfmaster")) {
            return handleSfMaster(sender, args);
        }
        if (args[0].equalsIgnoreCase("maintenance")) {
            return handleMaintenance(sender, args);
        }
        if (!args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(color("&eUso: &f/odysseia <reload|status|vipalerts|sfmaster|maintenance>"));
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

    private boolean handleSfMaster(CommandSender sender, String[] args) {
        if (!sender.hasPermission("odysseia.sfmaster.audit")) {
            sender.sendMessage(color("&cNo tienes permiso para auditar SFMaster."));
            return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("audit")) {
            sender.sendMessage(color("&eUso: &f/odysseia sfmaster audit <jugador-online>"));
            return true;
        }
        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(color("&cEl jugador debe estar conectado para auditar inventario y Ender Chest."));
            return true;
        }
        var result = plugin.getSfMasterWatcher().audit(target);
        sender.sendMessage(color("&6[SFMaster] &f" + target.getName()
                + " &8| &7marcados: &f" + result.markedItems()
                + " &8| &7guías: &f" + result.cheatGuides()
                + " &8| &7candidatos legacy: &f" + result.suspiciousLegacy().size()));
        result.suspiciousLegacy().entrySet().stream().limit(20)
                .forEach(entry -> sender.sendMessage(color("&e- &f" + entry.getKey() + " &7x" + entry.getValue())));
        if (result.suspiciousLegacy().size() > 20) {
            sender.sendMessage(color("&7... y " + (result.suspiciousLegacy().size() - 20) + " IDs adicionales."));
        }
        plugin.getLogger().info("[SFMaster Audit] " + sender.getName() + " revisó a " + target.getName()
                + ": marked=" + result.markedItems() + ", guides=" + result.cheatGuides()
                + ", legacyCandidates=" + result.suspiciousLegacy());
        sender.sendMessage(color("&cLos candidatos no se borran automáticamente: pueden haber sido fabricados legítimamente."));
        return true;
    }

    private boolean handleMaintenance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("odysseia.maintenance.admin")) {
            sender.sendMessage(color("&cNo tienes permiso para administrar mantenimiento."));
            return true;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage(color(plugin.getMaintenanceGuard().isActive()
                    ? "&eMantenimiento activo por " + plugin.getMaintenanceGuard().remainingSeconds() + " segundos."
                    : "&aNo hay una ventana de mantenimiento activa."));
            return true;
        }
        if (args[1].equalsIgnoreCase("cancel")) {
            plugin.getMaintenanceGuard().cancel();
            return true;
        }
        if (args[1].equalsIgnoreCase("start")) {
            long seconds = 60L;
            if (args.length >= 3) {
                try {
                    seconds = Long.parseLong(args[2]);
                } catch (NumberFormatException error) {
                    sender.sendMessage(color("&cLos segundos deben ser un número."));
                    return true;
                }
            }
            plugin.getMaintenanceGuard().begin(seconds);
            return true;
        }
        sender.sendMessage(color("&eUso: &f/odysseia maintenance <start [segundos]|status|cancel>"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "status", "vipalerts", "sfmaster", "maintenance").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sfmaster")) {
            return List.of("audit");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("maintenance")) {
            return List.of("start", "status", "cancel");
        }
        return List.of();
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
