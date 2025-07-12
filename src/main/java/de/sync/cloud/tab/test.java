package de.sync.cloud.tab;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Listener;

import java.net.InetSocketAddress;

public class test implements Listener {

    private final int maxPlayersPerServer = 20; // Beispielslotgrenze
    private final String baseTemplate = "lobby"; // Template für neue Server

    private void checkServerLoad() {
        ProxyServer.getInstance().getServers().forEach((name, serverInfo) -> {
            int onlinePlayers = serverInfo.getPlayers().size();

            if (onlinePlayers >= maxPlayersPerServer) {
               // getLogger().info("Server " + name + " ist voll (" + onlinePlayers + "/" + maxPlayersPerServer + "), starte neuen Server...");

                String newServerName = name + "_" + System.currentTimeMillis();
                boolean isProxy = false;

                String response = sendStartServerAndGetResponse(newServerName, baseTemplate, isProxy);
                if (response != null && response.startsWith("SERVER_STARTED")) {
                    int port = extractPortFromResponse(response);
                    if (port > 0) {
                        addServerToProxy(newServerName, port);
                  //      getLogger().info("Neuer Server " + newServerName + " gestartet und hinzugefügt.");
                        logServerStartToMySQL(newServerName, baseTemplate);
                    }
                }
            }
        });
    }

    // --- Placeholder-Methoden (musst du wie in deinem Code implementieren) ---

    private String sendStartServerAndGetResponse(String serverName, String template, boolean proxy) {
        // Deine Logik hier
        return "SERVER_STARTED:port=25569"; // Beispielantwort
    }

    private int extractPortFromResponse(String response) {
        try {
            String[] parts = response.split("=");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private void addServerToProxy(String serverName, int port) {
        // Beispiel-Implementierung
        ServerInfo info = ProxyServer.getInstance().constructServerInfo(
                serverName,
                new InetSocketAddress("127.0.0.1", port),
                "Auto-Server",
                false
        );
        ProxyServer.getInstance().getServers().put(serverName, info);
    }

    private void logServerStartToMySQL(String serverName, String template) {
        // Logik für Datenbank
    }

}
