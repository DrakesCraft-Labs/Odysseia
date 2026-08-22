package org.metamechanists.odysseia.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.restart.RestartCountdown;
import org.metamechanists.odysseia.restart.RestartRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;

/**
 * Reinicio avisado: cuenta atras, guardado y peticion a Star.
 *
 * Antes este comando no reiniciaba nada. Guardaba y le decia al administrador que fuera a hacerlo a
 * mano, asi que el nombre mentia y nadie lo usaba. Ahora hace las tres partes:
 *
 *   1. Avisa a los jugadores con tiempo, para que puedan ponerse a salvo.
 *   2. Guarda jugadores, mundos y las bovedas de modalidad.
 *   3. Deja la peticion que Star recoge para llamar al panel.
 *
 * El reinicio en si no puede dispararse desde aqui: Pterodactyl no permite que el servidor se
 * reinicie a si mismo, y la API key vive en Star a proposito. Ver {@link RestartRequest}.
 *
 * Uso: {@code /restart30 [segundos] [motivo...]} · {@code /restart30 cancelar}
 */
public final class SafeRestartCommand implements CommandExecutor {

    private static final String PERMISO = "odysseia.admin";

    private final Odysseia plugin;
    private BukkitTask cuentaAtras;
    private int restantes;

    public SafeRestartCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!puede(sender)) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para reiniciar el servidor.");
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("cancelar") || args[0].equalsIgnoreCase("cancel"))) {
            return cancelar(sender);
        }

        if (cuentaAtras != null) {
            sender.sendMessage(color("&6DrakesCraft &8· &7Ya hay un reinicio en marcha, quedan &e"
                    + RestartCountdown.tiempo(restantes) + "&7. Usa &e/" + label + " cancelar&7 para detenerlo."));
            return true;
        }

        int segundos = plugin.getConfig().getInt("reinicio.segundos-por-defecto", 30);
        int primerArgumento = 0;
        if (args.length > 0) {
            try {
                segundos = Integer.parseInt(args[0]);
                primerArgumento = 1;
            } catch (NumberFormatException ignored) {
                // sin numero: todo lo escrito es el motivo
            }
        }
        segundos = Math.max(5, Math.min(segundos, 3600));
        String motivo = args.length > primerArgumento
                ? String.join(" ", java.util.Arrays.copyOfRange(args, primerArgumento, args.length))
                : plugin.getConfig().getString("reinicio.motivo-por-defecto", "mantenimiento");

        arrancar(sender, segundos, motivo);
        return true;
    }

    private boolean puede(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;   // consola
        return player.isOp() || player.hasPermission(PERMISO) || player.hasPermission("drakes.admin");
    }

    private void arrancar(CommandSender sender, int segundos, String motivo) {
        restantes = segundos;
        String quien = sender.getName();
        plugin.getLogger().info("[Restart] " + quien + " pidio reinicio en " + segundos + "s. Motivo: " + motivo);
        anunciar(restantes, motivo);

        cuentaAtras = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            restantes--;
            if (restantes > 0) {
                if (RestartCountdown.debeAnunciar(restantes)) anunciar(restantes, motivo);
                return;
            }
            detenerTarea();
            ejecutar(quien, motivo);
        }, 20L, 20L);

        sender.sendMessage(color("&6DrakesCraft &8· &7Reinicio avisado para dentro de &e"
                + RestartCountdown.tiempo(segundos) + "&7. Motivo: &e" + motivo));
    }

    private boolean cancelar(CommandSender sender) {
        if (cuentaAtras == null) {
            sender.sendMessage(color("&6DrakesCraft &8· &7No hay ningun reinicio en marcha."));
            return true;
        }
        detenerTarea();
        Bukkit.broadcast(componente("&a&l✔ &fEl reinicio ha sido &acancelado&f. Podeis seguir."));
        plugin.getLogger().info("[Restart] Cuenta atras cancelada por " + sender.getName());
        return true;
    }

    private void detenerTarea() {
        if (cuentaAtras != null) {
            cuentaAtras.cancel();
            cuentaAtras = null;
        }
    }

    /** Guarda todo lo que pueda perderse y deja la peticion para Star. */
    private void ejecutar(String quien, String motivo) {
        Bukkit.broadcast(componente("&c&l⚠ &fReiniciando. &7Guardando todo, no cerreis el juego."));

        Bukkit.savePlayers();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
        respaldarInventarios();
        // Las bovedas de modalidad viven en su propia base y no las cubre save-all.
        try {
            plugin.flushModalityVaults();
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.WARNING, "[Restart] No se pudieron volcar las bovedas", error);
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");

        try {
            var ruta = RestartRequest.escribir(plugin.getDataFolder().toPath(), quien, motivo);
            plugin.getLogger().info("[Restart] Peticion dejada en " + ruta
                    + "; Star la recogera y llamara al panel.");
        } catch (IOException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Restart] No se pudo dejar la peticion de reinicio. Hay que reiniciar a mano "
                            + "desde Star con: python3 scripts/control_drakescraft.py restart", error);
            Bukkit.broadcast(componente("&e&l⚠ &fEl reinicio se hara en unos minutos. &7Todo quedo guardado."));
        }
    }

    /** Crea un punto de restauracion individual antes de entregar el reinicio a Star. */
    private void respaldarInventarios() {
        if (!Bukkit.getPluginManager().isPluginEnabled("InventoryRollbackPlus")) {
            plugin.getLogger().warning("[Restart] InventoryRollbackPlus no esta activo; no se crearon respaldos de inventario.");
            return;
        }

        for (Player jugador : Bukkit.getOnlinePlayers()) {
            boolean enviado = Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(), "irp forcebackup " + jugador.getName());
            if (!enviado) {
                plugin.getLogger().warning("[Restart] IRP rechazo el respaldo de " + jugador.getName());
            }
        }
    }

    private void anunciar(int restantes, String motivo) {
        String tiempo = RestartCountdown.tiempo(restantes);
        Bukkit.broadcast(componente("&6&l⚡ &fReinicio en &e&l" + tiempo + " &7(" + motivo + ")"));

        Title titulo = Title.title(
                componente("&c&lREINICIO"),
                componente("&fen &e" + tiempo),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300)));
        // Los ultimos cinco segundos suben de tono para que se note sin mirar el chat.
        float tono = restantes <= 5 ? 1.6F : 1.0F;
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            jugador.showTitle(titulo);
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, tono);
        }
    }

    private static Component componente(String texto) {
        return LegacyComponentSerializer.legacySection().deserialize(color(texto));
    }

    private static String color(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
}
