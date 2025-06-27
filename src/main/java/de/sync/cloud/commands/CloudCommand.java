//package de.sync.cloud.commands;
//
//import de.sync.cloud.BungeeCloudBridge;
//import net.md_5.bungee.api.CommandSender;
//import net.md_5.bungee.api.chat.TextComponent;
//import net.md_5.bungee.api.plugin.Command;
//
//import static de.sync.cloud.print.PrintInfo.PREFIX;
//import static de.sync.cloud.print.PrintInfo.sendHelp;
//
//public class CloudCommand extends Command {
//    public CloudCommand() {
//        super("cloud", "cloud.admin");
//    }
//
//    @Override
//    public void execute(CommandSender sender, String[] args) {
//        if (args.length == 0) {
//            sendHelp(sender);
//            return;
//        }
//
//        String cmd = args[0].toLowerCase();
//        try {
//            switch (cmd) {
//                case "startserver" -> handleStartServer(sender, args);
//                case "stopserver" -> handleStopServer(sender, args);
//                case "stopall" -> handleStopAll(sender);
//                case "listservers" -> handleListServers(sender);
//                case "statusserver" -> handleStatusServer(sender, args);
//                case "help" -> sendHelp(sender);
//                default -> sendError(sender, "Unbekannter Befehl. Benutze §b/cloud help§r für Hilfe.");
//            }
//        } catch (Exception e) {
//            sendError(sender, "Ein Fehler ist aufgetreten: §c" + e.getMessage());
//            getLogger().warning("Fehler beim Ausführen von /cloud " + cmd + ": " + e.getMessage());
//        }
//    }
//
//    private void sendError(CommandSender sender, String message) {
//        sender.sendMessage(new TextComponent(PREFIX + "§c" + message));
//    }
//
//    private void sendSuccess(CommandSender sender, String message) {
//        sender.sendMessage(new TextComponent(PREFIX + "§a" + message));
//    }
//
//    private void handleStartServer(CommandSender sender, String[] args) {
//        if (args.length < 2) {
//            sendError(sender, "Benutzung: /cloud startserver <name> [template] [proxy:true|false]");
//            return;
//        }
//        String serverName = args[1];
//        String template = args.length >= 3 ? args[2] : "default";
//        boolean proxy = args.length >= 4 && Boolean.parseBoolean(args[3]);
//
//        String response = sendStartServerAndGetResponse(serverName, template, proxy);
//        if (response != null && response.startsWith("SERVER_STARTED")) {
//            sendSuccess(sender, "Starte Server §b" + serverName + "§a mit Template §b" + template + (proxy ? " §aals Proxy" : "") + ".");
//            int port = extractPortFromResponse(response);
//            if (port > 0 && !proxy) {
//                addServerToProxy(serverName, port);
//                sendSuccess(sender, "Server §b" + serverName + " §awurde zum Proxy auf Port §b" + port + " §ahinzugefügt.");
//                logServerStartToMySQL(serverName, template);
//            }
//            sendSeparator(sender);
//        } else {
//            sendError(sender, "Fehler beim Starten des Servers.");
//        }
//    }
//}