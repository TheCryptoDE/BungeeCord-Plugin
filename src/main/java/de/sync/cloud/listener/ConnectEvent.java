package de.sync.cloud.listener;

import de.sync.cloud.BungeeCloudBridge;
import de.sync.cloud.random.RandomLobbyConnect;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class ConnectEvent implements Listener {




    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();


        if (event.getTarget() != null && event.getTarget().getName().equalsIgnoreCase("lobby")){
            List<String> lobbies = getLobbies();
            if (lobbies.isEmpty()){
                player.disconnect("§bEs konnte kein Lobby gefunden werden.");
                event.setCancelled(true);
                return;
            }

            String selectedLobby = lobbies.get(new Random().nextInt(lobbies.size()));
            ServerInfo target = ProxyServer.getInstance().getServerInfo(selectedLobby);

            if (target != null){
                event.setTarget(target);
                System.out.println("§aDu wurdest zu §b" + selectedLobby + " §asenden.");
            }else {
                player.disconnect("§bEs konnte kein Lobby gefunden werden.");
                event.setCancelled(true);
            }
        }

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



//  @EventHandler
//  public void onConnection(PostLoginEvent event){
//      event.getPlayer().connect(RandomLobbyConnect.connectToRandomLobby(event.getPlayer()));
//  }

//  @EventHandler
//  public void onConnec(ServerConnectEvent event){
//      RandomLobbyConnect.connectToRandomLobby(event.getPlayer());
//      event.getPlayer().connect(RandomLobbyConnect.connectToRandomLobby(event.getPlayer()));
//  }

}
