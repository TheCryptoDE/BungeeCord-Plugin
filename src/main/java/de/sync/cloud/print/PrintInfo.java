package de.sync.cloud.print;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class PrintInfo {

    public static final String PREFIX = "§8[§bCloud§8] §r"; // schöner Prefix für alle Nachrichten



    public static void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent("§8§m--------------------------------------"));
        sender.sendMessage(new TextComponent(PREFIX + "§bCloud Befehle §8(§7Admin§8)"));
        sender.sendMessage(new TextComponent("§a/cloud startserver <name> [template] [proxy:true|false] §7- Server starten"));
        sender.sendMessage(new TextComponent("§a/cloud stopserver <name> §7- Server stoppen"));
        sender.sendMessage(new TextComponent("§a/cloud stopall §7- Alle Server stoppen"));
        sender.sendMessage(new TextComponent("§a/cloud listservers §7- Liste laufender Server anzeigen"));
        sender.sendMessage(new TextComponent("§a/cloud statusserver <name> §7- Status eines Servers anzeigen"));
        sender.sendMessage(new TextComponent("§a/cloud help §7- Diese Hilfe anzeigen"));
        sender.sendMessage(new TextComponent("§8§m--------------------------------------"));
    }

}
