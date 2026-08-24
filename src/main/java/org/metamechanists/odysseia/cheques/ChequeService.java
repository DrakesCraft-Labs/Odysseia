package org.metamechanists.odysseia.cheques;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.metamechanists.odysseia.Odysseia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

/** Emite y canjea cheques firmados, únicos y persistentes mediante Vault. */
public final class ChequeService implements Listener, AutoCloseable {

    private final Odysseia plugin;
    private final ChequeSigner signer;
    private final ChequeRepository repository;
    private final NamespacedKey versionKey;
    private final NamespacedKey amountKey;
    private final NamespacedKey serialKey;
    private final NamespacedKey signatureKey;

    public ChequeService(Odysseia plugin) throws IOException, SQLException {
        this.plugin = plugin;
        Files.createDirectories(plugin.getDataFolder().toPath());
        signer = new ChequeSigner(loadOrCreateSecret(plugin.getDataFolder().toPath().resolve("cheques.secret")));
        repository = new ChequeRepository(new java.io.File(plugin.getDataFolder(), "cheques.db"));
        versionKey = new NamespacedKey(plugin, "cheque_version");
        amountKey = new NamespacedKey(plugin, "cheque_amount");
        serialKey = new NamespacedKey(plugin, "cheque_serial");
        signatureKey = new NamespacedKey(plugin, "cheque_signature");
    }

    public ItemStack issue(long amount) {
        String serial = UUID.randomUUID().toString();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GREEN + "Cheque de " + ChatColor.GREEN + format(amount) + " Dragmas");
        meta.setLore(List.of(
                ChatColor.GRAY + "Emitido por el Banco de DrakesCraft",
                ChatColor.GRAY + "Clic derecho para depositarlo.",
                "",
                ChatColor.DARK_GRAY + "Serie: " + serial.substring(0, 8).toUpperCase(Locale.ROOT),
                ChatColor.GOLD + "Valor: " + ChatColor.YELLOW + format(amount) + " ₯",
                ChatColor.RED + "Único e intransferible después del canje"
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(versionKey, PersistentDataType.INTEGER, ChequeSigner.VERSION);
        data.set(amountKey, PersistentDataType.LONG, amount);
        data.set(serialKey, PersistentDataType.STRING, serial);
        data.set(signatureKey, PersistentDataType.BYTE_ARRAY, signer.sign(amount, serial));
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCheque(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(serialKey, PersistentDataType.STRING);
    }

    @EventHandler(ignoreCancelled = false)
    public void onRedeem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) || !isCheque(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("odysseia.cheque.redeem")) {
            player.sendMessage(ChatColor.RED + "No puedes canjear cheques aquí.");
            return;
        }
        ItemStack item = event.getItem();
        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        Integer version = data.get(versionKey, PersistentDataType.INTEGER);
        Long amount = data.get(amountKey, PersistentDataType.LONG);
        String serial = data.get(serialKey, PersistentDataType.STRING);
        byte[] signature = data.get(signatureKey, PersistentDataType.BYTE_ARRAY);
        if (version == null || version != ChequeSigner.VERSION || amount == null
                || !signer.verify(amount, serial, signature)) {
            player.sendMessage(ChatColor.RED + "Este cheque no es auténtico o fue alterado.");
            plugin.getLogger().warning("[Cheques] Intento de cheque inválido por " + player.getName());
            return;
        }
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            player.sendMessage(ChatColor.RED + "La economía no está disponible; el cheque no fue consumido.");
            return;
        }
        try {
            if (!repository.reserve(serial, amount, player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Este cheque ya fue canjeado o está siendo procesado.");
                return;
            }
        } catch (SQLException error) {
            player.sendMessage(ChatColor.RED + "No se pudo validar el cheque; no se consumió.");
            plugin.getLogger().log(Level.SEVERE, "[Cheques] Falló el libro contable", error);
            return;
        }

        consumeOne(player);
        EconomyResponse response = registration.getProvider().depositPlayer(player, amount.doubleValue());
        if (!response.transactionSuccess()) {
            restore(player, item.asOne());
            try {
                repository.release(serial);
            } catch (SQLException error) {
                plugin.getLogger().log(Level.SEVERE,
                        "[Cheques] Vault rechazó el pago y no se pudo liberar " + serial, error);
            }
            player.sendMessage(ChatColor.RED + "No se pudo depositar el cheque; fue devuelto.");
            plugin.getLogger().warning("[Cheques] Vault rechazó " + serial + ": " + response.errorMessage);
            return;
        }

        try {
            repository.complete(serial);
        } catch (SQLException error) {
            // El dinero ya fue abonado: el serial queda PENDING (bloqueado) para impedir duplicación.
            plugin.getLogger().log(Level.SEVERE,
                    "[Cheques] Pago realizado, pero el serial quedó PENDING: " + serial, error);
        }
        player.sendMessage(ChatColor.GREEN + "Cheque canjeado: " + ChatColor.GOLD + "+" + format(amount) + " Dragmas");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        plugin.getLogger().info("[Cheques] REDEEMED serial=" + serial + " player=" + player.getName() + " amount=" + amount);
    }

    private static void consumeOne(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }

    public static void restore(Player player, ItemStack item) {
        var leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private static byte[] loadOrCreateSecret(Path path) throws IOException {
        if (Files.exists(path)) {
            byte[] current = Files.readAllBytes(path);
            if (current.length < 32) throw new IOException("cheques.secret es inválido");
            return current;
        }
        byte[] generated = new byte[32];
        new SecureRandom().nextBytes(generated);
        try {
            Files.write(path, generated, java.nio.file.StandardOpenOption.CREATE_NEW);
            return generated;
        } catch (java.nio.file.FileAlreadyExistsException race) {
            return Files.readAllBytes(path);
        }
    }

    public static String format(long amount) {
        return NumberFormat.getIntegerInstance(new Locale("es", "CL")).format(amount);
    }

    @Override
    public void close() throws SQLException {
        repository.close();
    }
}
