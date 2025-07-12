package de.sync.cloud.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.sql.*;

public class SetSlotCommand extends Command {

    private final Plugin plugin;

    public SetSlotCommand(Plugin plugin) {
        super("setslot", "bungee.setslot"); // Optional: Permission
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Benutzung: /setslot <anzahl>");
            return;
        }

        int slots;
        try {
            slots = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Ungültige Zahl: " + args[0]);
            return;
        }

        updateSlotCount(slots, sender);
    }

    private void updateSlotCount(int slots, CommandSender sender) {
        File mysqlFile = new File(plugin.getDataFolder().getAbsoluteFile()
                .toPath().getParent().getParent().getParent().getParent()
                .resolve("mysql.json").toUri());

        if (!mysqlFile.exists()) {
            sender.sendMessage(ChatColor.RED + "mysql.json nicht gefunden.");
            return;
        }

        try (FileReader reader = new FileReader(mysqlFile)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
            String host = config.get("host").getAsString();
            int port = config.get("port").getAsInt();
            String db = config.get("database").getAsString();
            String user = config.get("user").getAsString();
            String pass = config.get("password").getAsString();

            String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true";

            try (Connection conn = DriverManager.getConnection(jdbc, user, pass)) {
                createTableIfNotExists(conn);

                String proxyName = plugin.getProxy().getName();
                String sql = "INSERT INTO maxslots (proxy_name, slots, timestamp) VALUES (?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE slots = VALUES(slots), timestamp = NOW()";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, proxyName);
                    stmt.setInt(2, slots);
                    stmt.executeUpdate();
                }

                sender.sendMessage(ChatColor.GREEN + "Maximale Slots wurden in MySQL gesetzt: " + slots);
                plugin.getLogger().info("MaxSlots in DB gespeichert: " + slots);
            } catch (SQLException e) {
                plugin.getLogger().warning("MySQL-Fehler: " + e.getMessage());
                sender.sendMessage(ChatColor.RED + "Fehler beim MySQL-Zugriff.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Lesen der mysql.json: " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "Fehler beim Lesen der mysql.json.");
        }
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS maxslots (" +
                "proxy_name VARCHAR(255) PRIMARY KEY," +  // WICHTIG: macht proxy_name eindeutig
                "slots INT NOT NULL," +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
