package org.metamechanists.odysseia.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.reencarnacion.CapsulaRecuerdosGUI;
import org.metamechanists.odysseia.reencarnacion.ReencarnacionManager;
import org.metamechanists.odysseia.reencarnacion.ReencarnacionSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comando /reencarnar para que los jugadores inicien el rito de prestigio y la consola
 * de SAORI ejecute el borrado cruzado autorizado desde Discord.
 */
public final class ReencarnarCommand implements CommandExecutor, TabCompleter {

    private final ReencarnacionManager manager;

    public ReencarnarCommand(ReencarnacionManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Ejecucion administrativa desde consola / SAORI: /reencarnar ejecutar <jugador> <codigo>
        if (args.length >= 3 && args[0].equalsIgnoreCase("ejecutar")) {
            if (!sender.hasPermission("odysseia.reencarnar.admin") && !(sender.equals(Bukkit.getConsoleSender()))) {
                sender.sendMessage(Component.text("No tienes permiso para autorizar reencarnaciones.", NamedTextColor.RED));
                return true;
            }

            String targetName = args[1];
            String code = args[2].toUpperCase();

            ReencarnacionSession session = manager.getSessionByCode(code);
            if (session == null) {
                sender.sendMessage("ERROR: El codigo '" + code + "' no existe o ha expirado.");
                return true;
            }

            if (!targetName.equals("*") && !session.getPlayerName().equalsIgnoreCase(targetName)) {
                sender.sendMessage("ERROR: El codigo '" + code + "' pertenece a '" + session.getPlayerName() + "', no a '" + targetName + "'.");
                return true;
            }

            if (targetName.equals("*")) {
                targetName = session.getPlayerName();
            }

            boolean ok = manager.executeSession(code);
            if (ok) {
                sender.sendMessage("SUCCESS: Reencarnacion ejecutada exitosamente para " + targetName + ".");
            } else {
                sender.sendMessage("ERROR: Fallo durante la ejecucion de la reencarnacion para " + targetName + ".");
            }
            return true;
        }

        // Consulta de informacion para SAORI / consola: /reencarnar info <codigo>
        if (args.length >= 2 && args[0].equalsIgnoreCase("info")) {
            String code = args[1].toUpperCase();
            ReencarnacionSession session = manager.getSessionByCode(code);
            if (session == null) {
                sender.sendMessage("NOT_FOUND");
                return true;
            }

            StringBuilder itemsInfo = new StringBuilder();
            for (ItemStack item : session.getCapsuleItems()) {
                if (item != null) {
                    if (itemsInfo.length() > 0) itemsInfo.append(", ");
                    itemsInfo.append(item.getType().name()).append(" x").append(item.getAmount());
                }
            }
            if (itemsInfo.length() == 0) itemsInfo.append("Ninguno (0 items)");

            sender.sendMessage("SESSION:" + session.getPlayerName() + ":" + session.getPlayerUuid() + ":" + session.getCapsuleItems().size() + ":" + itemsInfo);
            return true;
        }

        // Acciones de jugador in-game
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Uso desde consola: /reencarnar ejecutar <jugador> <codigo> | /reencarnar info <codigo>");
            return true;
        }

        // /reencarnar cancelar
        if (args.length >= 1 && args[0].equalsIgnoreCase("cancelar")) {
            ReencarnacionSession session = manager.getSessionByPlayer(player.getUniqueId());
            if (session != null) {
                manager.cancelSession(player.getUniqueId());
                player.sendMessage(Component.text("✔ Tu solicitud de reencarnación y código han sido anulados.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("No tienes ninguna sesión de reencarnación pendiente.", NamedTextColor.YELLOW));
            }
            return true;
        }

        // /reencarnar (sin argumentos)
        ReencarnacionSession existing = manager.getSessionByPlayer(player.getUniqueId());
        if (existing != null && !existing.isExpired()) {
            player.sendMessage(Component.text("§eYa tienes una solicitud de reencarnación activa con el código: §6§l" + existing.getCode()));
            player.sendMessage(Component.text("§7Ve a Discord y escribe en §b#habla-con-saori§7: §f!reencarnar " + existing.getCode()));
            player.sendMessage(Component.text("§cSi deseas anularla para empezar de nuevo, usa: §f/reencarnar cancelar"));
            return true;
        }

        // Abrir la GUI de la Cápsula
        CapsulaRecuerdosGUI gui = new CapsulaRecuerdosGUI(manager, player);
        gui.open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("cancelar");
            if (sender.hasPermission("odysseia.reencarnar.admin") || sender.equals(Bukkit.getConsoleSender())) {
                list.add("ejecutar");
                list.add("info");
            }
            return list;
        }
        return Collections.emptyList();
    }
}
