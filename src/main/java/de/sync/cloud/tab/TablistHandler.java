package de.sync.cloud.tab;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;


import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.concurrent.TimeUnit;

public class TablistHandler implements Listener {

    private final String rawHeader;
    private final String rawFooter;
    private final Plugin plugin;
    private static volatile int currentMaxPlayers = 65; // Initialwert, wird später überschrieben

    public TablistHandler(Plugin plugin) {
        this.plugin = plugin;

        File serviceFile = new File("service.json").getAbsoluteFile();
        serviceFile = serviceFile.getParentFile().getParentFile().getParentFile();
        serviceFile = new File(serviceFile, "service.json");

        String headerTemp = "\n &b&o■ &8┃ &3&lSyncCloud &8● &7Earthquake &8&l» &7&o%online_players%&8/&7&o%max_players% &8┃ &b&o■ \n &8► &7Current server &8● &b%server% &8◄ \n ";
        String footerTemp = "\n &7Discord &8&l» &bdiscord.synccloud.eu \n &7&oNext &3&l&oGeneration &7&oNetwork \n ";

        if (serviceFile.exists()) {
            try (FileReader reader = new FileReader(serviceFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("header")) {
                    headerTemp = json.get("header").getAsString();
                }
                if (json.has("footer")) {
                    footerTemp = json.get("footer").getAsString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.rawHeader = headerTemp;
        this.rawFooter = footerTemp;

        fetchMaxPlayersFromMySQL(); // Initial laden
        startTablistUpdater(); // Regelmäßige Aktualisierung
    }

    public void updateTablist(ProxiedPlayer player) {
        int onlinePlayers = ProxyServer.getInstance().getOnlineCount();

        String server = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";

        String headerFormatted = rawHeader
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(currentMaxPlayers))
                .replace("%server%", server);

        String footerFormatted = rawFooter
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(currentMaxPlayers))
                .replace("%server%", server);

        headerFormatted = ChatColor.translateAlternateColorCodes('&', headerFormatted);
        footerFormatted = ChatColor.translateAlternateColorCodes('&', footerFormatted);

        player.setTabHeader(
                new TextComponent(headerFormatted),
                new TextComponent(footerFormatted)
        );
    }

    private void startTablistUpdater() {
        ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
            fetchMaxPlayersFromMySQL(); // Wert aus MySQL neu laden

            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                updateTablist(player);
            }
        }, 1L, 10L, TimeUnit.SECONDS); // Alle 10 Sekunden aktualisieren
    }

    private void fetchMaxPlayersFromMySQL() {
        try {
            File mysqlFile = new File("mysql.json").getAbsoluteFile();
            mysqlFile = mysqlFile.getParentFile().getParentFile().getParentFile();
            mysqlFile = new File(mysqlFile, "mysql.json");

            if (!mysqlFile.exists()) {
                plugin.getLogger().warning("mysql.json nicht gefunden!");
                return;
            }

            JsonObject config;
            try (FileReader reader = new FileReader(mysqlFile)) {
                config = JsonParser.parseReader(reader).getAsJsonObject();
            }

            String host = config.get("host").getAsString();
            int port = config.get("port").getAsInt();
            String db = config.get("database").getAsString();
            String user = config.get("user").getAsString();
            String pass = config.get("password").getAsString();

            String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true";

            try (Connection conn = DriverManager.getConnection(jdbc, user, pass)) {
                String proxyName = plugin.getProxy().getName();
                String query = "SELECT slots FROM maxslots WHERE proxy_name = ? ORDER BY timestamp DESC LIMIT 1";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, proxyName);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int slots = rs.getInt("slots");
                            if (slots > 0) {
                                currentMaxPlayers = slots;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Abrufen von MaxSlots aus MySQL: " + e.getMessage());
        }
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        updateTablist(event.getPlayer());
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
                 updateTablist(event.getPlayer());

    }
    public static void startMaxPlayersUpdater() {
            currentMaxPlayers = getMaxPlayersFromMySQL();

    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        int online = ProxyServer.getInstance().getOnlineCount();

        int maxPlayers = currentMaxPlayers; // Wert vom Scheduler, keine DB-Abfrage hier!

        ServerPing ping = event.getResponse();
        ping.setPlayers(new ServerPing.Players(maxPlayers, online, ping.getPlayers().getSample()));
        event.setResponse(ping);
    }


    public static int getMaxPlayersFromMySQL() {
        try {
            // Starte vom Plugins-Ordner aus



            File mysqlFile = new File("mysql.json").getAbsoluteFile();
            mysqlFile = mysqlFile.getParentFile().getParentFile().getParentFile();
            mysqlFile = new File(mysqlFile, "mysql.json");



            if (!mysqlFile.exists()) {
                System.out.println("mysql.json nicht gefunden unter: " + mysqlFile.getAbsolutePath());
                return 65;
            }

            JsonObject config;
            try (FileReader reader = new FileReader(mysqlFile)) {
                config = JsonParser.parseReader(reader).getAsJsonObject();
            }

            String host = config.get("host").getAsString();
            int port = config.get("port").getAsInt();
            String db = config.get("database").getAsString();
            String user = config.get("user").getAsString();
            String pass = config.get("password").getAsString();

            String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true";

            try (Connection conn = DriverManager.getConnection(jdbc, user, pass)) {
                String proxyName = ProxyServer.getInstance().getName();

                String query = "SELECT slots FROM maxslots WHERE proxy_name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, proxyName);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        return rs.getInt("slots");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Lesen der Slots aus MySQL: " + e.getMessage());
        }
        return 65;
    }


}
