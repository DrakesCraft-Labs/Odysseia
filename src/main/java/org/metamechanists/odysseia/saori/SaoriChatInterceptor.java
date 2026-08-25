package org.metamechanists.odysseia.saori;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Interceptor de chat y comandos sospechosos SAORI.
 * Detecta llamadas a Jack cuando está offline, disuade a bromistas de dupes
 * y aplica el Protocolo Kali-Freeze ante bucles anómalos de comandos.
 */
public final class SaoriChatInterceptor implements Listener {

    private static final long JACK_CALL_COOLDOWN_MS = 180_000L; // 3 minutos
    private static final long DUPE_TALK_COOLDOWN_MS = 120_000L; // 2 minutos
    private static final long KALI_WINDOW_MS = 20_000L; // Ventana de 20 segundos
    private static final int KALI_THRESHOLD = 6; // 6 comandos en 20s

    private static final Pattern JACK_CALL_PATTERN = Pattern.compile(
            "(?i)(^|\\s)(?:@?jack|¿?jack\\s*est[aá]s\\??|hola\\s+jack|dios\\??|dios\\s*jack)(\\s|$|[?!.,])"
    );

    private static final Pattern DUPE_TALK_PATTERN = Pattern.compile(
            "(?i)(^|\\s)(?:dupe\\w*|duplic\\w*|clon\\w*|glitch\\s+de\\s+items)(\\s|$|[?!.,])"
    );

    private static final List<String> KALI_SUSPICIOUS_COMMANDS = List.of(
            "/team echest", "/team join", "/team leave", "/team disband",
            "/pv ", "/vault", "/trade", "/papatrueque", "/lobby", "/spawn"
    );

    private final JavaPlugin plugin;
    private final SaoriFreezeManager freezeManager;

    private final Map<UUID, Long> lastJackCallNotification = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDupeTalkNotification = new ConcurrentHashMap<>();
    private final Map<UUID, List<Long>> playerCommandTimestamps = new ConcurrentHashMap<>();

    public SaoriChatInterceptor(JavaPlugin plugin, SaoriFreezeManager freezeManager) {
        this.plugin = plugin;
        this.freezeManager = freezeManager;
    }

    public static boolean matchesJackCall(@NotNull String message) {
        String n = normalize(message);
        return JACK_CALL_PATTERN.matcher(n).find();
    }

    public static boolean matchesDupeTalk(@NotNull String message) {
        String n = normalize(message);
        return DUPE_TALK_PATTERN.matcher(n).find();
    }

    private static String normalize(String text) {
        String n = Normalizer.normalize(text, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        // 1. Menciones a Jack cuando no está disponible
        if (matchesJackCall(message)) {
            Long last = lastJackCallNotification.get(uuid);
            if (last == null || (now - last) >= JACK_CALL_COOLDOWN_MS) {
                if (!isJackAvailable()) {
                    lastJackCallNotification.put(uuid, now);
                    Bukkit.getScheduler().runTask(plugin, () -> sendJackOfflineNotice(player));
                }
            }
        }

        // 2. Jugadores hablando de dupes (Broma o real) -> Intervención Psicológica
        if (matchesDupeTalk(message)) {
            Long last = lastDupeTalkNotification.get(uuid);
            if (last == null || (now - last) >= DUPE_TALK_COOLDOWN_MS) {
                lastDupeTalkNotification.put(uuid, now);
                Bukkit.getScheduler().runTask(plugin, () -> sendDupeDeterrenceWhisper(player));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("odysseia.admin") || player.isOp()) return;

        String cmd = event.getMessage().toLowerCase().trim();
        boolean isSuspicious = false;
        for (String pattern : KALI_SUSPICIOUS_COMMANDS) {
            if (cmd.startsWith(pattern)) {
                isSuspicious = true;
                break;
            }
        }

        if (isSuspicious) {
            long now = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();
            List<Long> timestamps = playerCommandTimestamps.computeIfAbsent(uuid, k -> new ArrayList<>());

            synchronized (timestamps) {
                timestamps.removeIf(t -> (now - t) > KALI_WINDOW_MS);
                timestamps.add(now);

                if (timestamps.size() >= KALI_THRESHOLD) {
                    timestamps.clear();
                    event.setCancelled(true);
                    freezeManager.freeze(player, Duration.ofMinutes(10), "Bucle sospechoso de contenedores / desincronización (Patrón Kali)");
                    plugin.getLogger().log(Level.WARNING, "[SAORI-SECURITY] Jugador {0} ({1}) activó Protocolo Kali-Freeze por bucle rápido de comandos: {2}",
                            new Object[]{player.getName(), uuid, cmd});
                }
            }
        }
    }

    private boolean isJackAvailable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase("JackStar6677") || p.getName().equalsIgnoreCase("Jack") || p.hasPermission("odysseia.owner")) {
                return true;
            }
        }
        return false;
    }

    private void sendJackOfflineNotice(Player player) {
        player.sendMessage(Component.text("────────────────────────────────────────────", NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("🤖 [SAORI] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text("Jack no se encuentra en línea en este momento.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Puedes dejarle tu mensaje o reporte directo con ", NamedTextColor.GRAY)
                .append(Component.text("/jack <mensaje>", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(".", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("────────────────────────────────────────────", NamedTextColor.LIGHT_PURPLE));

        try {
            player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1.8f);
        } catch (Exception ignored) {}
    }

    private void sendDupeDeterrenceWhisper(Player player) {
        player.sendMessage(Component.text("────────────────────────────────────────────", NamedTextColor.DARK_PURPLE));
        player.sendMessage(Component.text("👁 [SAORI] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .append(Component.text("HEY ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(player.getName() + ": ", NamedTextColor.WHITE))
                .append(Component.text("Si encontraste un fallo o dupe real, repórtalo en privado con ", NamedTextColor.GRAY))
                .append(Component.text("/jack <mensaje>", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" para recompensa.", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Si es una broma, jugar con la economía no es un juego. ", NamedTextColor.GRAY)
                .append(Component.text("Te estoy observando.", NamedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC)));
        player.sendMessage(Component.text("────────────────────────────────────────────", NamedTextColor.DARK_PURPLE));

        try {
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.8f);
        } catch (Exception ignored) {}
    }
}
