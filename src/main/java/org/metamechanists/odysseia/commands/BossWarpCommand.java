package org.metamechanists.odysseia.commands;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.boss.arena.BossArenaService;

/** Public entry point for isolated boss sessions. */
public final class BossWarpCommand implements CommandExecutor, TabCompleter {
    private final BossArenaService arenas;
    public BossWarpCommand(BossArenaService arenas) { this.arenas = arenas; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 1 && (args[0].equalsIgnoreCase("precios") || args[0].equalsIgnoreCase("precio"))) {
            sendPrices(player);
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("spectate")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !arenas.spectate(player, target)) player.sendMessage("§cEse jugador no está en una arena activa.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("§eUso: /bosswarp <jefe> [solo|grupo] | /bosswarp precios | /bosswarp spectate <jugador>");
            sendPrices(player);
            return true;
        }
        boolean group = args.length > 1 && args[1].equalsIgnoreCase("grupo");
        Set<Player> roster = new LinkedHashSet<>();
        roster.add(player);
        if (group) {
            for (int index = 2; index < args.length; index++) {
                Player member = Bukkit.getPlayerExact(args[index]);
                if (member == null) { player.sendMessage("§cNo está conectado: " + args[index]); return true; }
                roster.add(member);
            }
            if (roster.size() < 2) { player.sendMessage("§cGrupo requiere al menos dos jugadores."); return true; }
        }
        var result = arenas.start(args[0].toLowerCase(Locale.ROOT), roster, group);
        if (!result.started()) {
            player.sendMessage("§c[BossArena] " + result.error());
            return true;
        }
        if (result.feePerPlayer() > 0.0D) {
            player.sendMessage("§6[BossArena] §eEntrada cobrada: §6" + formatFee(result.feePerPlayer())
                    + " Dragmas §epor jugador.");
        } else {
            player.sendMessage("§6[BossArena] §eArena iniciada sin cobro.");
        }
        return true;
    }

    private void sendPrices(Player player) {
        player.sendMessage("§6[BossArena] §eEntradas por jugador: §fZeus §6" + formatFee(arenas.entryFee("zeus"))
                + " §8| §fHades §6" + formatFee(arenas.entryFee("hades"))
                + " §8| §fTifón §6" + formatFee(arenas.entryFee("tifon")));
        player.sendMessage("§6[BossArena] §fDragón §6" + formatFee(arenas.entryFee("dragon"))
                + " §8| §fWither Storm §6" + formatFee(arenas.entryFee("wither_storm"))
                + " §8| §7El grupo paga una entrada por integrante.");
    }

    private static String formatFee(double fee) {
        return String.format(Locale.ROOT, "%,.0f", fee);
    }
    @Override public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) return List.of("zeus", "hades", "tifon", "dragon", "wither_storm", "precios", "spectate");
        if (args.length == 2 && !args[0].equalsIgnoreCase("spectate")) return List.of("solo", "grupo");
        if (args.length >= 3 && args[1].equalsIgnoreCase("grupo")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
