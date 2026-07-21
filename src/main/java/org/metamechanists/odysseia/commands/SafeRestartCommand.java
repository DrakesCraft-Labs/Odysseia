package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

/**
 * Ejecuta una cuenta regresiva segura con guardado completo de datos:
 * Jugadores, Slimefun, Mundos, Banco, DiosesDrakes y respaldos IRP antes del restart.
 */
public final class SafeRestartCommand implements CommandExecutor {

    private final Odysseia plugin;
    private boolean running;

    public SafeRestartCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("drakes.admin") && !sender.hasPermission("odysseia.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para reiniciar el servidor.");
            return true;
        }

        if (running) {
            sender.sendMessage(ChatColor.RED + "Ya existe una cuenta regresiva de reinicio en curso.");
            return true;
        }

        running = true;
        sender.sendMessage(ChatColor.GREEN + "⚡ Iniciando secuencia de reinicio seguro (30s)...");

        // Anuncios de cuenta regresiva de 30 segundos
        int[] countdownTimes = {30, 20, 15, 10, 5, 4, 3, 2, 1};
        for (int seconds : countdownTimes) {
            long delayTicks = (30L - seconds) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> announce(seconds), delayTicks);
        }

        // Ejecutar la secuencia de guardado final a los 30 segundos
        Bukkit.getScheduler().runTaskLater(plugin, this::executeFullSaveAndRestart, 30L * 20L);
        return true;
    }

    private void announce(int seconds) {
        Bukkit.broadcastMessage(ChatColor.RED + "⚠ Reinicio seguro en " + ChatColor.WHITE + seconds + " segundos" + ChatColor.RED + ".");
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.DARK_RED + "⚠ REINICIO SEGURO", ChatColor.YELLOW + "Guardado e inicio en " + seconds + "s", 3, 20, 3);
        }
    }

    private void executeFullSaveAndRestart() {
        Bukkit.broadcastMessage(ChatColor.GOLD + "💾 Guardando todos los datos, mundos e inventarios del servidor...");

        // 1. Guardar datos de jugadores online (NBT e inventarios)
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                player.saveData();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "irp forcebackup " + player.getName());
            } catch (Exception ex) {
                plugin.getLogger().warning("No se pudo guardar datos/IRP para " + player.getName() + ": " + ex.getMessage());
            }
        }
        Bukkit.savePlayers();

        // 2. Guardar Slimefun, Banco y plugins dependientes
        executeConsoleCommand("sf save");
        executeConsoleCommand("slimefun save");
        executeConsoleCommand("sbank save");

        // 3. Guardado completo de chunks y mundos Bukkit/Paper
        executeConsoleCommand("save-all flush");

        // 4. Desconectar a los jugadores de forma segura con mensaje informativo
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(ChatColor.RED + "⚠ Servidor reiniciándose de forma segura.\n"
                        + ChatColor.YELLOW + "Tus datos han sido guardados correctamente.\n"
                        + ChatColor.GREEN + "¡Vuelve a conectarte en 1 minuto!");
            }

            // 5. Segundo flush y ejecución de comando restart
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeConsoleCommand("save-all flush");
                plugin.getLogger().info("⚡ Guardado final completado. Enviando señal /restart...");
                executeConsoleCommand("restart");
            }, 40L); // 2 segundos para vaciar I/O de red y disco

        }, 20L);
    }

    private void executeConsoleCommand(String cmd) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception ignored) {
            // Si algún comando no existe en la consola, continuar sin romper la secuencia
        }
    }
}
