package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.modalities.ModalityService;
import org.metamechanists.odysseia.vaults.ModalityVaultService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final String GAMEPLAY_BYPASS = "odysseia.modalidades.gameplay-bypass";

    private final JavaPlugin plugin;
    private final ModalityService modalities;
    private final ModalityVaultService vaults;
    private final Set<String> vaultCommands = new HashSet<>();
    /** Modalidades cuyo inventario aislado tambien exige bloquear almacenes globales. */
    private final Set<String> isolatedModalities = new HashSet<>();
    /** Mundos exactos donde se permite abrir el PlayerVaultZ historico de Survival. */
    private final Set<String> globalVaultWorlds = new HashSet<>();
    /**
     * Modalidades que deniegan por defecto: solo corre lo que este en su lista blanca.
     *
     * El servidor declara 373 comandos con 295 alias entre ellos, y cada plugin nuevo añade los
     * suyos. Mantener una lista negra ahi es perder por cansancio: basta que se escape un alias
     * --/ah tiene cuatro, /balance seis-- para abrir una via de fuga. En el laboratorio, donde
     * cualquiera puede invocar objetos con /sf cheat, se invierte la regla: se prohibe todo y se
     * permite lo justo. Un plugin instalado mañana queda bloqueado ahi sin tocar nada.
     */
    private final Set<String> whitelistModalities = new HashSet<>();
    /** Lo unico que se deja pasar en cada modalidad de lista blanca. */
    private final Map<String, List<List<String>>> whitelistPatterns = new HashMap<>();
    /** Patrones bloqueados ya tokenizados; cada uno se compara como prefijo del comando escrito. */
    private final List<List<String>> blockedPatterns = new ArrayList<>();
    /** Rutas que nunca pueden puentear una modalidad, ni siquiera mediante bypass de staff. */
    private final List<List<String>> strictBlockedPatterns = new ArrayList<>();
    /** Restricciones de jugabilidad por modalidad, independientes del aislamiento de items. */
    private final Map<String, List<List<String>>> gameplayPatterns = new HashMap<>();
    /** Excepciones concretas a los patrones globales bloqueados, por modalidad. */
    private final Map<String, List<List<String>>> allowedStoragePatterns = new HashMap<>();
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
        globalVaultWorlds.clear();
        whitelistModalities.clear();
        whitelistPatterns.clear();
        blockedPatterns.clear();
        strictBlockedPatterns.clear();
        gameplayPatterns.clear();
        allowedStoragePatterns.clear();
        enabled = plugin.getConfig().getBoolean("modalidades.guard.enabled", true);
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-boveda"))) vaultCommands.add(value);
        isolatedModalities.addAll(lower(plugin.getConfig().getStringList("modalidades.guard.modalidades-aisladas")));
        globalVaultWorlds.addAll(lower(plugin.getConfig().getStringList("modalidades.guard.mundos-boveda-global")));
        whitelistModalities.addAll(lower(plugin.getConfig().getStringList("modalidades.guard.modalidades-lista-blanca")));
        var whitelist = plugin.getConfig().getConfigurationSection("modalidades.guard.comandos-lista-blanca");
        if (whitelist != null) {
            for (String modality : whitelist.getKeys(false)) {
                List<List<String>> patterns = new ArrayList<>();
                for (String value : lower(whitelist.getStringList(modality))) {
                    List<String> pattern = tokens(value);
                    if (!pattern.isEmpty()) patterns.add(pattern);
                }
                whitelistPatterns.put(modality.toLowerCase(Locale.ROOT), patterns);
            }
        }
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-bloqueados"))) {
            List<String> pattern = tokens(value);
            if (!pattern.isEmpty()) blockedPatterns.add(pattern);
        }
        for (String value : lower(plugin.getConfig().getStringList("modalidades.guard.comandos-bloqueados-siempre"))) {
            List<String> pattern = tokens(value);
            if (!pattern.isEmpty()) strictBlockedPatterns.add(pattern);
        }
        var restrictions = plugin.getConfig().getConfigurationSection("modalidades.guard.comandos-restringidos");
        if (restrictions != null) {
            for (String modality : restrictions.getKeys(false)) {
                List<List<String>> patterns = new ArrayList<>();
                for (String value : lower(restrictions.getStringList(modality))) {
                    List<String> pattern = tokens(value);
                    if (!pattern.isEmpty()) patterns.add(pattern);
                }
                gameplayPatterns.put(modality.toLowerCase(Locale.ROOT), patterns);
            }
        }
        var allowed = plugin.getConfig().getConfigurationSection("modalidades.guard.comandos-permitidos");
        if (allowed != null) {
            for (String modality : allowed.getKeys(false)) {
                List<List<String>> patterns = new ArrayList<>();
                for (String value : lower(allowed.getStringList(modality))) {
                    List<String> pattern = tokens(value);
                    if (!pattern.isEmpty()) patterns.add(pattern);
                }
                allowedStoragePatterns.put(modality.toLowerCase(Locale.ROOT), patterns);
            }
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

    /**
     * Allowlist matching is deliberately stricter than denylist matching. A one-token entry such
     * as "sf" permits only /sf itself; it must never implicitly authorize /sf give or a future
     * administrative subcommand added by Slimefun.
     */
    static boolean matchesAllowlist(List<List<String>> patterns, List<String> written) {
        for (List<String> pattern : patterns) {
            if (pattern.size() > written.size()) continue;
            if (pattern.size() == 1 && written.size() != 1) continue;
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

    /** Hides commands which the laboratory would reject, reducing accidental and probing paths. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandList(PlayerCommandSendEvent event) {
        if (!enabled || playerBypassesLaboratory(event.getPlayer())) return;
        String modalityId = modalities.resolve(event.getPlayer()).id().toLowerCase(Locale.ROOT);
        if (!whitelistModalities.contains(modalityId)) return;
        List<List<String>> allowed = whitelistPatterns.getOrDefault(modalityId, List.of());
        event.getCommands().removeIf(command -> !matchesAllowlist(allowed, tokens(command)));
    }

    /**
     * Ultima barrera contra accesos directos por API, menus o aliases que no disparen el evento
     * de comando. PlayerVaultZ solo puede abrirse en los tres mundos Survival autorizados.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVaultOpen(InventoryOpenEvent event) {
        if (!enabled || !(event.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null || !holder.getClass().getName().equals("com.rugzy.playervaultz.ui.VaultGUI")) return;
        if (globalVaultWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6DrakesCraft &8· &7Ese /pv pertenece a &eSurvival&7. "
                        + "Abre la boveda propia de tu modalidad desde un mundo de juego."));
        plugin.getLogger().warning("[Bovedas] PlayerVaultZ global bloqueado fuera de Survival para "
                + player.getName() + " en " + player.getWorld().getName());
    }

    /**
     * Applies the modality boundary to commands launched by menus as well as commands typed in
     * chat. Bukkit.dispatchCommand does not fire PlayerCommandPreprocessEvent, so every native
     * menu must pass its player actions through this method before dispatching them.
     *
     * @return true when the command was consumed and must not be dispatched
     */
    public boolean intercept(Player player, String rawCommand) {
        if (!enabled) return false;

        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isBlank()) return false;

        String[] parts = command.split("\\s+");
        List<String> written = tokens(String.join(" ", parts));
        if (written.isEmpty()) return false;

        String modalityId = modalities.resolve(player).id().toLowerCase(Locale.ROOT);

        // Denegar por defecto. Va antes que el resto de comprobaciones a proposito: si la
        // modalidad es de lista blanca, nada de lo que venga despues puede reabrir un comando.
        // El bypass de staff sigue valiendo, para poder administrar dentro del laboratorio.
        if (whitelistModalities.contains(modalityId) && !playerBypassesLaboratory(player)) {
            if (!matchesAllowlist(whitelistPatterns.getOrDefault(modalityId, List.of()), written)) {
                String message = plugin.getConfig().getString(
                        "modalidades.guard.mensaje-lista-blanca",
                        "&6DrakesCraft &8· &7En el laboratorio solo funcionan &e/sf&7, &e/modalidades&7, "
                                + "&e/spawn&7 y &e/tpa&7. Nada de aqui sale a las demas modalidades.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return true;
            }
            return false;
        }

        List<List<String>> gameplay = gameplayPatterns.getOrDefault(modalityId, List.of());
        if (!player.hasPermission(GAMEPLAY_BYPASS) && matches(gameplay, written)) {
            String message = plugin.getConfig().getString(
                    "modalidades.guard.mensaje-restringido",
                    "&6DrakesCraft &8· &7Ese comando no forma parte de esta modalidad.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }

        boolean isolated = isIsolatedModality(isolatedModalities, modalityId);
        if (vaultCommands.contains(written.get(0)) && isolated) {
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

        // Los mundos auxiliares caen en el fallback Survival, pero no deben poder abrir la base
        // global: SpawnWarps, arenas, limbo o futuros mundos serian un puente entre inventarios.
        if (vaultCommands.contains(written.get(0))
                && !globalVaultWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &7/pv solo se abre dentro del mundo de tu modalidad."));
            return true;
        }

        // Estas herramientas operan directamente sobre la base global de PlayerVaultZ. Un bypass
        // accidental desde una isla puede copiar items entre modalidades, por eso no admite staff.
        if (isolated && matches(strictBlockedPatterns, written)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &7La administracion de PlayerVault solo se ejecuta en Survival."));
            return true;
        }

        if (player.hasPermission(BYPASS) || !isolated) return false;

        List<List<String>> allowed = allowedStoragePatterns.getOrDefault(modalityId, List.of());
        if (matches(blockedPatterns, written) && !matches(allowed, written)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', mensaje(written, player)));
            return true;
        }
        return false;
    }

    private static boolean playerBypassesLaboratory(Player player) {
        return player.hasPermission(GAMEPLAY_BYPASS);
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
