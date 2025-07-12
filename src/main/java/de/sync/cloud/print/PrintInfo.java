package de.sync.cloud.print;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class PrintInfo {

    public static final String PREFIX = "§8[§bCloud§8] §r"; // schöner Prefix für alle Nachrichten



    public static void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent("§8§m--------------------------------------"));
        sender.sendMessage(new TextComponent(PREFIX + "§bCloud Commands §8(§7Admin§8)"));
        sender.sendMessage(new TextComponent("§a/cloud startserver <name> [template] [proxy:true|false] §7- Start a server"));
        sender.sendMessage(new TextComponent("§a/cloud stopserver <name> §7- Stop a server"));
        sender.sendMessage(new TextComponent("§a/cloud stopall §7- Stop all servers"));
        sender.sendMessage(new TextComponent("§a/cloud listservers §7- Show list of running servers"));
        sender.sendMessage(new TextComponent("§a/cloud statusserver <name> §7- Show status of a server"));
        sender.sendMessage(new TextComponent("§b/cloud set maintenance <true|false>"));
        sender.sendMessage(new TextComponent("§a/cloud help §7- Show this help"));
        sender.sendMessage(new TextComponent("§8§m--------------------------------------"));

    }

}
