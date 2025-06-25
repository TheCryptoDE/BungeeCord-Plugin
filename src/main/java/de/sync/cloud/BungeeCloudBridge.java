package de.sync.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sync.cloud.commands.CLOUD_Lobby;
import de.sync.cloud.infomation.BungeePluginMessageListener;
import de.sync.cloud.infomation.ServerInfoProvider;
import de.sync.cloud.listener.CloudSignBungeeListener;
import de.sync.cloud.listener.ConnectEvent;
import de.sync.cloud.tab.TablistHandler;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Map;

import static de.sync.cloud.print.PrintInfo.PREFIX;
import static de.sync.cloud.print.PrintInfo.sendHelp;


public class BungeeCloudBridge extends Plugin implements Listener {

    private static final String AUTH_PASSWORD = "supersecret"; // MUSS gleich sein wie CloudSystem
    private static final String CLOUD_HOST = "127.0.0.1";
    private static final int CLOUD_PORT = 9100;
    private static final String LOBBY_SERVER = "lobby-1";

    private final Gson gson = new Gson();
    private String motd = "§cMOTD nicht geladen!";
    private final File serviceFile = new File("../../../../service.json");

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerListener(this, new TablistHandler());
        getProxy().getPluginManager().registerListener(this, new ConnectEvent());
        getProxy().registerChannel("SignSystem");
        getProxy().registerChannel("BungeeCord");
        getProxy().getPluginManager().registerListener(this, new BungeePluginMessageListener());
        getProxy().getPluginManager().registerListener(this, new ServerInfoProvider());
        new CloudSignBungeeListener(this);
        getLogger().info("BungeeCloudPlugin aktiviert!");
        getProxy().getPluginManager().registerCommand(this, new CloudCommand());
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("lobby"));
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("hub"));
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("l"));
        loadMotdFromCloud();

        CloudWebSocketServer server = new CloudWebSocketServer(1234);
        server.start();

        loadAndStartMinServers();
        Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
        getLogger().info("Registered servers:");
        for (Map.Entry<String, ServerInfo> entry : servers.entrySet()) {
            String name = entry.getKey();
            ServerInfo info = entry.getValue();
            getLogger().info("- " + name + " -> " + info.getAddress().toString());
        }
    }
    private void loadMotdFromCloud() {
        JsonObject request = new JsonObject();
        request.addProperty("type", "GET_MOTD");

        String response = sendRequest(request);
        if (response != null && !response.isEmpty()) {
            motd = response;
            getLogger().info("MOTD von Cloud geladen: " + motd);
            saveMotdToFile(motd);
        } else {
            getLogger().warning("Fehler beim Laden der MOTD von der Cloud, versuche aus service.json");
            motd = loadMotdFromFile();
        }
    }

    private void saveMotdToFile(String motd) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("motd", motd);

            if (!serviceFile.exists()) {
                serviceFile.createNewFile();
            }

            try (FileWriter writer = new FileWriter(serviceFile)) {
                gson.toJson(json, writer);
                getLogger().info("MOTD in service.json gespeichert.");
            }
        } catch (IOException e) {
            getLogger().warning("Fehler beim Speichern der MOTD in service.json: " + e.getMessage());
        }
    }

    private String loadMotdFromFile() {
        if (!serviceFile.exists()) {
            getLogger().info("service.json existiert nicht, erstelle mit Standard-MOTD");
            saveMotdToFile("§aWillkommen auf dem Server!");
            return "§aWillkommen auf dem Server!";
        }

        try (FileReader reader = new FileReader(serviceFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("motd")) {
                return json.get("motd").getAsString();
            } else {
                getLogger().warning("Keine MOTD in service.json gefunden, setze Standard-MOTD");
                saveMotdToFile("§aWillkommen auf dem Server!");
                return "§aWillkommen auf dem Server!";
            }
        } catch (IOException e) {
            getLogger().warning("Fehler beim Lesen der service.json: " + e.getMessage());
            return "§cFehler beim Laden der MOTD";
        }
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        ping.setDescriptionComponent(new TextComponent(motd));
        event.setResponse(ping);
    }

    private String sendRequest(JsonObject request) {
        // Ergänzt: Timeout und besseres Logging
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(CLOUD_HOST, CLOUD_PORT), 3000); // 3 Sekunden Timeout

            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.write("AUTH " + AUTH_PASSWORD + "\n");
            out.flush();
            String authResp = in.readLine();
            if (!"AUTH_OK".equals(authResp)) {
                getLogger().warning("Auth fehlgeschlagen. Antwort: " + authResp);
                return null;
            }

            out.write(request.toString() + "\n");
            out.flush();

            String response = in.readLine();
            if (response == null) {
                getLogger().warning("Keine Antwort vom Cloud-System erhalten.");
                return null;
            }
            return response;

        } catch (IOException e) {
            getLogger().warning("Fehler bei der Verbindung zum Cloud-System: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return null;
        }
    }

    public class CloudCommand extends Command {

        public CloudCommand() {
            super("cloud", "cloud.admin");
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                sendHelp(sender);
                return;
            }

            String cmd = args[0].toLowerCase();
            try {
                switch (cmd) {
                    case "startserver" -> handleStartServer(sender, args);
                    case "stopserver" -> handleStopServer(sender, args);
                    case "stopall" -> handleStopAll(sender);
                    case "listservers" -> handleListServers(sender);
                    case "statusserver" -> handleStatusServer(sender, args);
                    case "help" -> sendHelp(sender);
                    default -> sendError(sender, "Unbekannter Befehl. Benutze §b/cloud help§r für Hilfe.");
                }
            } catch (Exception e) {
                sendError(sender, "Ein Fehler ist aufgetreten: §c" + e.getMessage());
                getLogger().warning("Fehler beim Ausführen von /cloud " + cmd + ": " + e.getMessage());
            }
        }



        private void sendError(CommandSender sender, String message) {
            sender.sendMessage(new TextComponent(PREFIX + "§c" + message));
        }

        private void sendSuccess(CommandSender sender, String message) {
            sender.sendMessage(new TextComponent(PREFIX + "§a" + message));
        }

        private void handleStartServer(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sendError(sender, "Benutzung: /cloud startserver <name> [template] [proxy:true|false]");
                return;
            }
            String serverName = args[1];
            String template = args.length >= 3 ? args[2] : "default";
            boolean proxy = args.length >= 4 && Boolean.parseBoolean(args[3]);

            String response = sendStartServerAndGetResponse(serverName, template, proxy);
            if (response != null && response.startsWith("SERVER_STARTED")) {
                sendSuccess(sender, "Starte Server §b" + serverName + "§a mit Template §b" + template + (proxy ? " §aals Proxy" : "") + ".");
                int port = extractPortFromResponse(response);
                if (port > 0 && !proxy) {
                    addServerToProxy(serverName, port);
                    sendSuccess(sender, "Server §b" + serverName + " §awurde zum Proxy auf Port §b" + port + " §ahinzugefügt.");

                    // ✅ HIER: Serverstart in MySQL loggen
                    logServerStartToMySQL(serverName, template);
                }
                sendSeparator(sender);
            } else {
                sendError(sender, "Fehler beim Starten des Servers.");
            }
        }


        private void logServerStartToMySQL(String serverName, String template) {
            File baseDir = getDataFolder().getAbsoluteFile();
            File mysqlFile = baseDir.toPath()
                    .getParent().getParent().getParent().getParent()
                    .resolve("mysql.json")
                    .toFile();

            if (!mysqlFile.exists()) {
                getLogger().warning("mysql.json nicht gefunden, kann Start nicht loggen.");
                return;
            }

            try (FileReader reader = new FileReader(mysqlFile)) {
                JsonObject mysqlConfig = JsonParser.parseReader(reader).getAsJsonObject();

                String host = mysqlConfig.get("host").getAsString();
                int port = mysqlConfig.get("port").getAsInt();
                String database = mysqlConfig.get("database").getAsString();
                String user = mysqlConfig.get("user").getAsString();
                String password = mysqlConfig.get("password").getAsString();

                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

                try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
                    String sql = "INSERT INTO started_servers (server_name, template, started_at) VALUES (?, ?, NOW())";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setString(1, serverName);
                        stmt.setString(2, template);
                        stmt.executeUpdate();
                        getLogger().info("Serverstart in MySQL geloggt: " + serverName);
                    }
                } catch (SQLException e) {
                    getLogger().warning("MySQL-Fehler beim Loggen des Serverstarts: " + e.getMessage());
                }
            } catch (Exception e) {
                getLogger().warning("Fehler beim Lesen der mysql.json: " + e.getMessage());
            }
        }




        public static int getFreePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            }
        }
        private void handleStopServer(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sendError(sender, "Benutzung: /cloud stopserver <name>");
                return;
            }
            String serverName = args[1];
            if (sendStopServer(serverName)) {
                sendSuccess(sender, "Server §b" + serverName + " §awird gestoppt.");
                removeServerFromProxy(serverName);
                removeServerFromMySQL(serverName);  // <---- Hier entferne aus DB
                sendSeparator(sender);
            } else {
                sendError(sender, "Fehler beim Stoppen des Servers.");
            }
        }

        private void removeServerFromMySQL(String serverName) {
            File baseDir = getDataFolder().getAbsoluteFile();
            File mysqlFile = baseDir.toPath()
                    .getParent().getParent().getParent().getParent()
                    .resolve("mysql.json")
                    .toFile();

            if (!mysqlFile.exists()) {
                getLogger().warning("mysql.json nicht gefunden, kann Stop nicht loggen.");
                return;
            }

            try (FileReader reader = new FileReader(mysqlFile)) {
                JsonObject mysqlConfig = JsonParser.parseReader(reader).getAsJsonObject();

                String host = mysqlConfig.get("host").getAsString();
                int port = mysqlConfig.get("port").getAsInt();
                String database = mysqlConfig.get("database").getAsString();
                String user = mysqlConfig.get("user").getAsString();
                String password = mysqlConfig.get("password").getAsString();

                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

                try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
                    String sql = "DELETE FROM started_servers WHERE server_name = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setString(1, serverName);
                        int affectedRows = stmt.executeUpdate();
                        if (affectedRows > 0) {
                            getLogger().info("Serverstop in MySQL geloggt (entfernt): " + serverName);
                        } else {
                            getLogger().info("Server " + serverName + " nicht in MySQL-Tabelle gefunden.");
                        }
                    }
                } catch (SQLException e) {
                    getLogger().warning("MySQL-Fehler beim Loggen des Serverstopps: " + e.getMessage());
                }
            } catch (Exception e) {
                getLogger().warning("Fehler beim Lesen der mysql.json: " + e.getMessage());
            }
        }


        private void handleStopAll(CommandSender sender) {
            String response = sendStopAllServers();
            if ("ALL_SERVERS_STOPPED".equals(response)) {
                sendSuccess(sender, "Alle Server werden gestoppt.");
                ProxyServer.getInstance().getServers().keySet().removeIf(server -> !server.equalsIgnoreCase(LOBBY_SERVER));
                getLogger().info("Alle Server außer Lobby wurden aus dem Proxy entfernt.");
                sendSeparator(sender);
            } else {
                sendError(sender, "Fehler beim Stoppen aller Server.");
            }
        }

        private void handleListServers(CommandSender sender) {
            String list = sendListServers();
            sender.sendMessage(new TextComponent(PREFIX + "§bLaufende Server:"));
            if (list.isBlank() || "Keine Server gefunden".equalsIgnoreCase(list)) {
                sender.sendMessage(new TextComponent(" §7(Keine Server gefunden)"));
            } else {
                String[] servers = list.split(",");
                for (String srv : servers) {
                    sender.sendMessage(new TextComponent(" §e- §b" + srv.trim()));
                }
            }
            sendSeparator(sender);
        }

        private void handleStatusServer(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sendError(sender, "Benutzung: /cloud statusserver <name>");
                return;
            }
            String serverName = args[1];
            String status = sendServerStatus(serverName);
            if (status != null) {
                sendSuccess(sender, "Status von Server §b" + serverName + ": §e" + status);
                sendSeparator(sender);
            } else {
                sendError(sender, "Fehler beim Abrufen des Serverstatus.");
            }
        }

        private void sendSeparator(CommandSender sender) {
            sender.sendMessage(new TextComponent("§8§m--------------------------------------"));
        }

        // --- Hilfsmethoden für Serverkommunikation ---

        private String sendStartServerAndGetResponse(String name, String template, boolean proxy) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "START_SERVER");
            json.addProperty("serverName", name);
            json.addProperty("template", template);
            json.addProperty("proxy", proxy);
            return sendRequest(json);
        }

        private boolean sendStopServer(String name) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "STOP_SERVER");
            json.addProperty("serverName", name);
            String resp = sendRequest(json);
            return resp != null && resp.startsWith("SERVER_STOPPED");
        }

        private String sendStopAllServers() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "STOP_ALL_SERVERS");
            return sendRequest(json);
        }

        public static String sendListServers() {
            JsonObject json = new JsonObject();
            json.addProperty("type", "LIST_SERVERS");
            String response = sendRequest(json);
            if (response != null && response.startsWith("SERVERS ")) {
                return response.substring(8);
            }
            return "Keine Server gefunden";
        }

        private String sendServerStatus(String serverName) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "SERVER_STATUS");
            json.addProperty("serverName", serverName);
            return sendRequest(json);
        }




        private static String sendRequest(JsonObject request) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(CLOUD_HOST, CLOUD_PORT), 3000); // 3 Sekunden Timeout

                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.write("AUTH " + AUTH_PASSWORD + "\n");
                out.flush();
                String authResp = in.readLine();
                if (!"AUTH_OK".equals(authResp)) {
                  //  getLogger().warning("Auth fehlgeschlagen.");
                    return null;
                }

                out.write(request.toString() + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null) {
                 //   getLogger().warning("Keine Antwort vom Cloud-System erhalten.");
                    return null;
                }
                return response;

            } catch (IOException e) {
            //    getLogger().warning("Fehler bei der Verbindung zum Cloud-System: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                return null;
            }
        }



        private int extractPortFromResponse(String response) {
            try {
                String[] parts = response.split(" ");
                for (int i = 0; i < parts.length; i++) {
                    if ("PORT".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                        return Integer.parseInt(parts[i + 1]);
                    }
                }
            } catch (Exception ignored) {}
            return -1;
        }

        private void addServerToProxy(String serverName, int port) {
            ServerInfo serverInfo = ProxyServer.getInstance().constructServerInfo(
                    serverName,
                    new InetSocketAddress("127.0.0.1", port),
                    "Dynamisch hinzugefügter Server",
                    false
            );
            ProxyServer.getInstance().getServers().put(serverName, serverInfo);
            getLogger().info("Server " + serverName + " dynamisch zum Proxy hinzugefügt.");
        }

        private void removeServerFromProxy(String serverName) {
            if (ProxyServer.getInstance().getServers().remove(serverName) != null) {
                getLogger().info("Server " + serverName + " vom Proxy entfernt.");
            }
        }
    }

    private void loadAndStartMinServers() {
        File baseDir = getDataFolder().getAbsoluteFile();
        File mysqlFile = baseDir.toPath()
                .getParent().getParent().getParent().getParent()
                .resolve("mysql.json")
                .toFile();

        if (!mysqlFile.exists()) {
            getLogger().severe("mysql.json wurde nicht gefunden unter: " + mysqlFile.getAbsolutePath());
            return;
        }

        try (FileReader reader = new FileReader(mysqlFile)) {
            JsonObject mysqlConfig = JsonParser.parseReader(reader).getAsJsonObject();

            String host = mysqlConfig.get("host").getAsString();
            int port = mysqlConfig.get("port").getAsInt();
            String database = mysqlConfig.get("database").getAsString();
            String user = mysqlConfig.get("user").getAsString();
            String password = mysqlConfig.get("password").getAsString();

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";

            try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password)) {
                String sql = "SELECT group_name, min_server FROM groups";
                try (PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {

                    while (resultSet.next()) {
                        String groupName = resultSet.getString("group_name");
                        int minServer = resultSet.getInt("min_server");

                        if (minServer <= 0) continue;

                        for (int i = 1; i <= minServer; i++) {
                            String serverName = groupName + "-" + i;
                            String command = "cloud startserver " + serverName + " " + groupName + " false";

                            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                                    ProxyServer.getInstance().getConsole(), command
                            );

                            getLogger().info("Automatisch gestartet: " + command);
                        }
                    }
                }
            } catch (SQLException e) {
                getLogger().severe("Fehler beim Abrufen der Gruppen aus MySQL: " + e.getMessage());
            }
        } catch (Exception e) {
            getLogger().severe("Fehler beim Lesen der mysql.json: " + e.getMessage());
        }
    }

}
