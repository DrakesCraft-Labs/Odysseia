package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.purchase.PurchaseEngine;

/** Entrada restringida para Tebex y operaciones administrativas del motor de compras. */
public final class PurchaseCommand implements CommandExecutor {
    private final PurchaseEngine engine;
    public PurchaseCommand(PurchaseEngine engine) { this.engine = engine; }
    @Override public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command,@NotNull String label,@NotNull String[] args) {
        if (!sender.hasPermission("odysseia.purchase.admin")) { sender.sendMessage(ChatColor.RED+"Sin permiso."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("validate")) { sender.sendMessage(engine.validateCatalog()); return true; }
        if (args.length >= 4 && (args[0].equalsIgnoreCase("deliver") || args[0].equalsIgnoreCase("dry-run"))) {
            PurchaseEngine.Result result=engine.deliver(args[1],args[2],args[3],args[0].equalsIgnoreCase("dry-run"),sender.getName());
            sender.sendMessage((result.success()?ChatColor.GREEN:ChatColor.RED)+result.message()); return true;
        }
        sender.sendMessage(ChatColor.YELLOW+"Uso: /"+label+" deliver <transaction> <username> <product_id> | dry-run <transaction> <username> <product_id> | validate"); return true;
    }
}
