package hjeisa.waifubot.model;

import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.HashSet;

public class Request {

    private MessageChannel channel;

    private long timeInterval;
    private String searchTags;

    public HashSet<Integer> alreadyPosted;

    public Request(MessageChannel channel, long timeInterval, String searchTags) {
        this.channel = channel;
        this.timeInterval = timeInterval;
        this.searchTags = searchTags;

        alreadyPosted = new HashSet<>();
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public String getSearchTags() {
        return searchTags;
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
