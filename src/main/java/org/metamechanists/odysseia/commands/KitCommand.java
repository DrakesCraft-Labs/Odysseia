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

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.metamechanists.odysseia.kits.KitClaimService;

/** Punto único de reclamación pública para kits configurados por Odysseia. */
public final class KitCommand implements CommandExecutor, TabCompleter {
    private final Odysseia plugin;
    private final KitDeliveryService delivery;
    private final KitClaimService claims;

    public KitCommand(Odysseia plugin) {
        this.plugin = plugin;
        this.delivery = new KitDeliveryService(plugin);
        this.claims = plugin.getKitClaimService();
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
            List<String> availableKits = kitNames(player);
            player.sendMessage(color(availableKits.isEmpty()
                    ? "&7No tienes kits disponibles con tu rango actual."
                    : "&7Disponibles: &f" + String.join(", ", availableKits)));
            return true;
        }

        String kit = args[0].toLowerCase(Locale.ROOT);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("kits." + kit);
        if (section == null) {
            player.sendMessage(color("&cEse kit no existe."));
            return true;
        }
        String permission = section.getString("permission", "").trim();
        if (permission.isEmpty() || !player.hasPermission(permission)) {
            player.sendMessage(color("&cNo tienes permiso para reclamar este kit."));
            return true;
        }

        KitClaimService.ClaimState state = claims.state(player.getUniqueId(), kit, section.getString("cooldown", "30d"));
        if (!state.available()) {
            player.sendMessage(color("&eEste kit ya fue reclamado. Disponible en &f" + state.remainingText()));
            return true;
        }

        ActionResult result = delivery.deliver(player, kit, "public-kit-" + UUID.randomUUID());
        if (result.status() != ActionResult.Status.COMPLETED) {
            player.sendMessage(color("&cNo se pudo entregar: &f" + result.detail()));
            return true;
        }
        claims.record(player.getUniqueId(), kit);
        String cooldown = section.getString("cooldown", "30d");
        String claimMessage = KitClaimService.parseDuration(cooldown) < 0L
                ? "&aKit &f" + kit + " &aentregado. Es una reclamación única."
                : "&aKit &f" + kit + " &aentregado. Podrás reclamarlo nuevamente en &f" + cooldown + "&a.";
        player.sendMessage(color(claimMessage));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        if (!(sender instanceof Player player)) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return kitNames(player).stream().filter(name -> name.startsWith(prefix)).toList();
    }

    /** Solo revela kits cuyo permiso explícito el jugador ya posee por LuckPerms. */
    private List<String> kitNames(Player player) {
        ConfigurationSection kits = plugin.getConfig().getConfigurationSection("kits");
        if (kits == null) return List.of();
        return kits.getKeys(false).stream()
                .filter(name -> {
                    ConfigurationSection kit = kits.getConfigurationSection(name);
                    String permission = kit == null ? "" : kit.getString("permission", "").trim();
                    return !permission.isEmpty() && player.hasPermission(permission);
                })
                .sorted()
                .toList();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
