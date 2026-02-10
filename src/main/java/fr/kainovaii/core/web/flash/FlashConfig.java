package fr.kainovaii.core.web.flash;

public class FlashConfig
{
    private static String customCSS = "";
    private static int duration = 3000;
    private static String position = "bottom-right";

    public static void setCustomCSS(String css) {
        customCSS = css;
    }

    public static String getCustomCSS() {
        return customCSS;
    }

    public static void setDuration(int ms) {
        duration = ms;
    }

    public static int getDuration() {
        return duration;
    }

    public static void setPosition(String pos) {
        position = pos;
    }

    public static String getPosition() {
        return position;
    }

    public static String getPositionCSS()
    {
        return switch (position) {
            case "top-right" -> "top: 2rem; right: 2rem;";
            case "top-left" -> "top: 2rem; left: 2rem;";
            case "bottom-left" -> "bottom: 2rem; left: 2rem;";
            case "top-center" -> "top: 2rem; left: 50%; transform: translateX(-50%);";
            case "bottom-center" -> "bottom: 2rem; left: 50%; transform: translateX(-50%);";
            default -> "bottom: 2rem; right: 2rem;";
        };
    }
}