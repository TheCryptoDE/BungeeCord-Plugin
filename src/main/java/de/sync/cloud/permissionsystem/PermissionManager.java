package de.sync.cloud.permissionsystem;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {

    private final Map<UUID, Set<String>> permissionCache = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public PermissionManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Set<String> getPermissions(UUID uuid) {
        return permissionCache.getOrDefault(uuid, Collections.emptySet());
    }

    public void reload() {
        File mysqlFile = new File("mysql.json"); // Direkt im Hauptverzeichnis von BungeeCord

        if (!mysqlFile.exists()) {
            plugin.getLogger().warning("mysql.json nicht gefunden unter: " + mysqlFile.getAbsolutePath());
            return;
        }

        try (FileReader reader = new FileReader(mysqlFile)) {
            JsonObject mysqlConfig = JsonParser.parseReader(reader).getAsJsonObject();

            String host = mysqlConfig.get("host").getAsString();
            int port = mysqlConfig.get("port").getAsInt();
            String database = mysqlConfig.get("database").getAsString();
            String user = mysqlConfig.get("user").getAsString();
            String password = mysqlConfig.get("password").getAsString();

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                Map<UUID, Set<String>> newPermissions = new HashMap<>();

                // Spielergruppen holen
                Map<UUID, Set<String>> playerGroups = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, group_name FROM player_groups");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String group = rs.getString("group_name");
                        playerGroups.computeIfAbsent(uuid, k -> new HashSet<>()).add(group);
                    }
                }

                // Gruppenrechte holen
                Map<String, Set<String>> groupPermissions = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT group_name, permission FROM group_permissions");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String group = rs.getString("group_name");
                        String perm = rs.getString("permission");
                        groupPermissions.computeIfAbsent(group, k -> new HashSet<>()).add(perm);
                    }
                }

                // Direktrechte holen
                Map<UUID, Set<String>> directPermissions = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, permission FROM permissions");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String perm = rs.getString("permission");
                        directPermissions.computeIfAbsent(uuid, k -> new HashSet<>()).add(perm);
                    }
                }

                // Kombinieren: Gruppenrechte + Direktrechte
                for (UUID uuid : playerGroups.keySet()) {
                    Set<String> perms = new HashSet<>();
                    for (String group : playerGroups.get(uuid)) {
                        perms.addAll(groupPermissions.getOrDefault(group, Collections.emptySet()));
                    }
                    perms.addAll(directPermissions.getOrDefault(uuid, Collections.emptySet()));
                    newPermissions.put(uuid, perms);
                }

                // Direktrechte-only Spieler
                for (UUID uuid : directPermissions.keySet()) {
                    newPermissions.computeIfAbsent(uuid, k -> new HashSet<>()).addAll(directPermissions.get(uuid));
                }

                // Cache ersetzen
                permissionCache.clear();
                permissionCache.putAll(newPermissions);

                plugin.getLogger().info("Permissions erfolgreich geladen. Gesamt: " + permissionCache.size());

            } catch (SQLException e) {
                plugin.getLogger().severe("MySQL Fehler: " + e.getMessage());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Lesen von mysql.json: " + e.getMessage());
        }
    }
}
