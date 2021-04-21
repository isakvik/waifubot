package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.util.*;

public class Util {

    public static String parseDuration(long duration) {
        StringBuilder retStr = new StringBuilder();

        long hours = duration / 3600;
        long minutes = duration % 3600 / 60;
        long seconds = duration % 60;

        if(hours > 0) {
            if(hours > 1)
                retStr.append(hours).append(" hours");
            else
                retStr.append("hour");

            if(minutes > 0 && seconds > 0)
                retStr.append(", ");
            else if(minutes > 0)
                retStr.append(" and ");
        }

        if(minutes > 0) {
            if(minutes > 1)
                retStr.append(minutes).append(" minutes");
            else
                retStr.append(minutes);

            if(seconds > 0)
                retStr.append(" and ");
        }

        if(seconds > 0)
            retStr.append((seconds > 1 ? "seconds" : "second"));

        return retStr.toString();
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
