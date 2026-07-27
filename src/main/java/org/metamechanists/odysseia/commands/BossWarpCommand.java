package org.metamechanists.odysseia.commands;

import java.util.List;
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
        if (args.length >= 2 && args[0].equalsIgnoreCase("spectate")) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !arenas.spectate(player, target)) player.sendMessage("§cEse jugador no está en una arena activa.");
            return true;
        }
        if (args.length < 1) { player.sendMessage("§eUso: /bosswarp <jefe> [solo|grupo] | /bosswarp spectate <jugador>"); return true; }
        boolean group = args.length > 1 && args[1].equalsIgnoreCase("grupo");
        var session = arenas.start(args[0].toLowerCase(), List.of(player), group);
        if (session == null) player.sendMessage("§cNo se pudo iniciar la arena. Ya puedes estar en otra sesión o el jefe no existe.");
        return true;
    }
    @Override public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? List.of("zeus", "hades", "tifon", "dragon", "wither_storm", "spectate") : args.length == 2 && !args[0].equalsIgnoreCase("spectate") ? List.of("solo", "grupo") : List.of();
    }
}
