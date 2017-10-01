package hjeisa.waifubot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class PrivateMessageListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        // anti bot security
        if(event.getAuthor().isBot()) return;

        if(Config.debug){
            System.out.println("Received a message. "
                    + event.getChannel().getName() + ": <"
                    + event.getAuthor().getName() + "> "
                    + event.getMessage().getContent());
        }

        ping(event);
    }

    private void initFunction(PrivateMessageReceivedEvent event) {
    }

    private static void ping(PrivateMessageReceivedEvent event){
        Message message = event.getMessage();
        String content = message.getRawContent();

        // ping function
        if(content.equalsIgnoreCase("!ping")) {
            MessageChannel chan = message.getChannel();
            chan.sendMessage("Pong! (" + "some amount of " + "ms)").queue();
        }
    }
}
