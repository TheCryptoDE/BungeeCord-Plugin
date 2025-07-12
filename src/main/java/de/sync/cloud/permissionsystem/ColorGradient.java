package de.sync.cloud.permissionsystem;

import java.awt.*;

public class ColorGradient {

    /**
     * Gibt einen farbverlaufenen String mit §-Farbcodes zurück.
     * @param text Der Text (z. B. "TheMania")
     * @param startColor Die Startfarbe im RGB (z. B. new Color(255, 0, 255)) — Pink
     * @param endColor Die Endfarbe im RGB (z. B. new Color(0, 0, 255)) — Blau
     * @return Der farbige String mit Minecraft-kompatiblen §x§R§G§B...
     */
    public static String applyGradient(String text, Color startColor, Color endColor) {
        StringBuilder sb = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            sb.append(toMinecraftColorCode(new Color(red, green, blue))).append(text.charAt(i));
        }

        return sb.toString();
    }

    /**
     * Wandelt eine Farbe in §x§R§G§B Minecraft Format um.
     * @param color Die Farbe
     * @return Der §-Farbcode
     */
    private static String toMinecraftColorCode(Color color) {
        return String.format("§x§%x§%x§%x§%x§%x§%x",
                (color.getRed() >> 4) & 0xF, color.getRed() & 0xF,
                (color.getGreen() >> 4) & 0xF, color.getGreen() & 0xF,
                (color.getBlue() >> 4) & 0xF, color.getBlue() & 0xF);
    }
}
