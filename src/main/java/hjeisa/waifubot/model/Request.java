package hjeisa.waifubot.model;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.HashSet;

public class Request {

    private MessageChannel channel;
    private User user;

    private long timeInterval;
    private String searchTags;
    private int searchTagSize;

    public HashSet<Integer> alreadyPosted;

    public Request(MessageChannel channel, User user, long timeInterval, String searchTags) {
        this.channel = channel;
        this.user = user;
        this.timeInterval = timeInterval;
        this.searchTags = searchTags;
        this.searchTagSize = searchTags.split(" ").length;

        alreadyPosted = new HashSet<>();
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public String getSearchTags() {
        return searchTags;
    }
    public int getSearchTagSize() {
        return searchTagSize;
    }

    public String getSearchTagsWithoutExcludes() {
        String[] tags = searchTags.split(" ");
        StringBuilder sb = new StringBuilder();
        for(String tag : tags){
            if(!tag.startsWith("-")){
                sb.append(tag).append(" ");
            }
        }
        return sb.toString();
    }
}
