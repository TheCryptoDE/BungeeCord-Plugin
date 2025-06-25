package de.sync.cloud.infomation;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;

import java.io.*;

public class ServerInfoProvider implements Listener {

    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase("SignSystem")) return;

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()));
            String subchannel = in.readUTF();

            if (subchannel.equals("RequestMaxPlayers")) {
                String serverName = in.readUTF();

                ServerInfo info = ProxyServer.getInstance().getServerInfo(serverName);
                if (info != null) {
                    int maxPlayers = info.getPlayers().size(); // <-- eigentlich keine echte MaxPlayer Info
                    // Workaround: Du musst die MaxPlayers manuell konfigurieren (z.B. aus Config) ODER:
                    maxPlayers = 20; // hier hart hinterlegt, besser später aus config laden

                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(b);
                    out.writeUTF("ResponseMaxPlayers");
                    out.writeUTF(serverName);
                    out.writeInt(maxPlayers);

                    if (event.getReceiver() instanceof ProxiedPlayer player) {
                        player.sendData("SignSystem", b.toByteArray());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
