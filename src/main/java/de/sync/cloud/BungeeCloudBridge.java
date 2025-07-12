package de.sync.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.sync.cloud.commands.CLOUD_Lobby;
import de.sync.cloud.commands.SetSlotCommand;
import de.sync.cloud.infomation.BungeePluginMessageListener;
import de.sync.cloud.infomation.CloudCommandReceiver;
import de.sync.cloud.infomation.ServerInfoProvider;
import de.sync.cloud.listener.*;
import de.sync.cloud.permissionsystem.PermissionManager;
import de.sync.cloud.tab.TablistHandler;
import de.sync.cloud.tab.test;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static de.sync.cloud.print.PrintInfo.PREFIX;
import static de.sync.cloud.print.PrintInfo.sendHelp;


public class BungeeCloudBridge extends Plugin implements Listener {


    private PermissionManager permissionManager;

    private static BungeeCloudBridge instance;




    private static final String AUTH_PASSWORD = "supersecret"; // MUSS gleich sein wie CloudSystem
    private static final String CLOUD_HOST = "127.0.0.1";
    private static final int CLOUD_PORT = 9100;
    private static final String LOBBY_SERVER = "Lobby-1";

    private final Gson gson = new Gson();
    private String motd = "§cMOTD nicht geladen!";
    private final File serviceFile = new File("../../../../service.json");

    @Override
    public void onEnable() {
        this.permissionManager = new PermissionManager(this);
        this.permissionManager.reload();
        startAutoUpdate();
        instance = this;
        TablistHandler.startMaxPlayersUpdater();
        getProxy().getPluginManager().registerListener(this, new LoginListener(permissionManager));
        getProxy().getPluginManager().registerListener(this, this);
        setInitialMaxPlayers();
        getProxy().getPluginManager().registerListener(this, new TablistHandler(this));
        getProxy().getPluginManager().registerListener(this, new ConnectEvent());
        getProxy().getPluginManager().registerListener(this, new KickRedirectListener());
        getProxy().registerChannel("SignSystem");
        getProxy().registerChannel("BungeeCord");
        getProxy().getPluginManager().registerListener(this, new CloudCommandReceiver());

        System.out.println("[DEBUG] PluginMessageListener registered");
        getProxy().getPluginManager().registerListener(this, new BungeePluginMessageListener());
        getProxy().getPluginManager().registerListener(this, new ServerInfoProvider());
        getProxy().getPluginManager().registerListener(this, new ServerListener());
        getProxy().getPluginManager().registerListener(this, new test());
        new CloudSignBungeeListener(this);
        getLogger().info("BungeeCloudPlugin aktiviert!");
        getProxy().getPluginManager().registerCommand(this, new CloudCommand());
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("lobby"));
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("hub"));
        getProxy().getPluginManager().registerCommand(this, new CLOUD_Lobby("l"));
        getProxy().getPluginManager().registerCommand(this, new SetSlotCommand(this));
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



    private void setInitialMaxPlayers() {
        try {
            File mysqlFile = getDataFolder().toPath()
                    .getParent().getParent().getParent().getParent()
                    .resolve("mysql.json").toFile();

            if (!mysqlFile.exists()) {
                getLogger().warning("mysql.json nicht gefunden!");
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

                // 1. Tabelle erstellen, falls sie nicht existiert
                String createTable = "CREATE TABLE IF NOT EXISTS maxslots (" +
                        "proxy_name VARCHAR(255) PRIMARY KEY," +
                        "slots INT NOT NULL," +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")";
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(createTable);
                }

                // 2. Standardwert setzen oder aktualisieren
                String proxyName = getProxy().getName(); // z.B. "Proxy-1"
                int initialSlots = 65;

                String insertOrUpdate = "INSERT INTO maxslots (proxy_name, slots) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE slots = VALUES(slots), timestamp = NOW()";

                try (PreparedStatement stmt = conn.prepareStatement(insertOrUpdate)) {
                    stmt.setString(1, proxyName);
                    stmt.setInt(2, initialSlots);
                    stmt.executeUpdate();
                    getLogger().info("MaxPlayers in MySQL gesetzt: " + initialSlots);
                }
            }

        } catch (Exception e) {
            getLogger().warning("Fehler beim Setzen der initialen MaxPlayers: " + e.getMessage());
        }
    }



    public static BungeeCloudBridge getInstance() {
        return instance;
    }



    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    private void startAutoUpdate() {
        ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            try {
                permissionManager.reload();
                getLogger().info("Permissions automatisch neu geladen.");
            } catch (Exception e) {
                getLogger().warning("Fehler beim automatischen Reload: " + e.getMessage());
            }
        }, 10L, 10L, TimeUnit.SECONDS); // alle 60 Sekunden
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
                    case "startservice" -> handleStartService(sender, args);
                    case "stopserver" -> handleStopServer(sender, args);
                    case "stopall" -> handleStopAll(sender);
                    case "listservers" -> handleListServers(sender);
                    case "statusserver" -> handleStatusServer(sender, args);
                    case "set" -> handleSetCommand(sender, args);

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
                sendError(sender, "useage: /cloud startserver <name> [template] [proxy:true|false]");
                return;
            }
            String serverName = args[1];
            String template = args.length >= 3 ? args[2] : "default";
            boolean proxy = args.length >= 4 && Boolean.parseBoolean(args[3]);

            String response = sendStartServerAndGetResponse(serverName, template, proxy);
            if (response != null && response.startsWith("SERVER_STARTED")) {
                sendSuccess(sender, "Starting server §b" + serverName + "§a with template §b" + template + (proxy ? " §aas a proxy" : "") + ".");
                int port = extractPortFromResponse(response);
                if (port > 0 && !proxy) {
                    addServerToProxy(serverName, port);
                    sendSuccess(sender, "Server §b" + serverName + " §ahas been added as a proxy on port §b" + port + "§a.");
                    logServerStartToMySQL(serverName, template);
                }
                sendSeparator(sender);
            } else {
                sendError(sender, "Error while starting the server.");
            }
        }

        private void handleStartService(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sendError(sender, "usage: /cloud startservice <name> [count] [template] [proxy:true|false]");
                return;
            }

            String baseName = args[1];
            int count = 1;
            String template = "default";
            boolean proxy = false;

            if (args.length >= 3) {
                try {
                    count = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    template = args[2];
                }
            }

            if (args.length >= 4) {
                if (template.equals("default")) {
                    template = args[3];
                } else {
                    proxy = Boolean.parseBoolean(args[3]);
                }
            }

            if (args.length >= 5) {
                proxy = Boolean.parseBoolean(args[4]);
            }

            int started = 0;
            int index = 1;

            while (started < count) {
                String serverName = baseName + "-" + index;

                if (isServerRegisteredInProxy(serverName)) {
                    index++;
                    continue; // Skip already registered server
                }

                String response = sendStartServerAndGetResponse(serverName, template, proxy);
                if (response != null && response.startsWith("SERVER_STARTED")) {
                    sendSuccess(sender, "Starting server §b" + serverName + "§a with template §b" + template + (proxy ? " §aas a proxy" : "") + ".");
                    int port = extractPortFromResponse(response);
                    if (port > 0 && !proxy) {
                        addServerToProxy(serverName, port);
                        sendSuccess(sender, "Server §b" + serverName + " §ahas been added as a proxy on port §b" + port + "§a.");
                        logServerStartToMySQL(serverName, template);
                    }
                    sendSeparator(sender);
                    started++;
                } else {
                    sendError(sender, "Error while starting server §c" + serverName + "§r.");
                }

                index++; // Always increment to avoid duplicates
            }

        }
        private boolean isServerRegisteredInProxy(String serverName) {
            return ProxyServer.getInstance().getServers().containsKey(serverName.toLowerCase());
        }



        private void handleSetCommand(CommandSender sender, String[] args) {
            if (args.length >= 3 && args[1].equalsIgnoreCase("maintenance")) {
                boolean value;
                try {
                    value = Boolean.parseBoolean(args[2]);
                } catch (Exception e) {
                    sendError(sender, "Ungültiger Wert. Bitte benutze true oder false.");
                    return;
                }
                MaintenanceManager.setMaintenance(value);
                sendSuccess(sender, "Wartungsmodus wurde " + (value ? "§aaktiviert" : "§cdeaktiviert") + "§r.");
            } else {
                sendError(sender, "Benutze: /cloud set maintenance <true|false>");
            }
        }

        private void checkAndStartMinServersForGroup(String stoppedServerName) {
            // Gruppe herausfinden aus dem Servernamen: z.B. "lobby-2" → "lobby"
            String groupName = stoppedServerName.split("-")[0];

            File baseDir = getDataFolder().getAbsoluteFile();
            File mysqlFile = baseDir.toPath()
                    .getParent().getParent().getParent().getParent()
                    .resolve("mysql.json")
                    .toFile();

            if (!mysqlFile.exists()) {
                getLogger().warning("mysql.json nicht gefunden, kann min_server nicht prüfen.");
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
                    // 1. min_server auslesen
                    String queryMin = "SELECT min_server FROM groups WHERE group_name = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(queryMin)) {
                        stmt.setString(1, groupName);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int minServer = rs.getInt("min_server");

                                // 2. Online-Server zählen
                                long onlineCount = ProxyServer.getInstance().getServers().keySet().stream()
                                        .filter(name -> name.startsWith(groupName + "-"))
                                        .count();

                                int toStart = minServer - (int) onlineCount;

                                if (toStart > 0) {
                                    getLogger().info("Es fehlen " + toStart + " Server für Gruppe " + groupName + ", starte automatisch nach.");
                                    for (int i = 1; i <= 100; i++) {
                                        String serverCandidate = groupName + "-" + i;
                                        if (!ProxyServer.getInstance().getServers().containsKey(serverCandidate)) {
                                            String cmd = "cloud startserver " + serverCandidate + " " + groupName + " false";
                                            ProxyServer.getInstance().getPluginManager().dispatchCommand(
                                                    ProxyServer.getInstance().getConsole(), cmd
                                            );
                                            getLogger().info("Auto-start: " + serverCandidate);
                                            for (ProxiedPlayer  all : ProxyServer.getInstance().getPlayers()){
                                                if(all.hasPermission("*")){
                                                    all.sendMessage("§8[§b§lCloud§8] §a" + serverCandidate + " §7is now starting.");
                                                }
                                            }
                                            toStart--;
                                            if (toStart <= 0) break;
                                        }
                                    }
                                } else {
                                    getLogger().info("Enough servers online for group " + groupName + " (" + onlineCount + "/" + minServer + ")");
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().severe("Fehler bei min_server-Prüfung für " + stoppedServerName + ": " + e.getMessage());
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
        private void handleStopServer(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sendError(sender, "Benutzung: /cloud stopserver <name>");
                return;
            }
            String serverName = args[1];
            if (sendStopServer(serverName)) {
                for (ProxiedPlayer  all : ProxyServer.getInstance().getPlayers()) {
                    if (all.hasPermission("*")) {
                        all.sendMessage("§8[§b§lCloud§8] §a" + serverName + " §7is now stopping.");
                    }
                }
                removeServerFromProxy(serverName);
                removeServerFromMySQL(serverName);  // <---- Hier entferne aus DB
                checkAndStartMinServersForGroup(serverName);
                sendSeparator(sender);
            } else {
                sendError(sender, "Error while stopping the server.");
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
                sendSuccess(sender, "All servers are stopping.");
                ProxyServer.getInstance().getServers().keySet().removeIf(server -> !server.equalsIgnoreCase(LOBBY_SERVER));
                getLogger().info("Alle Server außer Lobby wurden aus dem Proxy entfernt.");
                sendSeparator(sender);
            } else {
                sendError(sender, "Error while stopping all servers.");
            }
        }

        private void handleListServers(CommandSender sender) {
            String list = sendListServers();
            sender.sendMessage(new TextComponent(PREFIX + "§bRunning servers:"));
            if (list.isBlank() || "No servers found".equalsIgnoreCase(list)) {
                sender.sendMessage(new TextComponent(" §7(No servers found)"));
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
                sendError(sender, "Usage: /cloud statusserver <name>");
                return;
            }
            String serverName = args[1];
            String status = sendServerStatus(serverName);
            if (status != null) {
                sendSuccess(sender, "Status of server §b" + serverName + ": §e" + status);
                sendSeparator(sender);
            } else {
                sendError(sender, "Error retrieving the server status.");
            }
        }

        private void sendSeparator(CommandSender sender) {
            sender.sendMessage(new TextComponent("§8§m--------------------------------------"));
        }

        // --- Hilfsmethoden für Serverkommunikation ---

        public static String sendStartServerAndGetResponse(String name, String template, boolean proxy) {
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
                    "Dynamically added server",
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
