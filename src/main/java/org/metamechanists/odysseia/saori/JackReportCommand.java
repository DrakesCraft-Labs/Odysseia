package org.metamechanists.odysseia.saori;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Comando /jack y /reportar con blindaje Anti-Prompt Injection,
 * rate-limiting estricto y persistencia estructurada para SAORI.
 */
public final class JackReportCommand implements CommandExecutor, TabCompleter {

    private static final long COOLDOWN_MILLIS = 60_000L; // 60 segundos por jugador
    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 200;

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
            "(?i)(ignore\\s+(?:all\\s+)?(?:previous\\s+)?instructions|system\\s+prompt|as\\s+saori|act\\s+as\\s+admin|dame\\s+op|give\\s+me\\s+op|dan\\s+mode|developer\\s+mode|roleplay\\s+as|run\\s+command|console:|/(?:op|pex|lp|ban|stop|execute|eval|reload)\\b)"
    );

    private final JavaPlugin plugin;
    private final File reportsLogFile;
    private final Map<UUID, Long> lastReportTimes = new ConcurrentHashMap<>();

    public JackReportCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reportsLogFile = new File(plugin.getDataFolder(), "saori-player-reports.jsonl");
    }

    public static boolean isInsecureMessage(@NotNull String message) {
        String normalized = normalizeText(message);
        return INJECTION_PATTERN.matcher(normalized).find();
    }

    public static String normalizeText(@NotNull String text) {
        String n = Normalizer.normalize(text, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", ""); // Quita acentos
        n = n.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""); // Quita caracteres de control
        return n.trim();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Uso: /" + label + " <detalle del problema o mensaje para Jack>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Ejemplo: /" + label + " Hay un bug visual con el teleporter de Slimefun", NamedTextColor.GRAY));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Este comando solo puede ser usado por jugadores in-game.", NamedTextColor.RED));
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // 1. Rate-Limiting
        Long lastTime = lastReportTimes.get(uuid);
        if (lastTime != null && (now - lastTime) < COOLDOWN_MILLIS) {
            long waitSecs = Math.max(1, (COOLDOWN_MILLIS - (now - lastTime)) / 1000L);
            player.sendMessage(Component.text("⏳ [SAORI] Por favor espera " + waitSecs + "s antes de enviar otro reporte.", NamedTextColor.RED));
            return true;
        }

        // 2. Unir y limpiar mensaje
        String rawMessage = String.join(" ", args);
        String cleaned = normalizeText(rawMessage);

        // 3. Validación de Longitud
        if (cleaned.length() < MIN_LENGTH) {
            player.sendMessage(Component.text("⛔ [SAORI] El mensaje es demasiado corto (mínimo " + MIN_LENGTH + " caracteres).", NamedTextColor.RED));
            return true;
        }
        if (cleaned.length() > MAX_LENGTH) {
            cleaned = cleaned.substring(0, MAX_LENGTH);
        }

        // 4. Blindaje Anti-Prompt Injection
        if (isInsecureMessage(cleaned)) {
            player.sendMessage(Component.text("⛔ [SAORI] Sintaxis o patrón de comando no permitido en el reporte.", NamedTextColor.RED));
            plugin.getLogger().log(Level.WARNING, "[SAORI-SECURITY] Jugador {0} ({1}) intentó inyección en /jack: \"{2}\"",
                    new Object[]{player.getName(), uuid, cleaned});
            return true;
        }

        // Actualizar cooldown
        lastReportTimes.put(uuid, now);

        // 5. Persistencia y Registro Forense
        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "desconocido";
        String timeStr = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(now));

        String jsonEntry = String.format(
                "{\"timestamp\":\"%s\",\"player\":\"%s\",\"uuid\":\"%s\",\"world\":\"%s\",\"x\":%.1f,\"y\":%.1f,\"z\":%.1f,\"message\":\"%s\"}",
                timeStr,
                escapeJson(player.getName()),
                uuid.toString(),
                escapeJson(worldName),
                loc.getX(), loc.getY(), loc.getZ(),
                escapeJson(cleaned)
        );

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveReport(jsonEntry));

        // 6. Respuesta privada al jugador
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("✔ [SAORI] Tu mensaje ha sido registrado de forma privada.", NamedTextColor.GREEN, TextDecoration.BOLD));
        player.sendMessage(Component.text("Jack y el sistema de orquestación lo revisarán a la brevedad.", NamedTextColor.GRAY));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.LIGHT_PURPLE));

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        } catch (Exception ignored) {}

        plugin.getLogger().log(Level.INFO, "[SAORI-REPORT] Reporte recibido de {0} ({1}): {2}",
                new Object[]{player.getName(), worldName, cleaned});

        return true;
    }

    private synchronized void saveReport(String jsonLine) {
        try {
            if (!reportsLogFile.getParentFile().exists()) {
                reportsLogFile.getParentFile().mkdirs();
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(reportsLogFile, true))) {
                out.println(jsonLine);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[SAORI] Error al guardar reporte de jugador en JSONL", e);
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("<escribe_tu_mensaje_aqui>");
        }
        return Collections.emptyList();
    }
}
