package de.sync.cloud.infomation;

import de.sync.cloud.BungeeCloudBridge;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class BungeeCloudMessage {
    private static boolean running = true;


    public static void cloudMessage(){
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(9100)) {
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message;
                    while ((message = reader.readLine()) != null) {
                        String finalMessage = message;
                        ProxyServer.getInstance().getScheduler().runAsync(BungeeCloudBridge.getInstance(), () -> {
                            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                                    player.sendMessage("§b[Cloud] §f" + finalMessage);

                            }
                        });
                    }
                }
            } catch (Exception e) {
                for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                        player.sendMessage("§8[§bCloud§8] §cFehler beim Empfangen von Cloud-Nachrichten: " + e.getMessage());

                }
            }
        }).start();

    }}
