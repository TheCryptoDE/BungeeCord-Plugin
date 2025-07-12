package de.sync.cloud.infomation;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class CloudCommandReceiver  implements Listener {

    // PluginMessageListener.java (BungeeCord)

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase("BungeeCord")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subchannel = in.readUTF();

        if (subchannel.equalsIgnoreCase("StopServer")) {
            String serverName = in.readUTF();
            ProxyServer.getInstance().getLogger().info("StopServer Befehl erhalten für Server: " + serverName);

            // Hier dann z.B. den Stopserver-Befehl ausführen:
            ProxyServer.getInstance().getPluginManager().dispatchCommand(ProxyServer.getInstance().getConsole(), "cloud stopserver " + serverName);
        }
    }
}