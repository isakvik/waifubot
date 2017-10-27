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
        chan.sendMessage("Scheduled message placeholder~ (debug)").queue();
    }
}
