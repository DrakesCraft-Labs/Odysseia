package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.modalities.ModalityService;
import org.metamechanists.odysseia.vaults.ModalityVaultService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cierra los puentes de items entre modalidades.
 *
 * InvSwitcher separa el inventario, el cofre de ender y la experiencia por mundo, pero los
 * plugins de almacenamiento guardan en bases globales y permiten sacar en skyblock lo que se
 * guardo en survival. Aqui:
 *   - /pv se redirige a las bovedas por modalidad de Odysseia cuando el jugador esta en una isla
 *   - el resto de almacenamientos globales se bloquea dentro de las islas
 */
public final class ModalityStorageGuardListener implements Listener {

    private static final String BYPASS = "odysseia.modalidades.storage-bypass";

    private final JavaPlugin plugin;
    private final ModalityService modalities;
    private final ModalityVaultService vaults;
    private final Set<String> vaultCommands = new HashSet<>();
    private final Set<String> blockedCommands = new HashSet<>();
    private boolean enabled;

    public ModalityStorageGuardListener(JavaPlugin plugin, ModalityService modalities, ModalityVaultService vaults) {
        this.plugin = plugin;
        this.modalities = modalities;
        this.vaults = vaults;
        reload();
    }

    public void reload() {
        vaultCommands.clear();
        blockedCommands.clear();
        enabled = plugin.getConfig().getBoolean("modalidades.guard.enabled", true);
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-boveda"))) vaultCommands.add(value);
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-bloqueados"))) blockedCommands.add(value);
    }

    private static List<String> lower(List<String> values) {
        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS) || !modalities.isIsland(player)) return;

        String[] parts = event.getMessage().substring(1).trim().split("\\s+");
        String label = label(parts[0]);
        if (label.isBlank()) return;

        if (vaultCommands.contains(label)) {
            event.setCancelled(true);
            int vault = 1;
            if (parts.length > 1) {
                try {
                    vault = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // sin numero valido abrimos la primera
                }
            }
            vaults.open(player, vault);
            return;
        }

        if (blockedCommands.contains(label)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &7Ese almacenamiento es del &eSurvival&7. "
                            + "Aqui usa &e/pv&7, que es exclusivo de esta modalidad."));
        }
    }

    /** Extrae la etiqueta del comando, tolerando la forma plugin:comando. */
    static String label(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        int separator = value.lastIndexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }
}
