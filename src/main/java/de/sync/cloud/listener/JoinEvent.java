package de.sync.cloud.listener;

import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public class JoinEvent implements Listener {

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        String targetServer = event.getTarget().getName();

        if (targetServer.equalsIgnoreCase("lobby")) {
            // Verbindung erstmal abbrechen
            event.setCancelled(true);

            // Starte nach 5 Sekunden den Connect manuell
            ProxyServer.getInstance().getScheduler().schedule(BungeeCloudBridge.getInstance(), () -> {
                event.getPlayer().connect(ProxyServer.getInstance().getServerInfo("Lobby-1"));
            }, 5, TimeUnit.SECONDS);
        }
    }
}
