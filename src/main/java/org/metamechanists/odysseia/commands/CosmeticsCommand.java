package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.cosmetics.Cosmetic;
import org.metamechanists.odysseia.cosmetics.CosmeticService;

import java.util.ArrayList;
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
        if (!player.hasPermission(CosmeticService.USE)) {
            player.sendMessage(ChatColor.RED + "Los cosméticos son exclusivos de rangos VIP y staff.");
            player.sendMessage(ChatColor.GRAY + "Mira los rangos con " + ChatColor.YELLOW + "/buy" + ChatColor.GRAY + ".");
            return true;
        }

        if (args.length < 2) {
            mostrarMenu(player, args.length == 1 ? args[0] : null);
            return true;
        }

        String tipo = Cosmetic.tipoCanonico(args[0]);
        if (tipo.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Uso: /cosmeticos <aura|rastro|muerte> <nombre|none>");
            return true;
        }
        String choice = args[1].toLowerCase();

        if (!choice.equals("none") && !player.hasPermission("drakes.cosmetics." + tipo + "." + choice)) {
            player.sendMessage(ChatColor.RED + "Ese cosmético no viene con tu rango.");
            player.sendMessage(ChatColor.GRAY + "Con " + ChatColor.YELLOW + "/cosmeticos " + args[0]
                    + ChatColor.GRAY + " ves los que sí tienes.");
            return true;
        }

        switch (tipo) {
            case "aura" -> cosmeticService.setAura(player, choice);
            case "trail" -> cosmeticService.setTrail(player, choice);
            case "death" -> cosmeticService.setDeathEffect(player, choice);
            default -> player.sendMessage(ChatColor.RED + "Uso: /cosmeticos <aura|rastro|muerte> <nombre|none>");
        }
        return true;
    }

    /** Lista lo que el jugador puede usar y, en gris, lo que le falta por rango. */
    private void mostrarMenu(Player player, String tipoPedido) {
        player.sendMessage(ChatColor.GOLD + "=== Cosméticos DrakesCraft ===");
        for (String etiqueta : List.of("aura", "rastro", "muerte")) {
            if (tipoPedido != null && !Cosmetic.tipoCanonico(tipoPedido).equals(Cosmetic.tipoCanonico(etiqueta))) {
                continue;
            }
            String tipo = Cosmetic.tipoCanonico(etiqueta);
            player.sendMessage(ChatColor.YELLOW + "/cosmeticos " + etiqueta + " <nombre|none>");
            for (Cosmetic cosmetic : Cosmetic.of(etiqueta)) {
                boolean tiene = player.hasPermission(cosmetic.permiso(tipo));
                player.sendMessage("  " + (tiene ? ChatColor.GREEN + "✔ " : ChatColor.DARK_GRAY + "✖ ")
                        + (tiene ? ChatColor.WHITE : ChatColor.DARK_GRAY) + cosmetic.id()
                        + ChatColor.GRAY + " · " + cosmetic.nombre()
                        + ChatColor.DARK_GRAY + " (" + cosmetic.rango() + ")");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !player.hasPermission(CosmeticService.USE)) return List.of();
        if (args.length == 1) return List.of("aura", "rastro", "muerte");
        if (args.length == 2) {
            String tipo = Cosmetic.tipoCanonico(args[0]);
            if (tipo.isEmpty()) return List.of();
            // Solo se sugiere lo que el jugador puede equipar de verdad.
            List<String> disponibles = new ArrayList<>();
            for (Cosmetic cosmetic : Cosmetic.of(args[0])) {
                if (player.hasPermission(cosmetic.permiso(tipo))) disponibles.add(cosmetic.id());
            }
            disponibles.add("none");
            return disponibles;
        }
        return List.of();
    }
}
