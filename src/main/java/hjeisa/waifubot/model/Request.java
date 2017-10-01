package hjeisa.waifubot.model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.Instant;

public class Request {

    private Guild server;
    private TextChannel channel;
    private long creationTime;

    private long timeInterval;
    private String searchText;

    public Request(Guild server, TextChannel channel, long timeInterval, String searchText) {
        this.server = server;
        this.channel = channel;
        this.timeInterval = timeInterval;
        this.searchText = searchText;

        creationTime = Instant.now().toEpochMilli();
    }

    public Guild getServer() {
        return server;
    }

    public void setServer(Guild server) {
        this.server = server;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public void setChannel(TextChannel channel) {
        this.channel = channel;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(long timeInterval) {
        this.timeInterval = timeInterval;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
