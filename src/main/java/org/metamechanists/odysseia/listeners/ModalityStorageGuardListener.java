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

import java.util.ArrayList;
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
    /** Modalidades cuyo inventario aislado tambien exige bloquear almacenes globales. */
    private final Set<String> isolatedModalities = new HashSet<>();
    /** Patrones bloqueados ya tokenizados; cada uno se compara como prefijo del comando escrito. */
    private final List<List<String>> blockedPatterns = new ArrayList<>();
    private boolean enabled;

    public ModalityStorageGuardListener(JavaPlugin plugin, ModalityService modalities, ModalityVaultService vaults) {
        this.plugin = plugin;
        this.modalities = modalities;
        this.vaults = vaults;
        reload();
    }

    public void reload() {
        vaultCommands.clear();
        isolatedModalities.clear();
        blockedPatterns.clear();
        enabled = plugin.getConfig().getBoolean("modalidades.guard.enabled", true);
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-boveda"))) vaultCommands.add(value);
        isolatedModalities.addAll(lower(plugin.getConfig().getStringList("modalidades.guard.modalidades-aisladas")));
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-bloqueados"))) {
            List<String> pattern = tokens(value);
            if (!pattern.isEmpty()) blockedPatterns.add(pattern);
        }
    }

    private static List<String> lower(List<String> values) {
        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }

    /**
     * Parte un comando o patron en tokens normalizados. El primero pasa por {@link #label} para
     * tolerar la forma plugin:comando; el resto son subcomandos ("team echest").
     */
    static List<String> tokens(String raw) {
        List<String> result = new ArrayList<>();
        for (String part : raw.trim().split("\\s+")) {
            if (part.isBlank()) continue;
            result.add(result.isEmpty() ? label(part) : part.toLowerCase(Locale.ROOT));
        }
        result.removeIf(String::isBlank);
        return result;
    }

    /**
     * Mensaje del bloqueo. Se busca el mas especifico en "modalidades.guard.mensajes" usando el
     * patron como clave con guiones ("ps-get"); si no hay, se usa el generico de almacenamiento.
     *
     * Existe porque no todos los bloqueos son de almacenamiento: a un jugador que intento comprar
     * una proteccion dentro de su isla decirle "aqui usa /pv" no le explica nada.
     */
    private String mensaje(List<String> written, Player player) {
        var section = plugin.getConfig().getConfigurationSection("modalidades.guard.mensajes");
        if (section != null) {
            for (int size = Math.min(written.size(), 3); size >= 1; size--) {
                String key = String.join("-", written.subList(0, size));
                String texto = section.getString(key);
                if (texto != null && !texto.isBlank()) return texto;
            }
        }
        return "&6DrakesCraft &8· &7Ese almacenamiento es del &eSurvival&7 y no cruza modalidades. "
                + "Aqui tienes &e/pv&7, exclusivo de &e" + modalities.resolve(player).displayName() + "&7.";
    }

    /** True si el comando escrito empieza con alguno de los patrones bloqueados. */
    static boolean matches(List<List<String>> patterns, List<String> written) {
        for (List<String> pattern : patterns) {
            if (pattern.size() > written.size()) continue;
            if (written.subList(0, pattern.size()).equals(pattern)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (intercept(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
        }
    }

    /**
     * Applies the modality boundary to commands launched by menus as well as commands typed in
     * chat. Bukkit.dispatchCommand does not fire PlayerCommandPreprocessEvent, so every native
     * menu must pass its player actions through this method before dispatching them.
     *
     * @return true when the command was consumed and must not be dispatched
     */
    public boolean intercept(Player player, String rawCommand) {
        if (!enabled || player.hasPermission(BYPASS)
                || !isIsolatedModality(isolatedModalities, modalities.resolve(player).id())) return false;

        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isBlank()) return false;

        String[] parts = command.split("\\s+");
        List<String> written = tokens(String.join(" ", parts));
        if (written.isEmpty()) return false;

        if (vaultCommands.contains(written.get(0))) {
            int vault = 1;
            if (parts.length > 1) {
                try {
                    vault = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // sin numero valido abrimos la primera
                }
            }
            vaults.open(player, vault);
            return true;
        }

        if (matches(blockedPatterns, written)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensaje(written, player)));
            return true;
        }
        return false;
    }

    /** Extrae la etiqueta del comando, tolerando la forma plugin:comando. */
    static String label(String raw) {
        String value = raw.toLowerCase(Locale.ROOT);
        int separator = value.lastIndexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    /** Evita tratar como isla cualquier modalidad nueva que simplemente no sea la base. */
    static boolean isIsolatedModality(Set<String> isolated, String modalityId) {
        return modalityId != null && isolated.contains(modalityId.toLowerCase(Locale.ROOT));
    }
}
