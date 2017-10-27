package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.TimerTask;

public class PostMessageTask implements Runnable {

    private Request request;

    public PostMessageTask(Request request) {
        this.request = request;
    }

    @Override
    public void run() {
        MessageChannel chan = request.getChannel();
        String postedText = "Scheduled message placeholder~ (debug)";
        chan.sendMessage(postedText).queue();

        if(Config.debug){
            System.out.println("Requested (tags: " + request.getSearchText() + "): #" + chan.getName() + ": <" + Config.bot_name + "> " + postedText);
        }
    }
}
