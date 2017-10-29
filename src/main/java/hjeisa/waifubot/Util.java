package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static String parseDuration(long duration) {
        // TODO: this is pretty lazy. return a better format
        long hours = duration / 3600;
        long minutes = duration % 3600 / 60;
        long seconds = duration % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // returns one request where the search tags and channel match, or null
    public static Request findRequestBySearchText(List<Request> list, MessageChannel chan, String searchWords){
        for(Request req : list){
            if(req.getSearchText().equals(searchWords) && req.getChannel().equals(chan)){
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
