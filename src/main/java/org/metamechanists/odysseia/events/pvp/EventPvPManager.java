package org.metamechanists.odysseia.events.pvp;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.utils.WebhookSender;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestiona el sistema de eventos PvP con seguridad absoluta de inventario (cero pérdidas),
 * selección de kits balanceados y entrega de rangos temporales en LuckPerms.
 */
public class EventPvPManager {

    private final JavaPlugin plugin;
    private final String webhookUrl;
    @Getter private final EventPvPStashRepository stashRepository;
    @Getter private boolean active = false;

    // UUID -> Kit seleccionado
    private final Map<UUID, PvPKit> participants = new ConcurrentHashMap<>();
    // UUID -> Kills/Puntos
    private final Map<UUID, Integer> killsMap = new ConcurrentHashMap<>();
    @Getter @Setter private Location arenaSpawn;

    public EventPvPManager(JavaPlugin plugin, String webhookUrl) {
        this.plugin = plugin;
        this.webhookUrl = webhookUrl;
        EventPvPStashRepository repo = null;
        try {
            File dbFile = new File(plugin.getDataFolder(), "events_pvp_stash.db");
            repo = new EventPvPStashRepository(dbFile, plugin.getLogger());
            // Reconciliar stashes pendientes por caídas previas del servidor
            List<UUID> pending = repo.getAllStashedPlayers();
            if (!pending.isEmpty()) {
                plugin.getLogger().warning("[EventPvP] Se encontraron " + pending.size() + " jugadores con inventario en consigna PvP por reinicio previo.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "[EventPvP] No se pudo inicializar la base de datos de consigna PvP", e);
        }
        this.stashRepository = repo;
    }

    public void startTournament(Location arenaSpawn) {
        this.arenaSpawn = arenaSpawn;
        this.active = true;
        this.participants.clear();
        this.killsMap.clear();

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&c⚔ &4&lTORNEO PVP INICIADO &8» &f¡Se ha abierto el torneo gladiador!\n" +
                "  &7• Tu inventario real queda &aguardado al 100% de forma segura&7.\n" +
                "  &7• Únete con: &e/evento pvp join <gladiador|tanque|arquero|berserker>\n" +
                "  &7• ¡Premios en rangos temporales para los campeones!\n"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
        }

        sendDiscordStartWebhook();
    }

    public boolean joinTournament(Player player, PvPKit kit) {
        if (!active) {
            player.sendMessage(ChatColor.RED + "No hay un torneo PvP activo actualmente.");
            return false;
        }
        if (stashRepository == null) {
            player.sendMessage(ChatColor.RED + "Error en base de datos de seguridad. No es seguro entrar.");
            return false;
        }
        if (participants.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Ya estás participando en el torneo. Usa /evento pvp leave para salir.");
            return false;
        }

        // 1. Guardar inventario real en SQLite (PRAGMA synchronous=FULL)
        boolean stashed = stashRepository.stashPlayer(player);
        if (!stashed) {
            player.sendMessage(ChatColor.RED + "Error crítico asegurando tu inventario. Operación cancelada.");
            return false;
        }

        // 2. Aplicar kit seleccionado
        kit.apply(player);
        participants.put(player.getUniqueId(), kit);
        killsMap.putIfAbsent(player.getUniqueId(), 0);

        // 3. Teletransportar a la arena si existe
        if (arenaSpawn != null) {
            player.teleport(arenaSpawn);
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a✔ &f¡Te has unido al torneo con el kit &e" + kit.getDisplayName() + "&f! Tu inventario ha sido guardado de forma segura."));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
        return true;
    }

    public boolean leaveTournament(Player player) {
        if (!participants.containsKey(player.getUniqueId())) {
            // Verificar si tiene stash huérfano para devolverlo
            if (stashRepository != null && stashRepository.hasStash(player.getUniqueId())) {
                boolean restored = stashRepository.restorePlayer(player);
                if (restored) {
                    player.sendMessage(ChatColor.GREEN + "Tu inventario original ha sido restaurado con éxito.");
                }
                return restored;
            }
            player.sendMessage(ChatColor.RED + "No estás participando en el torneo PvP.");
            return false;
        }

        participants.remove(player.getUniqueId());
        boolean restored = stashRepository.restorePlayer(player);
        if (restored) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a✔ &fHas abandonado el torneo. Tu inventario original ha sido restaurado al 100%."));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else {
            player.sendMessage(ChatColor.RED + "Error restaurando tu inventario. Contacta de inmediato a un administrador.");
        }
        return restored;
    }

    public void handleKill(Player killer, Player victim) {
        if (!participants.containsKey(victim.getUniqueId())) return;

        // Victim pierde y su inventario se restaura
        leaveTournament(victim);
        victim.sendMessage(ChatColor.RED + "Has sido eliminado del torneo. Tu inventario real fue restaurado.");

        if (killer != null && participants.containsKey(killer.getUniqueId())) {
            int currentKills = killsMap.merge(killer.getUniqueId(), 1, Integer::sum);
            killer.sendMessage(ChatColor.GREEN + "¡Has eliminado a " + victim.getName() + "! (Bajas: " + currentKills + ")");
            killer.playSound(killer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c⚔ &e" + killer.getName() + " &7eliminó a &c" + victim.getName() + " &7en el Torneo PvP!"));
        }
    }

    public void awardTemporaryRank(Player winner, String rank, int days) {
        if (winner == null || rank == null || rank.isBlank() || days <= 0) return;

        String cmd = String.format(Locale.ROOT, "lp user %s parent addtemp %s %dd", winner.getName(), rank, days);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "\n&6👑 &e&lCAMPEÓN DEL TORNEO PVP &8» &a¡Felicitaciones a &f" + winner.getName() + "&a!\n" +
                "  &7• Ha recibido el rango temporal &6&l" + rank.toUpperCase(Locale.ROOT) + " &7por &b" + days + " días&7.\n" +
                "  &7• ¡Un aplauso para el nuevo campeón de DrakesCraft!\n"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        sendDiscordWinnerWebhook(winner.getName(), rank, days);
    }

    public void stopTournament() {
        if (!active) return;
        this.active = false;

        // Devolver inventarios a todos los que sigan en el torneo
        for (UUID uuid : new ArrayList<>(participants.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                leaveTournament(p);
            }
        }
        participants.clear();

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                "&c⚔ &4&lTORNEO PVP FINALIZADO &8» &7El evento de combate ha concluido."));
    }

    private void sendDiscordStartWebhook() {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String json = """
        {
          "content": "<@&1539644230941806602>",
          "embeds": [{
            "title": "⚔️ ¡TORNEO PVP ACTIVADO EN DRAKES! ⚔️",
            "description": "**¡El coliseo abre sus puertas para los más valientes!**\\n\\n• **Protección:** 🛡️ 100%% Cero Pérdidas (tu inventario se guarda y restaura automáticamente).\\n• **Kits Disponibles:** Gladiador, Tanque, Arquero, Berserker.\\n• **Premio:** Rangos temporales exclusivos (LuckPerms).\\n\\nÚnete usando `/evento pvp join <kit>` en `mc.drakescraft.net`!",
            "color": 13380630,
            "footer": { "text": "DrakesCraft Event Suite • PvP System" },
            "timestamp": "%s"
          }]
        }
        """.formatted(java.time.Instant.now().toString());
        WebhookSender.sendAsync(plugin, webhookUrl, json);
    }

    private void sendDiscordWinnerWebhook(String winnerName, String rank, int days) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        String json = """
        {
          "content": "<@&1539644230941806602>",
          "embeds": [{
            "title": "👑 ¡TENEMOS UN NUEVO CAMPEÓN PVP! 👑",
            "description": "**¡El combate en la arena ha concluido!**\\n\\n• **Ganador:** **%s**\\n• **Rango Ganado:** **%s** (durante %d días)\\n\\n¡Felicitaciones a nuestro guerrero por su victoria en el torneo!",
            "color": 16766720,
            "footer": { "text": "DrakesCraft Event Suite • PvP System" },
            "timestamp": "%s"
          }]
        }
        """.formatted(winnerName, rank.toUpperCase(Locale.ROOT), days, java.time.Instant.now().toString());
        WebhookSender.sendAsync(plugin, webhookUrl, json);
    }

    public boolean isParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public void shutdown() {
        stopTournament();
        if (stashRepository != null) {
            stashRepository.close();
        }
    }
}
