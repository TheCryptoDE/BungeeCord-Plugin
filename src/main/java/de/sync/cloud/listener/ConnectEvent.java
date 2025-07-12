package de.sync.cloud.listener;

import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectEvent implements Listener {

    private static final int MAX_PLAYERS_PER_LOBBY = 20;
    private static final int SERVER_START_COOLDOWN_SECONDS = 60;
    private long lastServerStartTimestamp = 0;

    // Zähler für Round-Robin
    private final AtomicInteger counter = new AtomicInteger(0);

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // Nur beim ersten Verbinden (kein Server zugewiesen)
        if (player.getServer() != null) {
            return;
        }

        ProxyServer proxy = ProxyServer.getInstance();

        List<String> lobbies = getLobbies();

        if (lobbies.isEmpty()) {
            player.disconnect(new TextComponent("§cNo lobbies available."));
            event.setCancelled(true);
            return;
        }

        int index = counter.getAndIncrement() % lobbies.size();
        String targetLobbyName = lobbies.get(index);
        ServerInfo targetLobby = proxy.getServerInfo(targetLobbyName);

        if (targetLobby == null) {
            player.disconnect(new TextComponent("§cLobby " + targetLobbyName + " is not reachable."));
            event.setCancelled(true);
            return;
        }

        int playerCount = targetLobby.getPlayers().size();

        // Wenn die Lobby voll ist, neuen Lobby-Server starten
        if (playerCount >= MAX_PLAYERS_PER_LOBBY) {
            tryStartNewLobby();
            // Nach dem Starten die aktuelle Lobby umgehen und neue Lobby erneut abrufen
            lobbies = getLobbies();

            if (lobbies.isEmpty()) {
                player.disconnect(new TextComponent("§cNo lobbies available."));
                event.setCancelled(true);
                return;
            }

            // Neuen Index nach Startserver anpassen
            index = counter.getAndIncrement() % lobbies.size();
            targetLobbyName = lobbies.get(index);
            targetLobby = proxy.getServerInfo(targetLobbyName);

            if (targetLobby == null) {
                player.disconnect(new TextComponent("§cLobby " + targetLobbyName + " is not reachable."));
                event.setCancelled(true);
                return;
            }
        }

        System.out.println("§aPlayer " + player.getName() + " is being sent to lobby §b" + targetLobbyName + "§a.");
        event.setTarget(targetLobby);
    }

    private void tryStartNewLobby() {
        long now = System.currentTimeMillis();
        if (now - lastServerStartTimestamp < SERVER_START_COOLDOWN_SECONDS * 1000) {
            System.out.println("Lobby start denied due to cooldown.");
            return;
        }
        lastServerStartTimestamp = now;

        String newLobbyName = generateNewLobbyName();
        ProxyServer proxy = ProxyServer.getInstance();

        System.out.println("Attempting to start new lobby server: " + newLobbyName);

        boolean commandSuccess = proxy.getPluginManager().dispatchCommand(proxy.getConsole(), "cloud startserver " + newLobbyName + " lobby false");
        System.out.println("Startserver Befehl ausgeführt: " + commandSuccess);

        String message = "§8[§b§lCloud§8] §7A new lobby has been started: §b" + newLobbyName;
        proxy.getPlayers().forEach(p -> p.sendMessage(new TextComponent(message)));
    }

    private String generateNewLobbyName() {
        List<String> lobbies = getLobbies();
        int highestNumber = 1; // Starte bei Lobby-1

        for (String lobby : lobbies) {
            if (lobby.matches("(?i)Lobby-(\\d+)")) {
                try {
                    int num = Integer.parseInt(lobby.split("-")[1]);
                    if (num > highestNumber) highestNumber = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return "Lobby-" + (highestNumber + 1);
    }

    public static List<String> getLobbies() {
        List<String> availableLobbies = new ArrayList<>();
        ProxyServer proxy = ProxyServer.getInstance();

        String listResponse = BungeeCloudBridge.CloudCommand.sendListServers();

        System.out.println("[DEBUG] ListServers Antwort: " + listResponse);

        if (listResponse == null || listResponse.isEmpty()) return availableLobbies;

        String[] servers = listResponse.split(",");
        for (String server : servers) {
            server = server.trim();
            if (server.matches("(?i)Lobby(-\\d+)?")) {
                ServerInfo info = proxy.getServerInfo(server);
                if (info != null && info.canAccess(proxy.getConsole()) && proxy.getServers().containsKey(server)) {
                    System.out.println("[DEBUG] Lobby hinzugefügt: " + server);
                    availableLobbies.add(server);
                } else {
                    System.out.println("[DEBUG] Lobby " + server + " existiert nicht oder ist nicht erreichbar.");
                }
            }
        }
        System.out.println("[DEBUG] Gefundene Lobbys: " + availableLobbies);
        return availableLobbies;
    }
}
