package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.metamechanists.odysseia.modalities.Modality;
import org.metamechanists.odysseia.modalities.ModalityService;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla y aísla el comando /back por modalidad de juego.
 *
 * <p>Garantiza que el comando /back únicamente retorne al jugador a una posición
 * previa registrada dentro de su modalidad actual (ej. Clásico, Survival, OneBlock, SkyBlock),
 * impidiendo por completo el teletransporte intermodal.</p>
 */
public final class ModalityBackGuardListener implements Listener {

    private static final Set<String> BACK_COMMANDS = Set.of(
            "back", "eback", "return", "ereturn"
    );

    private final Plugin plugin;
    private final ModalityService modalityService;
    private final Map<UUID, Map<String, Location>> modalityLastLocations = new ConcurrentHashMap<>();

    public ModalityBackGuardListener(Plugin plugin, ModalityService modalityService) {
        this.plugin = plugin;
        this.modalityService = modalityService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from == null || from.getWorld() == null || to == null || to.getWorld() == null) {
            return;
        }

        // Ignorar micro-movimientos en el mismo bloque
        if (from.getWorld().equals(to.getWorld())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Modality fromModality = modalityService.resolve(from.getWorld().getName());
        recordLocation(player.getUniqueId(), fromModality.id(), from);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();
        if (deathLoc == null || deathLoc.getWorld() == null) {
            return;
        }

        Modality deathModality = modalityService.resolve(deathLoc.getWorld().getName());
        recordLocation(player.getUniqueId(), deathModality.id(), deathLoc);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String rawMessage = event.getMessage();
        if (rawMessage == null || rawMessage.isBlank()) {
            return;
        }

        if (!isBackCommand(rawMessage)) {
            return;
        }

        Player player = event.getPlayer();

        // Si tiene argumentos adicionales y permiso essentials.back.others, delegar
        String[] parts = rawMessage.trim().split("\\s+");
        if (parts.length > 1 && player.hasPermission("essentials.back.others")) {
            return;
        }

        // Comprobar permisos
        if (!player.hasPermission("essentials.back") && !player.hasPermission("odysseia.back")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para utilizar /back.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (player.getWorld() == null) {
            return;
        }

        Modality currentModality = modalityService.resolve(player.getWorld().getName());
        Map<String, Location> playerLocations = modalityLastLocations.get(player.getUniqueId());
        Location targetLocation = (playerLocations != null) ? playerLocations.get(currentModality.id()) : null;

        if (targetLocation == null || targetLocation.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "No tienes una posición previa registrada en "
                    + ChatColor.translateAlternateColorCodes('&', currentModality.displayName()) + ChatColor.RED + ".");
            return;
        }

        // Validar estrictamente que el destino pertenezca a la misma modalidad
        Modality targetModality = modalityService.resolve(targetLocation.getWorld().getName());
        if (!targetModality.id().equalsIgnoreCase(currentModality.id())) {
            player.sendMessage(ChatColor.RED + "La posición previa registrada no pertenece a esta modalidad.");
            return;
        }

        Location previousLoc = player.getLocation();
        player.teleportAsync(targetLocation).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                // Registrar la posición anterior para permitir volver a alternar
                recordLocation(player.getUniqueId(), currentModality.id(), previousLoc);
                player.sendMessage(ChatColor.GREEN + "Teletransportado a tu última posición en "
                        + ChatColor.translateAlternateColorCodes('&', currentModality.displayName()) + ChatColor.GREEN + ".");
            } else {
                player.sendMessage(ChatColor.RED + "No fue posible teletransportarte a tu posición previa.");
            }
        });
    }

    public void recordLocation(UUID playerId, String modalityId, Location location) {
        if (playerId == null || modalityId == null || location == null) {
            return;
        }
        modalityLastLocations.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(modalityId.toLowerCase(Locale.ROOT), location.clone());
    }

    public Location getLastLocation(UUID playerId, String modalityId) {
        Map<String, Location> map = modalityLastLocations.get(playerId);
        if (map == null || modalityId == null) return null;
        return map.get(modalityId.toLowerCase(Locale.ROOT));
    }

    public static boolean isBackCommand(String message) {
        if (message == null) return false;
        String trimmed = message.trim();
        if (!trimmed.startsWith("/")) return false;
        trimmed = trimmed.substring(1);
        int spaceIndex = trimmed.indexOf(' ');
        String command = (spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex)).toLowerCase(Locale.ROOT);
        int colonIndex = command.indexOf(':');
        if (colonIndex != -1 && colonIndex < command.length() - 1) {
            command = command.substring(colonIndex + 1);
        }
        return BACK_COMMANDS.contains(command);
    }
}
