package org.metamechanists.odysseia.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.listeners.ServerAutomationListener;

/** Muestra la racha diaria persistida por Odysseia. */
public final class DailyCommand implements CommandExecutor {
    private final ServerAutomationListener automation;

    public DailyCommand(ServerAutomationListener automation) {
        this.automation = automation;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        automation.showDaily(player);
        return true;
    }
}
