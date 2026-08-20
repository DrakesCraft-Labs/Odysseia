package org.metamechanists.odysseia.laboratorio;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Frontera fisica del laboratorio: lo que entra no sale y lo que sale no entro.
 *
 * El guardian de comandos cierra las vias que pasan por escribir algo en el chat, pero no las
 * que no pasan por ahi, y la mas evidente es la mas simple: cruzar el portal con la mochila
 * llena. De poco sirve prohibir /sellall si un jugador puede invocar un lingote con /sf cheat y
 * llevarselo andando. Aqui el inventario real se deja en consigna al entrar y se devuelve al
 * salir; lo fabricado dentro se descarta en la puerta.
 *
 * ORDEN DE LAS OPERACIONES
 *   Al entrar se guarda, se vuelve a LEER lo guardado, y solo si lo leido cuadra se vacia el
 *   inventario. Si el guardado falla o no se puede releer, no se toca nada y se devuelve al
 *   jugador a su mundo: prefiero que alguien no pueda entrar al laboratorio a que alguien pierda
 *   su equipo. Es la misma razon por la que la consigna vive en SQLite y no en memoria.
 */
public final class SandboxIsolationListener implements Listener {

    private final JavaPlugin plugin;
    private final SandboxStashRepository stash;
    private final Set<String> sandboxWorlds;

    public SandboxIsolationListener(JavaPlugin plugin, SandboxStashRepository stash, Set<String> sandboxWorlds) {
        this.plugin = plugin;
        this.stash = stash;
        this.sandboxWorlds = sandboxWorlds;
    }

    private boolean isSandbox(Location location) {
        return location != null && location.getWorld() != null
                && sandboxWorlds.contains(location.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        boolean ahora = isSandbox(player.getLocation());
        boolean antes = sandboxWorlds.contains(event.getFrom().getName().toLowerCase(Locale.ROOT));

        if (ahora && !antes) {
            entrar(player);
        } else if (antes && !ahora) {
            salir(player);
        }
    }

    /**
     * Devuelve el inventario a quien se quedo con el en consigna.
     *
     * Pasa cuando el servidor cae con gente dentro, o cuando la regeneracion semanal saca a un
     * jugador desconectado del laboratorio: en ambos casos vuelve fuera y su equipo sigue
     * depositado, sin que ningun cambio de mundo llegue a dispararse.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isSandbox(player.getLocation())) return;
        try {
            if (stash.load(player.getUniqueId()) != null) {
                salir(player);
                plugin.getLogger().info("Inventario devuelto a " + player.getName()
                        + ": estaba en consigna del laboratorio y ha entrado fuera de el.");
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "No se pudo comprobar la consigna del laboratorio de " + player.getName(), error);
        }
    }

    private void entrar(Player player) {
        PlayerInventory inventory = player.getInventory();
        UUID id = player.getUniqueId();

        SandboxStashRepository.Stash deposito = new SandboxStashRepository.Stash(
                inventory.getContents().clone(),
                inventory.getArmorContents().clone(),
                inventory.getItemInOffHand(),
                player.getLevel(),
                player.getExp(),
                player.getGameMode().name());

        try {
            stash.save(id, deposito);
            // Releer antes de vaciar. Un guardado que no se puede recuperar es una perdida de
            // equipo, y no hay forma de deshacerla despues de limpiar el inventario.
            SandboxStashRepository.Stash comprobacion = stash.load(id);
            if (comprobacion == null
                    || comprobacion.contents().length != deposito.contents().length) {
                throw new SQLException("la consigna no se pudo releer tras guardarla");
            }
        } catch (SQLException | RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "No se pudo depositar el inventario de " + player.getName()
                            + "; se cancela la entrada al laboratorio y se conserva su equipo", error);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cNo se pudo guardar tu inventario, asi que no te dejo entrar. "
                            + "&7Tu equipo esta intacto. Avisa al staff."));
            devolverAlSpawn(player);
            return;
        }

        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItemInOffHand(null);
        player.setLevel(0);
        player.setExp(0f);
        player.setGameMode(GameMode.CREATIVE);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6DrakesCraft &8· &eLaboratorio&7. Tu inventario esta guardado y te espera al salir.&r\n"
                        + "&7Aqui tienes creativo y &e/sf cheat&7 sin limites. &cNada de lo que hagas aqui sale&7: "
                        + "ni objetos, ni dinero, ni progreso. El mundo se borra cada lunes."));
    }

    private void salir(Player player) {
        UUID id = player.getUniqueId();
        PlayerInventory inventory = player.getInventory();

        SandboxStashRepository.Stash deposito;
        try {
            deposito = stash.load(id);
        } catch (SQLException | RuntimeException error) {
            plugin.getLogger().log(Level.SEVERE,
                    "No se pudo leer la consigna de " + player.getName()
                            + "; se le deja el inventario tal cual para no borrar nada", error);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6DrakesCraft &8· &cNo pude recuperar tu inventario guardado. &7Avisa al staff "
                            + "&cantes de tocar nada&7."));
            return;
        }

        if (deposito == null) {
            // Sin consigna no hay nada que devolver, pero lo que lleva encima se fabrico dentro
            // del laboratorio y no puede cruzar. Se descarta y se deja constancia.
            inventory.clear();
            inventory.setArmorContents(null);
            inventory.setItemInOffHand(null);
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getLogger().warning("Salida del laboratorio de " + player.getName()
                    + " sin consigna registrada; se descarto lo que llevaba encima.");
            return;
        }

        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItemInOffHand(null);
        inventory.setContents(ajustar(deposito.contents(), inventory.getSize()));
        inventory.setArmorContents(deposito.armor());
        inventory.setItemInOffHand(deposito.offhand());
        player.setLevel(deposito.level());
        player.setExp(deposito.exp());
        try {
            player.setGameMode(GameMode.valueOf(deposito.gameMode()));
        } catch (IllegalArgumentException ignored) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Solo se borra la consigna despues de haberla devuelto. Si el servidor se cayera entre
        // medias, la fila sigue ahi y onJoin la devuelve otra vez.
        try {
            stash.clear(id);
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING,
                    "Inventario devuelto a " + player.getName() + " pero no se pudo limpiar su consigna", error);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6DrakesCraft &8· &7Tu inventario ha vuelto. Lo del laboratorio se queda en el laboratorio."));
    }

    private static ItemStack[] ajustar(ItemStack[] guardado, int tamano) {
        ItemStack[] resultado = new ItemStack[tamano];
        System.arraycopy(guardado, 0, resultado, 0, Math.min(guardado.length, tamano));
        return resultado;
    }

    private void devolverAlSpawn(Player player) {
        String comando = plugin.getConfig().getString("modalidades.comando-spawn", "spawn");
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando + " " + player.getName()));
    }

    /** Devuelve los inventarios que quedaron en consigna de jugadores que ya no estan dentro. */
    public void reconciliarAlArrancar() {
        try {
            List<UUID> pendientes = stash.pending();
            if (!pendientes.isEmpty()) {
                plugin.getLogger().info("Consignas del laboratorio pendientes: " + pendientes.size()
                        + ". Se devolveran cuando cada jugador entre fuera del laboratorio.");
            }
        } catch (SQLException error) {
            plugin.getLogger().log(Level.WARNING, "No se pudo revisar las consignas del laboratorio", error);
        }
    }
}
