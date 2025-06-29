package de.sync.cloud.tab;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.FileReader;

public class TablistHandler implements Listener {

    private final String rawHeader;
    private final String rawFooter;

    public TablistHandler() {
        // Gehe 5 Verzeichnisse zurück und dann in die Datei "service.json"
        File serviceFile = new File("service.json").getAbsoluteFile();

        System.out.println(   serviceFile.getAbsolutePath());

        serviceFile = serviceFile.getParentFile().getParentFile().getParentFile();
        serviceFile = new File(serviceFile, "service.json");

        System.out.println(   serviceFile.getAbsolutePath());

        String headerTemp = "\n &b&o■ &8┃ &3&lSyncCloud &8● &7Earthquake &8&l» &7&o%online_players%&8/&7&o%max_players% &8┃ &b&o■ \n &8► &7Current server &8● &b%server% &8◄ \n ";
        String footerTemp = "\n &7Discord &8&l» &bdiscord.synccloud.eu \n &7&oNext &3&l&oGeneration &7&oNetwork \n ";

        if (serviceFile.exists()) {
            try (FileReader reader = new FileReader(serviceFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("header")) {
                    headerTemp = json.get("header").getAsString();
                }
                if (json.has("footer")) {
                    footerTemp = json.get("footer").getAsString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("[SyncCloud] service.json wurde nicht gefunden unter: " + serviceFile.getPath());
        }

        this.rawHeader = headerTemp;
        this.rawFooter = footerTemp;
    }

    @EventHandler
    public void onSwitchServer(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();

        int onlinePlayers = ProxyServer.getInstance().getOnlineCount();
        int maxPlayers = ProxyServer.getInstance().getConfig().getPlayerLimit();
        String server = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";

        String headerFormatted = rawHeader
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%server%", server);

        String footerFormatted = rawFooter
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%server%", server);

        headerFormatted = ChatColor.translateAlternateColorCodes('&', headerFormatted);
        footerFormatted = ChatColor.translateAlternateColorCodes('&', footerFormatted);

        player.setTabHeader(
                new TextComponent(headerFormatted),
                new TextComponent(footerFormatted)
        );
    }



    @EventHandler
    public void onConnectEvent(ServerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()){
            int onlinePlayers = ProxyServer.getInstance().getOnlineCount();
            int maxPlayers = ProxyServer.getInstance().getConfig().getPlayerLimit();
            String server = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";

            String headerFormatted = rawHeader
                    .replace("%online_players%", String.valueOf(onlinePlayers))
                    .replace("%max_players%", String.valueOf(maxPlayers))
                    .replace("%server%", server);

            String footerFormatted = rawFooter
                    .replace("%online_players%", String.valueOf(onlinePlayers))
                    .replace("%max_players%", String.valueOf(maxPlayers))
                    .replace("%server%", server);

            headerFormatted = ChatColor.translateAlternateColorCodes('&', headerFormatted);
            footerFormatted = ChatColor.translateAlternateColorCodes('&', footerFormatted);

            all.setTabHeader(
                    new TextComponent(headerFormatted),
                    new TextComponent(footerFormatted)
            );
        }
    }

    @EventHandler
    public void onConnectEvent(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()){
            int onlinePlayers = ProxyServer.getInstance().getOnlineCount();
            int maxPlayers = ProxyServer.getInstance().getConfig().getPlayerLimit();
            String server = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";

            String headerFormatted = rawHeader
                    .replace("%online_players%", String.valueOf(onlinePlayers))
                    .replace("%max_players%", String.valueOf(maxPlayers))
                    .replace("%server%", server);

            String footerFormatted = rawFooter
                    .replace("%online_players%", String.valueOf(onlinePlayers))
                    .replace("%max_players%", String.valueOf(maxPlayers))
                    .replace("%server%", server);

            headerFormatted = ChatColor.translateAlternateColorCodes('&', headerFormatted);
            footerFormatted = ChatColor.translateAlternateColorCodes('&', footerFormatted);

            all.setTabHeader(
                    new TextComponent(headerFormatted),
                    new TextComponent(footerFormatted)
            );
          }
        }




    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        int onlinePlayers = ProxyServer.getInstance().getOnlineCount();
        int maxPlayers = ProxyServer.getInstance().getConfig().getPlayerLimit();
        String server = player.getServer() != null ? player.getServer().getInfo().getName() : "Unknown";

        String headerFormatted = rawHeader
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%server%", server);

        String footerFormatted = rawFooter
                .replace("%online_players%", String.valueOf(onlinePlayers))
                .replace("%max_players%", String.valueOf(maxPlayers))
                .replace("%server%", server);

        headerFormatted = ChatColor.translateAlternateColorCodes('&', headerFormatted);
        footerFormatted = ChatColor.translateAlternateColorCodes('&', footerFormatted);

        player.setTabHeader(
                new TextComponent(headerFormatted),
                new TextComponent(footerFormatted)
        );
    }
}
