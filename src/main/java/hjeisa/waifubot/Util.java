package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.*;

public class Util {

    public static String parseDuration(long duration) {
        // TODO: this is pretty lazy. return a better format
        long hours = duration / 3600;
        long minutes = duration % 3600 / 60;
        long seconds = duration % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // remove parantheses, capitalize first letter
    public static String cleanNameTag(String tag) {
        tag = tag.replaceAll("_\\(.*\\)","");
        return tag.substring(0,1).toUpperCase() + tag.substring(1);
    }

    // returns one request where the search tags and channel match, or null
    public static Request findRequestBySearchText(List<Request> list, MessageChannel chan, String searchTags){
        for(Request req : list){
            if(req.getChannel().equals(chan)){
                HashSet<String> tagSet = new HashSet<>(
                        Arrays.asList(searchTags.split(" ")));
                HashSet<String> requestTagSet = new HashSet<>(
                        Arrays.asList(req.getSearchTagsWithoutExcludes().split(" ")));
                if(tagSet.equals(requestTagSet))
                    return req;
            }
        }

        return null;
    }

    // returns list of all requests where the channel matches, or empty list
    public static List<Request> findAllRequestsByChannel(List<Request> list, MessageChannel chan){
        List<Request> requests = new ArrayList<>();
        for(Request req : list){
            if(req.getChannel().equals(chan)){
                requests.add(req);
            }
        }
        return requests;
    }
}
