package hjeisa.waifubot;

public class Util {
    public static String parseDuration(long duration) {
        long hours = duration / 3600000;
        long minutes = hours / 60000;
        long seconds = minutes / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
