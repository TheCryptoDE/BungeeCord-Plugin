package de.sync.cloud.listener;

import de.sync.cloud.permissionsystem.ColorGradient;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.awt.*;
import java.util.Collections;

public class ServerListener implements Listener {

    @EventHandler
    public void onLogin(LoginEvent event) {
        if (MaintenanceManager.isMaintenance()) {
            PendingConnection connection = event.getConnection();
            // Check auf Bungee-Permissions ist hier tricky, da Spieler noch nicht voll verbunden ist
            // → wir nutzen den Namen oder die UUID
            if (!connection.getName().equalsIgnoreCase("admin") && !connection.getUniqueId().toString().equalsIgnoreCase("39e89d13-c2a7-4bf8-90f8-deaa2306cbac")) {
                // Optional: Wenn du UUID-Permission-Check willst, mach es extern
                event.setCancelReason("§cThe server is in maintenance mode!\n§7Please try again later.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();


        if (MaintenanceManager.isMaintenance()) {
            String baseMotd = "§8➤  %gradient% §7§lNetwork §8» §7Ready for the Future               §8➤ §f1.21.1§7–§f1.21.§c✘ §8| §7Official Test Server";
            String themania = "TheMania";
            String[] hexColors = {
                    "#C723FF", "#CF36E0", "#D74AC2", "#DF5DA3",
                    "#E77185", "#EF8466", "#F79848", "#FFAB29"
            };
            String gradient = applyGradient(themania, hexColors, true); // mit fett
            String motd = baseMotd.replace("%gradient%", gradient);
            event.getResponse().setDescription(motd);


            event.setResponse(ping);
        }
    }

    private String rgbToMinecraftColor(int r, int g, int b) {
        return String.format("§x§%x§%x§%x§%x§%x§%x",
                (r >> 4) & 0xF, r & 0xF,
                (g >> 4) & 0xF, g & 0xF,
                (b >> 4) & 0xF, b & 0xF);
    }
    private int[] hexToRgb(String hex) {
        return new int[] {
                Integer.parseInt(hex.substring(1, 3), 16), // Rot
                Integer.parseInt(hex.substring(3, 5), 16), // Grün
                Integer.parseInt(hex.substring(5, 7), 16)  // Blau
        };
    }

    private String applyGradient(String text, String[] hexColors, boolean bold) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String hex = hexColors[i % hexColors.length];
            int[] rgb = hexToRgb(hex);
            builder.append(rgbToMinecraftColor(rgb[0], rgb[1], rgb[2]));
            if (bold) builder.append("§l");
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }

}
