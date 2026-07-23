package org.metamechanists.odysseia.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

public final class EsteUsuarioEsViejoCommand implements CommandExecutor, TabCompleter {

    private final Odysseia plugin;
    private final KitDeliveryService kits;

    public EsteUsuarioEsViejoCommand(Odysseia plugin) {
        this.plugin = plugin;
        this.kits = new KitDeliveryService(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.admin.oldschool") && !sender.hasPermission("drakes.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNo tienes permisos para ejecutar este comando staff."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eUso: &f/esteusuarioesviejo <jugador>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        // 1. Assign LuckPerms group oldschool and permission
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetName + " parent add oldschool");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + targetName + " permission set drakes.kit.oldschool true");

        // 2. If online, deliver kit and send announcement
        if (target != null && target.isOnline()) {
            ActionResult result = kits.deliver(target, "oldschool", "STAFF_GRANT_" + System.currentTimeMillis());
            if (result.status() == ActionResult.Status.COMPLETED) {
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&7&lOLDSCHOOL&8] &a¡Bienvenido a la Leyenda de Veteranos 5+ Años! Se te ha otorgado el Rango y Kit Oldschool."));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Odysseia] &e¡El jugador &b" + target.getName() + " &eahora es &7&lOldschool &e(Rango, permisos y kit entregados)!"));
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Odysseia] &e¡Rango y permisos otorgados a &b" + target.getName() + "&e! (Nota Kit: " + result.detail() + ")"));
            }
        } else {
            // Queue pending kit for offline player
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "odysseiapendingkit " + targetName + " oldschool");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a[Odysseia] &eRango y permisos otorgados a &b" + targetName + "&e. El kit se le entregará automáticamente al conectarse."));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            String search = args[0].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(search)) {
                    matches.add(player.getName());
                }
            }
            return matches;
        }
        return List.of();
    }
}
