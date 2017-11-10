package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.posting.PostController;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.*;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;

public class BotFunctions {

    // schedules post cycles
    private static PostController postController = new PostController();
    // holds all requests not cancelled
    private static List<Request> requestList = new ArrayList<>();
    // holds each user's best girl
    private static Map<Long, String> bestGirlMap = new HashMap<>();
    // holds each user's exclude list
    private static Map<Long, String> excludeMap = new HashMap<>();

    static void initialize(){
        BotFunctions.loadMap(bestGirlMap, Config.best_girl_data_filename);
        BotFunctions.loadMap(excludeMap, Config.exclude_data_filename);
    }

    static void ping(String content, MessageChannel chan) {
        // ping function
        if(content.equalsIgnoreCase("!ping")) {
            chan.sendMessage("Pong!").queue();
        }
    }

    static void post(User user, String content, MessageChannel chan) {
        // create posting cycle
        boolean nsfw = content.toLowerCase().startsWith("!postnsfw");
        boolean exnsfw = content.toLowerCase().startsWith("!postexnsfw");  // exclusively*

        if(nsfw || exnsfw){
            if(chan instanceof TextChannel && !((TextChannel) chan).isNSFW()){
                chan.sendMessage("This is not set as an NSFW channel.").queue();
                return;
            }
        }

        if(content.toLowerCase().startsWith("!post") || nsfw || exnsfw) {
            if(content.split(" ").length >= 3){
                int durationIndex = content.indexOf(' ');
                int searchTagIndex = content.indexOf(' ',durationIndex + 1);
                String intervalString = content.substring(durationIndex + 1, searchTagIndex);
                String searchTags = content.substring(searchTagIndex + 1);
                if(nsfw) searchTags +=      " -rating:safe";
                if(exnsfw) searchTags +=    " rating:explicit";
                else searchTags +=          " rating:safe";

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

                    if(Util.findRequestBySearchText(requestList, chan, searchTags) == null){
                        Request request = new Request(chan, intervalSeconds,
                                searchTags + " " + excludeMap.getOrDefault(user.getIdLong(), ""));
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
                    chan.sendMessage("Could not find duration. Proper usage is `!post <interval> <search tags>`").queue();
                }
                catch (Exception e) {
                    chan.sendMessage(e.getMessage() + ".").queue();
                }
            }
            else {
                chan.sendMessage("Invalid arguments. Correct form is:\n"+
                        "`!post <interval> <search tags>`").queue();
            }
        }
    }

    static void picture(User user, String content, MessageChannel chan) {
        // create posting cycle
        boolean nsfw = content.toLowerCase().startsWith("!picturensfw");
        boolean exnsfw = content.toLowerCase().startsWith("!pictureexnsfw");

        if(nsfw || exnsfw){
            if(chan instanceof TextChannel && !((TextChannel) chan).isNSFW()){
                chan.sendMessage("This is not set as an NSFW channel.").queue();
                return;
            }
        }

        // post one picture with tags
        if(content.toLowerCase().startsWith("!picture") || nsfw || exnsfw) {
            int searchTagIndex = content.indexOf(' ');
            String searchTags = "";
            if(searchTagIndex != -1){
                searchTags = content.substring(searchTagIndex + 1);
                if(nsfw) searchTags +=      " -rating:safe";
                if(exnsfw) searchTags +=    " rating:explicit";
                else searchTags +=          " rating:safe";
            }
            
            Request request = new Request(chan, 0,
                    searchTags + " " + excludeMap.getOrDefault(user.getIdLong(), ""));
            postController.schedulePostOnce(request);
        }
    }

    static void bestgirl(User user, String content, MessageChannel chan) {
        // posts user's best girl if one is found
        if(content.toLowerCase().startsWith("!bestgirl")) {
            String girlToPost;
            // sets user's best girl
            if(content.toLowerCase().startsWith("!bestgirl set ") && content.split(" ").length >= 3){
                girlToPost = content.substring("!bestgirl set ".length());
                bestGirlMap.put(user.getIdLong(), girlToPost);
                if(!saveMap(bestGirlMap, Config.best_girl_data_filename)){
                    chan.sendMessage("Ahh, I couldn't save your best girl to my data file. " +
                            "I'll keep it in mind until I restart, though!").queue();
                }
                else {
                    chan.sendMessage("Ok, recognized your best girl.").queue();
                }
            }
            else {
                girlToPost = bestGirlMap.get(user.getIdLong());
                if(girlToPost == null){
                    chan.sendMessage("Set your best girl with the `!bestgirl set <character>` command first.").queue();
                    return;
                }
                chan.sendMessage(Util.cleanNameTag(girlToPost) + "!").queue();
            }
            Request request = new Request(chan, 0,
                    girlToPost + " 1girl " + excludeMap.getOrDefault(user.getIdLong(), ""));
            postController.schedulePostOnce(request);
        }
    }

    static void cancel(String content, MessageChannel chan){
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

    static void list(String content, MessageChannel chan){
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

    static void exclude(User user, String content, MessageChannel chan) {
        // TODO: figure out why extra space appears in list
        // create a personalized blacklist for each user, adding exclude tags to each request
        if(content.toLowerCase().startsWith("!exclude ")){
            ArrayList<String> tagList = new ArrayList<>(Arrays.asList(
                    content.substring("!exclude ".length()).split(" ")));
            ArrayList<String> excludeList = new ArrayList<>(Arrays.asList(
                    excludeMap.getOrDefault(user.getIdLong(), "").split(" ")));
            boolean changed = false;

            for(String tag : tagList){
                tag = tag.replaceAll("\\s"," ");
                if(!tag.startsWith("-")){
                    tag = "-" + tag;
                }
                if(!excludeList.contains(tag)){
                    excludeList.add(tag);
                    changed = true;
                }
            }

            if(changed){
                StringBuilder sb = new StringBuilder();
                for(String exclude : excludeList)
                    sb.append(exclude).append(" ");

            // will update list
                excludeMap.put(user.getIdLong(), sb.toString());
                if(!saveMap(excludeMap, Config.exclude_data_filename)){
                    chan.sendMessage("Couldn't save your exclude list, probably due to me being in test mode." +
                            " Sorry!").queue();
                }
                else {
                    chan.sendMessage("Ok. Exclude list is now: " + sb).queue();
                }
            }
            else {
                chan.sendMessage("Exclude list is unchanged: " + excludeMap.get(user.getIdLong())).queue();
            }
        }
    }

    static void excludes(User user, String content, MessageChannel chan) {
        // list current user's excludes, or clear excludes
        if(content.toLowerCase().equals("!excludes")){
            String excludes = excludeMap.get(user.getIdLong());
            if(excludes == null){
                chan.sendMessage("None given.").queue();
                return;
            }
            chan.sendMessage("Your exclude tags: " + excludes).queue();
        }
        else if(content.toLowerCase().equals("!excludes clear")){
            excludeMap.remove(user.getIdLong());
            saveMap(excludeMap, Config.exclude_data_filename);
            chan.sendMessage("Ok, removed all excludes.").queue();
        }
    }

    ///////////////////////////////////////////////////////// helpers

    private static boolean saveMap(Map<Long,String> map, String fileName){
        File mapFile = new File(Config.data_file_path + fileName);
        try {
            if(!mapFile.exists())
                mapFile.createNewFile();

            BufferedWriter out = new BufferedWriter(new FileWriter(mapFile));
            for(Map.Entry<Long,String> entry : map.entrySet()){
                out.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void loadMap(Map<Long,String> map, String filename){
        File mapFile = new File(Config.data_file_path + filename);
        if(!mapFile.exists())
            return;

        try {
            BufferedReader in = new BufferedReader(new FileReader(mapFile));
            String line;
            while((line = in.readLine()) != null){
                int indexOfDelimiter = line.indexOf(' ');
                long userID = Long.parseLong(line.substring(0, indexOfDelimiter));
                String data = line.substring(indexOfDelimiter + 1);
                map.put(userID, data);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
