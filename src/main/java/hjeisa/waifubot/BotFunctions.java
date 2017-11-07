package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.posting.PostController;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotFunctions {

    // schedules post cycles
    private static PostController postController = new PostController();
    // holds all requests not cancelled
    private static List<Request> requestList = new ArrayList<>();
    // holds each user's best girl
    private static Map<User, String> bestGirlMap = new HashMap<>();

    public static void ping(String content, MessageChannel chan) {
        // ping function
        if(content.equalsIgnoreCase("!ping")) {
            chan.sendMessage("Pong!").queue();
        }
    }

    public static void post(String content, MessageChannel chan) {
        // create posting cycle
        boolean nsfw = content.toLowerCase().startsWith("!postnsfw ");
        boolean exnsfw = content.toLowerCase().startsWith("!postexnsfw ");

        if(content.toLowerCase().startsWith("!post ") || nsfw || exnsfw) {
            if(content.split(" ").length >= 3){
                int durationIndex = content.indexOf(' ');
                int searchTagIndex = content.indexOf(' ',durationIndex + 1);
                String intervalString = content.substring(durationIndex + 1, searchTagIndex);
                String searchTags = content.substring(searchTagIndex + 1);
                if(!nsfw && !exnsfw) searchTags += " rating:safe";
                if(exnsfw) searchTags += " rating:explicit";

                try {
                    // Duration.parse requires "pt" prefix
                    long intervalSeconds = Duration.parse("pt" + intervalString).getSeconds();
                    if(intervalSeconds < 0)
                        throw new Exception("Time can't be negative.");

                    if(!Config.debug){
                        if (intervalSeconds < Config.min_posting_interval) {
                            throw new DateTimeParseException("Time is too short. Minimum interval is " +
                                    Util.parseDuration(Config.min_posting_interval) + "", intervalString, 0);
                        }
                    }
                    else if(intervalSeconds == 0){
                        throw new Exception("Time can't be zero.");
                    }

                    if(Util.findRequestBySearchText(requestList, chan, searchTags) == null){
                        Request request = new Request(chan, intervalSeconds, searchTags);
                        requestList.add(request);
                        postController.schedulePostCycle(request);

                        chan.sendMessage("Request added. Posting pictures matching \"" + searchTags + "\" tags every " +
                                Util.parseDuration(intervalSeconds) + ".").queue();
                    }
                    else {
                        chan.sendMessage("I'm already posting pictures with the same tags in this channel.").queue();
                    }
                }
                catch (DateTimeParseException dtpe){
                    chan.sendMessage("Could not find duration. Proper usage is `!post <duration> <tags>`").queue();
                }
                catch (Exception e) {
                    chan.sendMessage(e.getMessage() + ".").queue();
                }
            }
            else {
                chan.sendMessage("Invalid number of arguments. Correct form is:\n"+
                        "`!post <interval> <search string>`").queue();
            }
        }
    }

    public static void picture(String content, MessageChannel chan) {
        // post one picture with tags
        if(content.toLowerCase().startsWith("!picture ")) {
            if (content.split(" ").length >= 2) {
                String searchTags = content.substring("!picture ".length());
                Request request = new Request(chan, 0, searchTags);
                postController.schedulePostOnce(request);
            }
        }
        else if(content.equals("!picture")){
            Request request = new Request(chan, 0, "");
            postController.schedulePostOnce(request);
        }
    }

    public static void bestgirl(User user, String content, MessageChannel chan) {
        // TODO: store user's chosen best girl to file, reload on reboot
        // posts user's best girl if one is found
        if(content.toLowerCase().startsWith("!bestgirl")){
            String girlToPost;
            // sets user's best girl
            if(content.toLowerCase().startsWith("!bestgirl set ") && content.split(" ").length >= 3){
                girlToPost = content.substring("!bestgirl set ".length());
                bestGirlMap.put(user, girlToPost);
                chan.sendMessage("Ok, recognized your best girl.").queue();
            }
            else {
                girlToPost = bestGirlMap.get(user);
                if(girlToPost == null){
                    chan.sendMessage("Set your best girl with the `!bestgirl set <character>` command first.").queue();
                    return;
                }
                chan.sendMessage(girlToPost + "!").queue();
            }
            Request request = new Request(chan, 0, girlToPost + " 1girl");
            postController.schedulePostOnce(request);
        }
    }

    public static void cancel(String content, MessageChannel chan){
        // cancel post cycle
        if(content.toLowerCase().startsWith("!cancel ")){
            // if command has tag parameters
            if(content.split(" ").length > 1){
                String searchTags = content.substring(8);
                Request request = Util.findRequestBySearchText(requestList, chan, searchTags);

                if(request != null){
                    if(postController.cancelPostCycle(requestList, request)){
                        chan.sendMessage("Cancelled request for tags \"" + request.getSearchTags() + "\".").queue();
                    }
                    else {
                        chan.sendMessage("Could not cancel request.").queue();
                    }
                }
                else {
                    chan.sendMessage("No request matching criteria found.").queue();
                }
            }
        }
        else if(content.toLowerCase().equals("!cancel")){
            int cancelled = postController.cancelChannelPostCycles(requestList, chan);

            if (cancelled == 0){
                chan.sendMessage("No requests to cancel for this channel.").queue();
            }
            else {
                chan.sendMessage("Cancelled all requests for this channel.").queue();
            }
        }
    }

    public static void list(String content, MessageChannel chan){
        // list all current posting cycles
        if(content.toLowerCase().startsWith("!list")){
            List<Request> requestsForChannel = Util.findAllRequestsByChannel(requestList, chan);

            if(requestsForChannel.size() > 0){
                StringBuilder str = new StringBuilder("Image posting cycles for this channel: ");
                for(Request req : requestsForChannel){
                    str.append("\n- Tags: \"")
                            .append(req.getSearchTags())
                            .append("\" every ")
                            .append(Util.parseDuration(req.getTimeInterval()));
                }
                chan.sendMessage(str.toString()).queue();
            }
            else {
                chan.sendMessage("I'm not posting any images in this channel.").queue();
            }
        }
    }

    public static void exclude(String content, MessageChannel chan) {
        // create a personalized blacklist for each user, adding exclude tags to each request
        if(content.toLowerCase().startsWith("!exclude")){
            // TODO: implement this
        }
    }
}
