package de.sync.cloud.commands;

import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CLOUD_Lobby extends Command {

    public CLOUD_Lobby(String name) {
        super(name);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§cNur Spieler können diesen Befehl verwenden."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        // Hole verfügbare Lobbys
        List<String> lobbies = getLobbies();
        if (lobbies.isEmpty()) {
            player.sendMessage(new TextComponent("§cEs konnte keine Lobby gefunden werden."));
            return;
        }

        // Wenn der Spieler schon in einer Lobby ist
        if (lobbies.contains(player.getServer().getInfo().getName())) {
            player.sendMessage(new TextComponent("§eDu bist bereits in einer Lobby."));
            return;
        }

        // Wähle zufällige Lobby
        String selectedLobby = lobbies.get(new Random().nextInt(lobbies.size()));
        ServerInfo target = ProxyServer.getInstance().getServerInfo(selectedLobby);

        if (target != null) {
            player.connect(target);
            player.sendMessage(new TextComponent("§aDu wirst §averbunden..."));
        } else {
            player.sendMessage(new TextComponent("§cFehler beim Verbinden zur Lobby."));
        }
    }

    public static List<String> getLobbies() {
        List<String> availableLobbies = new ArrayList<>();
        String listResponse = BungeeCloudBridge.CloudCommand.sendListServers();
        if (listResponse == null || listResponse.isEmpty()) {
            return availableLobbies;
        }

        String[] servers = listResponse.split(",");
        for (String server : servers) {
            server = server.trim();
            if (server.equalsIgnoreCase("Lobby") || server.matches("(?i)Lobby(-\\d+)?")) {
                if (ProxyServer.getInstance().getServerInfo(server) != null) {
                    availableLobbies.add(server);
                }
            }
        }
        return availableLobbies;
    }
}
