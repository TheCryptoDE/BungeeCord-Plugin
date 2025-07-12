package de.sync.cloud.listener;

import de.sync.cloud.commands.CLOUD_Lobby;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.List;
import java.util.Random;

public class KickRedirectListener implements Listener {

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // Hole alle verfügbaren Lobbys
        List<String> lobbies = CLOUD_Lobby.getLobbies();

        // Keine Lobby gefunden – kein Redirect möglich
        if (lobbies.isEmpty()) {
            return;
        }

        // Spieler ist bereits in einer Lobby? Dann nicht umleiten (optional)
        String currentServer = player.getServer() != null ? player.getServer().getInfo().getName() : "";
        if (lobbies.contains(currentServer)) {
            return;
        }

        // Wähle zufällige Lobby
        String selectedLobby = lobbies.get(new Random().nextInt(lobbies.size()));
        ServerInfo targetLobby = ProxyServer.getInstance().getServerInfo(selectedLobby);

        if (targetLobby == null) {
            return;
        }

        // Kick abbrechen & Spieler zur Lobby senden
        event.setCancelled(true);
        event.setCancelServer(targetLobby);

        // Optional: Kickgrund anzeigen
        String reason = TextComponent.toLegacyText(event.getKickReasonComponent());

        player.sendMessage(new TextComponent("§eDu wurdest zur §a" + selectedLobby + " §eumgeleitet."));
        if (!reason.isEmpty()) {
            player.sendMessage(new TextComponent("§7Grund: §c" + reason));
        }
    }
}
