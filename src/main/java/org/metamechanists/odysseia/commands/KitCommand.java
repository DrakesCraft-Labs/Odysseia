package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.purchase.ActionResult;
import org.metamechanists.odysseia.purchase.KitDeliveryService;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Punto único de reclamación pública para kits configurados por Odysseia. */
public final class KitCommand implements CommandExecutor, TabCompleter {
    private final Odysseia plugin;
    private final KitDeliveryService delivery;
    private final File claimsFile;
    private final FileConfiguration claims;

    public KitCommand(Odysseia plugin) {
        this.plugin = plugin;
        this.delivery = new KitDeliveryService(plugin);
        this.claimsFile = new File(plugin.getDataFolder(), "kit-claims.yml");
        this.claims = YamlConfiguration.loadConfiguration(claimsFile);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede reclamar kits.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(color("&eUso: &f/kit <nombre>"));
            player.sendMessage(color("&7Disponibles: &f" + String.join(", ", kitNames())));
            return true;
        }

        String kit = args[0].toLowerCase(Locale.ROOT);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits." + kit);
        if (section == null) {
            player.sendMessage(color("&cEse kit no existe."));
            return true;
        }
        String permission = section.getString("permission", "drakes.kit." + kit);
        if (!player.hasPermission(permission)) {
            player.sendMessage(color("&cNo tienes permiso para reclamar este kit."));
            return true;
        }

        long cooldown = parseCooldown(section.getString("cooldown", "30d"));
        String claimKey = player.getUniqueId() + "." + kit;
        long lastClaim = claims.getLong(claimKey, 0L);
        long remaining = cooldown < 0 ? (lastClaim > 0 ? 1L : 0L) : lastClaim + cooldown - System.currentTimeMillis();
        if (remaining > 0) {
            player.sendMessage(color("&eEste kit ya fue reclamado. Disponible en &f" + formatRemaining(remaining)));
            return true;
        }

        ActionResult result = delivery.deliver(player, kit, "public-kit-" + UUID.randomUUID());
        if (result.status() != ActionResult.Status.COMPLETED) {
            player.sendMessage(color("&cNo se pudo entregar: &f" + result.detail()));
            return true;
        }
        claims.set(claimKey, System.currentTimeMillis());
        saveClaims();
        player.sendMessage(color("&aKit &f" + kit + " &aentregado. Podrás reclamarlo nuevamente en 30 días."));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return kitNames().stream().filter(name -> name.startsWith(prefix)).toList();
    }

    private List<String> kitNames() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    private long parseCooldown(String value) {
        if (value == null || value.equals("-1")) return -1L;
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            return switch (value.charAt(value.length() - 1)) {
                case 'm' -> amount * 60_000L;
                case 'h' -> amount * 3_600_000L;
                case 'd' -> amount * 86_400_000L;
                default -> 30L * 86_400_000L;
            };
        } catch (RuntimeException ignored) {
            return 30L * 86_400_000L;
        }
    }

    private String formatRemaining(long millis) {
        long days = millis / 86_400_000L;
        long hours = (millis % 86_400_000L) / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        return days + "d " + hours + "h " + minutes + "m";
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void saveClaims() {
        try {
            claims.save(claimsFile);
        } catch (IOException error) {
            plugin.getLogger().severe("[Kits] No se pudo guardar kit-claims.yml: " + error.getMessage());
        }
    }
}
