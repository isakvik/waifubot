package hjeisa.waifubot.model;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Request {

    private Guild server;
    private TextChannel channel;
    private long creationTime;

    private long timeInterval;
    private String searchTags;

    public Request(Guild server, TextChannel channel, long timeInterval, String searchTags) {
        this.server = server;
        this.channel = channel;
        this.timeInterval = timeInterval;
        this.searchTags = searchTags;

        creationTime = Instant.now().getEpochSecond();
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

    public String getSearchTags() {
        return searchTags;
    }

    public void setSearchTags(String searchText) {
        this.searchTags = searchText;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
