package de.sync.cloud.listener;

import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.stream.Collectors;

public class CloudSignBungeeListener implements Listener {

    public CloudSignBungeeListener(BungeeCloudBridge plugin) {
        ProxyServer.getInstance().registerChannel("CloudSign");
        ProxyServer.getInstance().getPluginManager().registerListener(plugin, this);
    }

    @net.md_5.bungee.event.EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("CloudSign")) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String subChannel = in.readUTF();

            if (subChannel.equals("GetGroupServers")) {
                String group = in.readUTF();

                String result = ProxyServer.getInstance().getServers().values().stream()
                        .map(ServerInfo::getName)
                        .filter(name -> name.equalsIgnoreCase(group) || name.toLowerCase().startsWith(group.toLowerCase() + "-"))
                        .collect(Collectors.joining(","));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dataOut = new DataOutputStream(out);
                dataOut.writeUTF("GroupServersResponse");
                dataOut.writeUTF(group);
                dataOut.writeUTF(result);

                Server server = (Server) event.getSender();
                server.sendData("CloudSign", out.toByteArray());
            }
            if (subChannel.equals("GetAllServers")) {
                String allServers = ProxyServer.getInstance().getServers().values().stream()
                        .map(ServerInfo::getName)
                        .collect(Collectors.joining(","));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DataOutputStream dataOut = new DataOutputStream(out);
                dataOut.writeUTF("AllServersResponse");
                dataOut.writeUTF(allServers);

                Server server = (Server) event.getSender();
                server.sendData("CloudSign", out.toByteArray());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
