package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

/** Ejecuta una cuenta regresiva única con guardado y respaldo IRP antes del restart. */
public final class SafeRestartCommand implements CommandExecutor {
    private final Odysseia plugin;
    private boolean running;

    public SafeRestartCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (running) {
            sender.sendMessage(ChatColor.RED + "Ya existe un reinicio en curso.");
            return true;
        }
        running = true;
        for (int second : new int[]{60, 50, 40, 30, 20, 15, 10, 5, 4, 3, 2, 1}) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> announce(second), (60L - second) * 20L);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::finish, 60L * 20L);
        return true;
    }

    private void announce(int seconds) {
        Bukkit.broadcastMessage(ChatColor.RED + "⚠ Reinicio seguro en " + ChatColor.WHITE + seconds + " segundos" + ChatColor.RED + ".");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.DARK_RED + "⚠ REINICIO", ChatColor.YELLOW + "En " + seconds + " segundos", 5, 25, 5);
        }
    }

    private void finish() {
        Bukkit.broadcastMessage(ChatColor.GOLD + "Guardando mundos e inventarios...");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "irp forcebackup " + player.getName());
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        }, 40L);
    }
}
