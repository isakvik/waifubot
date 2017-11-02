package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;

public class PostMessageTask implements Runnable {

    private Request request;

    public PostMessageTask(Request request) {
        this.request = request;
    }

    @Override
    public void run() {
        // TODO: make image finding system, replace placeholder message
        // TODO: store Request.lastPostCount, optimize network calls
        String postedText = "Scheduled message placeholder~ (debug)";
        MessageChannel chan = request.getChannel();

        if(Config.debug){
            System.out.println("Requested (tags: " + request.getSearchTags() + "): #" + chan.getName() + ": <" + Config.bot_name + "> " + postedText);
        }
        chan.sendMessage(postedText).queue();
    }
}
