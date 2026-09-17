package org.metamechanists.odysseia.events.boxes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persiste plantillas de cofres de recompensa en SQLite para que el staff pueda
 * guardar cajas premiadas (/evento box save <nombre>) y entregarlas masivamente o individualmente.
 */
public class StaffBoxRepository implements AutoCloseable {

    private final Connection connection;
    private final Logger logger;

    public StaffBoxRepository(File databaseFile, Logger logger) throws SQLException {
        this.logger = logger;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS odysseia_boxes (
                    box_name TEXT PRIMARY KEY,
                    creator TEXT NOT NULL,
                    items_base64 TEXT NOT NULL,
                    item_count INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);
        }
    }

    public synchronized boolean saveBox(String name, String creator, ItemStack[] items) {
        if (name == null || name.isBlank() || items == null) return false;
        String cleanName = name.trim().toLowerCase(Locale.ROOT);
        List<ItemStack> nonNullItems = Arrays.stream(items)
                .filter(it -> it != null && !it.getType().isAir())
                .toList();

        String base64 = serialize(nonNullItems.toArray(new ItemStack[0]));
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO odysseia_boxes(box_name, creator, items_base64, item_count, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(box_name) DO UPDATE SET
                creator=excluded.creator,
                items_base64=excluded.items_base64,
                item_count=excluded.item_count,
                created_at=excluded.created_at
        """)) {
            ps.setString(1, cleanName);
            ps.setString(2, creator);
            ps.setString(3, base64);
            ps.setInt(4, nonNullItems.size());
            ps.setString(5, Instant.now().toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffBox] Error al guardar caja " + name, e);
            return false;
        }
    }

    public synchronized ItemStack[] getBoxItems(String name) {
        if (name == null) return null;
        String cleanName = name.trim().toLowerCase(Locale.ROOT);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT items_base64 FROM odysseia_boxes WHERE box_name=?")) {
            ps.setString(1, cleanName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return deserialize(rs.getString("items_base64"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffBox] Error al leer caja " + name, e);
        }
        return null;
    }

    public synchronized boolean deleteBox(String name) {
        if (name == null) return false;
        String cleanName = name.trim().toLowerCase(Locale.ROOT);
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM odysseia_boxes WHERE box_name=?")) {
            ps.setString(1, cleanName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffBox] Error al eliminar caja " + name, e);
            return false;
        }
    }

    public synchronized List<String> listBoxes() {
        List<String> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT box_name, item_count, creator, created_at FROM odysseia_boxes ORDER BY box_name ASC")) {
            while (rs.next()) {
                list.add(String.format(Locale.ROOT, "&e%s &7(%d items, por &f%s&7)",
                        rs.getString("box_name"),
                        rs.getInt("item_count"),
                        rs.getString("creator")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffBox] Error al listar cajas", e);
        }
        return list;
    }

    public synchronized Set<String> getBoxNames() {
        Set<String> set = new HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT box_name FROM odysseia_boxes")) {
            while (rs.next()) {
                set.add(rs.getString("box_name"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[StaffBox] Error al obtener nombres de cajas", e);
        }
        return set;
    }

    public int giveBox(String boxName, Player target) {
        ItemStack[] items = getBoxItems(boxName);
        if (items == null || items.length == 0) return 0;

        Location dropLoc = target.getLocation();
        boolean droppedAny = false;
        int count = 0;

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            ItemStack clone = item.clone();
            HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(clone);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    target.getWorld().dropItemNaturally(dropLoc, drop);
                    droppedAny = true;
                }
            }
            count++;
        }

        target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6🎁 &a¡Has recibido la recompensa &e" + boxName + "&a!"));
        if (droppedAny) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e⚠️ &eTu inventario estaba lleno. Algunos objetos han caído a tus pies."));
        }
        return count;
    }

    public int giveBoxToAll(String boxName) {
        ItemStack[] items = getBoxItems(boxName);
        if (items == null || items.length == 0) return 0;

        int totalDelivered = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveBox(boxName, p);
            totalDelivered++;
        }
        return totalDelivered;
    }

    public static ItemStack[] getItemsFromTargetBlock(Player player) {
        Block targetBlock = player.getTargetBlockExact(6);
        if (targetBlock != null && targetBlock.getState() instanceof Container container) {
            return container.getInventory().getContents();
        }
        return null;
    }

    private static String serialize(ItemStack[] contents) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             BukkitObjectOutputStream data = new BukkitObjectOutputStream(output)) {
            data.writeInt(contents.length);
            for (ItemStack item : contents) {
                data.writeObject(item);
            }
            data.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Error serializando items de caja", e);
        }
    }

    private static ItemStack[] deserialize(String raw) {
        if (raw == null || raw.isBlank()) return new ItemStack[0];
        byte[] bytes = Base64.getDecoder().decode(raw);
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream data = new BukkitObjectInputStream(input)) {
            int length = data.readInt();
            ItemStack[] contents = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                contents[i] = (ItemStack) data.readObject();
            }
            return contents;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Error deserializando items de caja", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[StaffBox] Error cerrando conexión SQLite", e);
        }
    }
}
