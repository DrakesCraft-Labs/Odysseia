package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;

/**
 * Ejecuta una secuencia dinámica y amigable de reinicio seguro:
 * Anuncios personalizados ("Jack anda aplicando mejoras", "Volvemos en ~3 minutos"),
 * guardado completo de datos (NBT, IRP, Slimefun, sBank, mundos) y kick informativo.
 */
public final class SafeRestartCommand implements CommandExecutor {

    private final Odysseia plugin;
    private boolean running;

    public SafeRestartCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && !player.isOp() && !player.hasPermission("drakes.admin") && !player.hasPermission("odysseia.admin")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para reiniciar el servidor.");
            return true;
        }

        if (running) {
            sender.sendMessage(ChatColor.RED + "Ya existe una cuenta regresiva de reinicio en curso.");
            return true;
        }

        running = true;
        
        // Mensaje inicial global dinámico y amigable
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.RED + "🔨 " + ChatColor.GOLD + ChatColor.BOLD + "[DRAKESCRAFT MANTENIMIENTO]");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Jack anda aplicando mejoras y mantenimiento en el servidor.");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Lamentamos los inconvenientes. El servidor volverá a estar disponible en aprox. " + ChatColor.AQUA + "3 minutos" + ChatColor.GRAY + ".");
        Bukkit.broadcastMessage(ChatColor.RED + "⚠ Reinicio seguro programado en " + ChatColor.WHITE + "30 segundos" + ChatColor.RED + ".");
        Bukkit.broadcastMessage("");

        // Tiempos de cuenta regresiva
        int[] countdownTimes = {30, 20, 15, 10, 5, 4, 3, 2, 1};
        for (int seconds : countdownTimes) {
            long delayTicks = (30L - seconds) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> announce(seconds), delayTicks);
        }

        // Secuencia de guardado y restart a los 30s
        Bukkit.getScheduler().runTaskLater(plugin, this::executeFullSaveAndRestart, 30L * 20L);
        return true;
    }

    private void announce(int seconds) {
        String msg = ChatColor.RED + "⚠ Reinicio en " + ChatColor.WHITE + seconds + "s" + ChatColor.RED + ". " + ChatColor.GRAY + "Guardando progreso... (Volvemos en ~3 min)";
        Bukkit.broadcastMessage(msg);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(ChatColor.DARK_RED + "⚠ REINICIO DE SERVIDOR", ChatColor.YELLOW + "En " + ChatColor.WHITE + seconds + "s" + ChatColor.YELLOW + " • Volvemos en ~3 min", 3, 20, 3);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, seconds <= 5 ? 1.8f : 1.2f);
        }
    }

    private void executeFullSaveAndRestart() {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GOLD + "💾 " + ChatColor.BOLD + "GUARDANDO DATOS:" + ChatColor.YELLOW + " Inventarios, Slimefun, Bancos y Mundos...");
        Bukkit.broadcastMessage("");

        // 1. Guardar inventarios e NBT de jugadores
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                player.saveData();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "irp forcebackup " + player.getName());
            } catch (Exception ex) {
                plugin.getLogger().warning("No se pudo respaldar jugador " + player.getName() + ": " + ex.getMessage());
            }
        }
        Bukkit.savePlayers();

        // 2. Guardar Slimefun, Bancos y plugins
        executeConsoleCommand("sf save");
        executeConsoleCommand("slimefun save");
        executeConsoleCommand("sbank save");

        // 3. Flush de mundos y chunks
        executeConsoleCommand("save-all flush");

        // 4. Desconexión amistosa e informativa
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String kickScreen = ChatColor.RED + "" + ChatColor.BOLD + "✦ DRAKESCRAFT - MANTENIMIENTO SEGURO ✦\n\n"
                    + ChatColor.YELLOW + "🔨 Jack está aplicando mejoras y actualizaciones en el servidor.\n"
                    + ChatColor.GREEN + "✔ Tus inventarios, construcciones y progreso han sido guardados.\n\n"
                    + ChatColor.AQUA + "⏱ El servidor volverá a estar disponible en aproximadamente 3 minutos.\n"
                    + ChatColor.GRAY + "¡Lamentamos los inconvenientes y agradecemos tu paciencia!";

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(kickScreen);
            }

            // 5. Enviar restart final a la consola
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeConsoleCommand("save-all flush");
                plugin.getLogger().info("⚡ Guardado completado. Enviando comando restart...");
                executeConsoleCommand("restart");
            }, 40L);

        }, 20L);
    }

    private void executeConsoleCommand(String cmd) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } catch (Exception ignored) {
        }
    }
}
