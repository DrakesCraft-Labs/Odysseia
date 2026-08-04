package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.cosmetics.CosmeticService;

import java.util.List;

/** Comando /cosmeticos para equipar auras, rastros y efectos de muerte. */
public final class CosmeticsCommand implements CommandExecutor, TabCompleter {

    private final CosmeticService cosmeticService;

    public CosmeticsCommand(CosmeticService cosmeticService) {
        this.cosmeticService = cosmeticService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "=== Menú de Cosméticos DrakesCraft ===");
            player.sendMessage(ChatColor.YELLOW + "/cosmeticos aura <flame|lightning|soul|water|titan|caos|none>");
            player.sendMessage(ChatColor.YELLOW + "/cosmeticos rastro <sparkle|heart|dragon|portal|none>");
            player.sendMessage(ChatColor.YELLOW + "/cosmeticos muerte <lightning|totem|explosion|none>");
            return true;
        }

        String type = args[0].toLowerCase();
        String choice = args[1].toLowerCase();

        switch (type) {
            case "aura" -> {
                if (!choice.equalsIgnoreCase("none") && !player.hasPermission("drakes.cosmetics.aura." + choice)) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para el aura " + choice + ".");
                    return true;
                }
                cosmeticService.setAura(player, choice);
            }
            case "rastro", "trail" -> {
                if (!choice.equalsIgnoreCase("none") && !player.hasPermission("drakes.cosmetics.trail." + choice)) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para el rastro " + choice + ".");
                    return true;
                }
                cosmeticService.setTrail(player, choice);
            }
            case "muerte", "death" -> {
                if (!choice.equalsIgnoreCase("none") && !player.hasPermission("drakes.cosmetics.death." + choice)) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para el efecto de muerte " + choice + ".");
                    return true;
                }
                cosmeticService.setDeathEffect(player, choice);
            }
            default -> player.sendMessage(ChatColor.RED + "Uso: /cosmeticos <aura|rastro|muerte> <nombre|none>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("aura", "rastro", "muerte");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "aura" -> List.of("flame", "lightning", "soul", "water", "titan", "caos", "none");
                case "rastro", "trail" -> List.of("sparkle", "heart", "dragon", "portal", "none");
                case "muerte", "death" -> List.of("lightning", "totem", "explosion", "none");
                default -> List.of();
            };
        }
        return List.of();
    }
}
