package de.sync.cloud.random;

import de.sync.cloud.BungeeCloudBridge;
import de.sync.cloud.print.PrintInfo;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerConnectRequest;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomLobbyConnect {

    private static final Random random = new Random();

    public static ServerInfo connectToRandomLobby(ProxiedPlayer player) {
        List<String> lobbies = getLobbies();
        if (lobbies.isEmpty()) {
            player.sendMessage(TextComponent.fromLegacyText(PrintInfo.PREFIX + "§cEs konnte kein Lobby gefunden werden."));
            return null;
        }

        String selectedLobby = lobbies.get(random.nextInt(lobbies.size()));
        player.sendMessage(TextComponent.fromLegacyText(PrintInfo.PREFIX + "§aDu wurdest zu §b" + selectedLobby + " §asenden."));
        ServerConnectRequest request = ServerConnectRequest.builder()
                .target(ProxyServer.getInstance().getServerInfo(selectedLobby))
                .retry(true)
                .build();

        player.connect((ServerInfo) request, new Callback<Boolean>() {
            @Override
            public void done(Boolean success, Throwable throwable) {
                if (!success) {
                    player.sendMessage(TextComponent.fromLegacyText(PrintInfo.PREFIX + "§cDu wurdest nicht zu §b" + selectedLobby + " §csenden."));

                }
            }
        });
        return null;
    }


    public static List<String> getLobbies() {
        List<String> availableLobbies = new ArrayList<>();
        String listResponse = BungeeCloudBridge.CloudCommand.sendListServers();
        if (listResponse == null || listResponse.isEmpty()){
            return availableLobbies;
        }

        String[] servers = listResponse.split(",");
        for (String server : servers){
            server = server.trim();
            if (server.equalsIgnoreCase("Lobby") || server.matches("(?i)Lobby(-\\d+)?")){
                if (ProxyServer.getInstance().getServerInfo(server) != null){
                    availableLobbies.add(server);
                }
            }
        }
        return availableLobbies;
    }
}
