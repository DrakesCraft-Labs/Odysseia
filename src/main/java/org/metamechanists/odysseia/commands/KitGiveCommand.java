package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.purchase.ActionResult;
import org.metamechanists.odysseia.purchase.KitDeliveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Entrega únicamente los ítems de un kit para pruebas administrativas seguras. */
public final class KitGiveCommand implements CommandExecutor, TabCompleter {
    private final Odysseia plugin;
    private final KitDeliveryService kits;

    public KitGiveCommand(Odysseia plugin) {
        this.plugin = plugin;
        this.kits = new KitDeliveryService(plugin);
    }

    public List<String> validateConfiguration() {
        return kits.validateConfiguration();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(color("&eKits: &f" + String.join(", ", kitNames())));
            return true;
        }
        Player target;
        String kit;
        if (args.length == 1 && sender instanceof Player player) {
            target = player;
            kit = args[0];
        } else if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[0]);
            kit = args[1];
        } else {
            sender.sendMessage(color("&eUso: &f/kitgive <kit> &7o &f/kitgive <jugador> <kit>"));
            return true;
        }
        if (target == null) {
            sender.sendMessage(color("&cEl jugador debe estar conectado."));
            return true;
        }
        kit = kit.toLowerCase(Locale.ROOT);
        if (!kitNames().contains(kit)) {
            sender.sendMessage(color("&cKit desconocido. Usa /kitgive list."));
            return true;
        }
        ActionResult result = kits.deliver(target, kit, "kit-test-" + UUID.randomUUID());
        if (result.status() == ActionResult.Status.COMPLETED) {
            sender.sendMessage(color("&aKit &f" + kit + " &aentregado a &f" + target.getName() + "&a. Solo ítems; sin dinero, rango ni protección."));
            if (!sender.equals(target)) target.sendMessage(color("&eRecibiste el kit de prueba &f" + kit + "&e."));
        } else {
            sender.sendMessage(color("&cNo se entregó: " + result.detail()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> values = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            values.addAll(kitNames());
            values.add("list");
            return filter(values, args[0]);
        }
        if (args.length == 2) return filter(kitNames(), args[1]);
        return List.of();
    }

    private List<String> kitNames() {
        var section = plugin.getConfig().getConfigurationSection("kits");
        return section == null ? List.of() : section.getKeys(false).stream().sorted().toList();
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).sorted().toList();
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
