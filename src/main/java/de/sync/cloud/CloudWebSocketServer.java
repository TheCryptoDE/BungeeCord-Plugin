package de.sync.cloud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CloudWebSocketServer extends WebSocketServer {

    private static final Set<WebSocket> connections = ConcurrentHashMap.newKeySet();
    private static String currentMotd = "Willkommen auf dem Server!";

    public CloudWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        conn.send("[LOG] ✅ Verbindung hergestellt.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("❌ Verbindung getrennt: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject request = JsonParser.parseString(message).getAsJsonObject();

            if (!request.has("type")) {
                conn.send("[LOG] Fehler: Nachricht enthält kein 'type'-Feld.");
                return;
            }

            String type = request.get("type").getAsString().toUpperCase();

            switch (type) {
                case "LIST_SERVERS" -> {
                    String list = String.join(",", ProxyServer.getInstance().getServers().keySet());
                    conn.send("SERVERS " + list);
                }
                case "START_SERVER" -> {
                    if (!request.has("serverName") || !request.has("template") || !request.has("proxy")) {
                        conn.send("[LOG] Fehlende Parameter zum Starten.");
                        return; // Wichtig!
                    }

                    String name = request.get("serverName").getAsString();
                    String template = request.get("template").getAsString();
                    boolean proxy = request.get("proxy").getAsBoolean();

                    String result = startServer(name, template, proxy);
                    conn.send("[LOG] Starte Server: " + name + " (" + template + ", Proxy=" + proxy + ")");
                    conn.send("[LOG] Ergebnis: " + result);

            }
                case "STOP_SERVER" -> {
                    if (!request.has("serverName")) {
                        conn.send("[LOG] Fehlender Parameter: serverName");
                        return;
                    }

                    String name = request.get("serverName").getAsString();
                    boolean result = stopServer(name);
                    conn.send("[LOG] Stoppe Server: " + name + " - " + (result ? "Erfolgreich" : "Fehlgeschlagen"));
                }
                case "SERVER_STATUS" -> {
                    if (!request.has("serverName")) {
                        conn.send("[LOG] Fehlender Parameter: serverName");
                        return;
                    }

                    String name = request.get("serverName").getAsString();
                    String status = ProxyServer.getInstance().getServers().containsKey(name) ? "Online" : "Offline";
                    conn.send("STATUS " + status);
                }
                case "ONLINE_PLAYERS" -> {
                    if (!request.has("serverName")) {
                        conn.send("[LOG] Fehlender Parameter: serverName");
                        return;
                    }
                    String name = request.get("serverName").getAsString();

                    ServerInfo serverInfo = ProxyServer.getInstance().getServers().get(name);
                    int onlinePlayers = 0;
                    if (serverInfo != null) {
                        // Anzahl der Spieler ermitteln
                        onlinePlayers = ProxyServer.getInstance().getOnlineCount();
                    }
                    conn.send("ONLINE_PLAYERS " + name + " " + onlinePlayers);
                }

                case "SET_MOTD" -> {
                    if (!request.has("motd")) {
                        conn.send("[LOG] Fehlender Parameter: motd");
                        return;
                    }

                    currentMotd = request.get("motd").getAsString();
                    conn.send("MOTD_SET " + currentMotd);
                    conn.send("[LOG] MOTD gesetzt: " + currentMotd);
                }
                default -> conn.send("[LOG] Unbekannter Typ: " + type);
            }
        } catch (Exception e) {
            conn.send("[LOG] Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null && conn.isOpen()) {
            conn.send("[LOG] ❌ Fehler: " + ex.getMessage());
        }
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[CloudWebSocketServer] ✅ WebSocket-Server gestartet.");
    }

    private String startServer(String name, String template, boolean proxy) {
        if (ProxyServer.getInstance().getServers().containsKey(name)) {
            return "⚠️ Server '" + name + "' existiert bereits.";
        }

        ServerInfo dummyInfo = ProxyServer.getInstance().constructServerInfo(
                name,
                new InetSocketAddress("127.0.0.1", 25565),
                "Dummy Server (" + template + ")",
                false
        );

        ProxyServer.getInstance().getServers().put(name, dummyInfo);
        return "🟢 Server '" + name + "' gestartet mit Template '" + template + "'";
    }

    private boolean stopServer(String name) {
        return ProxyServer.getInstance().getServers().remove(name) != null;
    }

    private JsonObject getSystemStats() {
        JsonObject stats = new JsonObject();

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();

            stats.addProperty("cpu", String.format("%.2f", cpuLoad));
            stats.addProperty("ram", usedMemory / 1024 / 1024 + " MB / " + maxMemory / 1024 / 1024 + " MB");
        } catch (Exception e) {
            stats.addProperty("error", "Statistiken konnten nicht gelesen werden.");
        }

        return stats;
    }

    public static void broadcastLog(String msg) {
        for (WebSocket conn : connections) {
            if (conn.isOpen()) {
                conn.send("[LOG] " + msg);
            }
        }
    }

    public static String getCurrentMotd() {
        return currentMotd;
    }
}
