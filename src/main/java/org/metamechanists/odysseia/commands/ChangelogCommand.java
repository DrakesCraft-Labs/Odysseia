package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.services.ServerChangelogService;

public final class ChangelogCommand implements CommandExecutor {

    private final Odysseia plugin;
    private final ServerChangelogService changelogService;

    public ChangelogCommand(Odysseia plugin, ServerChangelogService changelogService) {
        this.plugin = plugin;
        this.changelogService = changelogService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para publicar un changelog.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /" + label + " <Título> | <Detalles/Puntos>");
            sender.sendMessage(ChatColor.GRAY + "Ejemplo: /" + label + " Actualización v2.5 | ✨ Nuevas misiones; 🔧 Corrección de Slimefun");
            return true;
        }

        String full = String.join(" ", args);
        String title = "Actualización del Servidor";
        String details = full;

        if (full.contains("|")) {
            String[] parts = full.split("\\|", 2);
            title = parts[0].trim();
            details = parts[1].trim();
        }

        String author = sender.getName();
        changelogService.postManualChangelog(title, details, author);
        sender.sendMessage(ChatColor.GREEN + "[Odysseia] Changelog enviado con éxito al canal de Discord.");
        return true;
    }
}
