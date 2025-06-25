package de.sync.cloud.infomation;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;

public class BungeePluginMessageListener implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase("BungeeCord")) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String subchannel = in.readUTF();

            if (subchannel.equalsIgnoreCase("PlayerCount")) {
                String serverName = in.readUTF();

                int count = 0;
                if (serverName.equalsIgnoreCase("ALL")) {
                    count = ProxyServer.getInstance().getPlayers().size();
                } else if (ProxyServer.getInstance().getServerInfo(serverName) != null) {
                    count = ProxyServer.getInstance().getServerInfo(serverName).getPlayers().size();
                }

                // Nun Antwort zurücksenden
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(byteOut);
                out.writeUTF("PlayerCount");
                out.writeUTF(serverName);
                out.writeInt(count);

                // Jetzt an den ursprünglichen Spieler zurücksenden
                if (event.getReceiver() instanceof ProxiedPlayer player) {
                    player.getServer().sendData("BungeeCord", byteOut.toByteArray());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
