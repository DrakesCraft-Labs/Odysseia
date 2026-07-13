package org.metamechanists.odysseia.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.kits.KitClaimService;

/**
 * Provides PlaceholderAPI integration for Odysseia.
 * Exposes %odysseia_owner_prefix% and specific placeholders.
 */
public final class OdysseiaPlaceholderExpansion extends PlaceholderExpansion {

    private final Odysseia plugin;

    public OdysseiaPlaceholderExpansion(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "odysseia";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JackStar";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep registered on PAPI reload
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player != null && params.toLowerCase().startsWith("kit_")) {
            String[] parts = params.split("_", 3);
            if (parts.length == 3) {
                String type = parts[1].toLowerCase();
                String kit = parts[2].toLowerCase();
                var section = plugin.getConfig().getConfigurationSection("kits." + kit);
                if (section == null) return "no";
                Player online = player.getPlayer();
                boolean owned = online != null && online.hasPermission(section.getString("permission", "drakes.kit." + kit));
                KitClaimService.ClaimState state = plugin.getKitClaimService().state(player.getUniqueId(), kit, section.getString("cooldown", "30d"));
                return switch (type) {
                    case "owned" -> owned ? "yes" : "no";
                    case "available" -> owned && state.available() ? "yes" : "no";
                    case "remaining" -> owned ? state.remainingText() : "";
                    default -> null;
                };
            }
        }
        if (params.equalsIgnoreCase("owner_prefix_odiseo")) {
            return plugin.getCurrentOdiseoPrefix();
        }
        if (params.equalsIgnoreCase("owner_prefix_penelope")) {
            return plugin.getCurrentPenelopePrefix();
        }
        if (params.equalsIgnoreCase("owner_prefix")) {
            if (player != null && player.getName() != null) {
                String name = player.getName().toLowerCase();
                if (name.equals("odiseo")) {
                    return plugin.getCurrentOdiseoPrefix();
                } else if (name.equals("penelope")) {
                    return plugin.getCurrentPenelopePrefix();
                }
            }
            return "";
        }
        return null;
    }
}
