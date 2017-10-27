package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ServerMessageListener extends ListenerAdapter {

    private PostController postController = new PostController();
    private List<Request> requestList = new ArrayList<>();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // anti bot security
        // TODO: create common class for listener implementations?
        if(event.getAuthor().isBot()) return;

        if(Config.debug){
            System.out.println("#" + event.getChannel().getName() + ": <"
                            + event.getAuthor().getName() + "> "
                            + event.getMessage().getContent());
        }

        try {
            ping(event);
            post(event);
        }
        catch(Exception e) {
            event.getChannel().sendMessage("An error occurred while processing your request.").queue();
        }
    }

    ////////////////////////////////////////////////////////// functions below

    private void ping(GuildMessageReceivedEvent event){
        Message message = event.getMessage();
        String content = message.getRawContent();
        MessageChannel chan = message.getChannel();

        // ping function
        if(content.equalsIgnoreCase("!ping")) {
            chan.sendMessage("Pong! (" + "some amount of " + "ms)").queue();
        }
    }

    private void post(GuildMessageReceivedEvent event){
        Message message = event.getMessage();
        String content = message.getRawContent();
        MessageChannel chan = message.getChannel();

        // create request
        if(content.toLowerCase().startsWith("!post ")) {
            if(content.split(" ").length > 2){
                int indexOfSearchWords = content.indexOf(' ',6);
                String intervalString = content.substring(6, indexOfSearchWords);
                String searchWords = content.substring(indexOfSearchWords + 1);

                try {
                    // Duration.parse requires "pt" prefix
                    long intervalSeconds = Duration.parse("pt" + intervalString).getSeconds();
                    if(intervalSeconds < 0)
                        throw new DateTimeParseException("Time can't be negative", intervalString, 0);

                    if(!Config.debug)
                        if(intervalSeconds < Config.min_posting_interval)
                            throw new DateTimeParseException("Time is too short. Minimum interval is " +
                                    Util.parseDuration(Config.min_posting_interval) + ".", intervalString, 0);
                    else if(intervalSeconds == 0)
                        throw new DateTimeParseException("Time can't be zero.", intervalString, 0);

                    Request request = new Request(event.getGuild(), event.getChannel(), intervalSeconds, searchWords);
                    requestList.add(request);
                    postController.startPostCycle(request);

                    chan.sendMessage("Request added. Posting pictures matching \"" + searchWords + "\" tags every " +
                            Util.parseDuration(intervalSeconds) + ".").queue();
                }
                catch (DateTimeParseException e) {
                    chan.sendMessage(e.getMessage() + ".").queue();
                    if(!Config.debug) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                chan.sendMessage("Invalid number of arguments. Correct form is:\n"+
                        "`!post <interval> <search string>`").queue();
            }
        }
    }
}
