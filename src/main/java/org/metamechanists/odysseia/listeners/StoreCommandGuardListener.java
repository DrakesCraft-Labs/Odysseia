package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Keeps the public commerce entry point to a single, documented command. */
public final class StoreCommandGuardListener implements Listener {
    private static final ThreadLocal<UUID> INTERNAL_DISPATCH = new ThreadLocal<>();
    private static final Set<String> LEGACY_STORE_COMMANDS = Set.of(
            "drakestienda", "shop", "mercado", "ushop", "tiendaitems",
            "tiendamateriales", "tmateriales", "tiendaprot", "menu",
            "kitsvip", "kits_vip", "kits-vip", "ultimateshop", "sfmercado"
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isInternalDispatch(event.getPlayer().getUniqueId())
                || event.getPlayer().hasPermission("odysseia.store.command-bypass")
                || !isLegacyStoreCommand(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6DrakesCraft &8· &7La tienda se abre con &e/tienda&7."));
    }

    /** Allows the unified menu to invoke a guarded destination on the same server thread. */
    public static void runInternal(org.bukkit.entity.Player player, Runnable action) {
        runInternal(player.getUniqueId(), action);
    }

    static void runInternal(UUID playerId, Runnable action) {
        UUID previous = INTERNAL_DISPATCH.get();
        INTERNAL_DISPATCH.set(playerId);
        try {
            action.run();
        } finally {
            if (previous == null) {
                INTERNAL_DISPATCH.remove();
            } else {
                INTERNAL_DISPATCH.set(previous);
            }
        }
    }

    static boolean isInternalDispatch(UUID playerId) {
        return playerId.equals(INTERNAL_DISPATCH.get());
    }

    /** Extracts a command label safely, including namespaced forms such as plugin:command. */
    static boolean isLegacyStoreCommand(String message) {
        if (message == null || !message.startsWith("/")) return false;

        String rawLabel = message.substring(1).trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (rawLabel.isBlank()) return false;

        int namespaceSeparator = rawLabel.lastIndexOf(':');
        String label = namespaceSeparator >= 0 ? rawLabel.substring(namespaceSeparator + 1) : rawLabel;
        return LEGACY_STORE_COMMANDS.contains(label);
    }
}
