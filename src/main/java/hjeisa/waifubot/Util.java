package hjeisa.waifubot;

public class Util {
    public static String parseDuration(long duration) {
        // TODO: this is pretty lazy. return a better format
        long hours = duration / 3600;
        long minutes = duration % 3600 / 60;
        long seconds = duration % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
