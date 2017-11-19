package hjeisa.waifubot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class BotMessageListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        executeFunctions(event.getMessage());
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        executeFunctions(event.getMessage());
    }

    private void executeFunctions(Message message){
        String content = message.getRawContent();
        MessageChannel chan = message.getChannel();
        User user = message.getAuthor();

        // no bots please
        if(user.isBot()) return;

        // remove unneeded whitespace in commands
        content = content.replaceAll("\\s+", " ");
        // replace anti-markdown backslashes
        content = content.replaceAll("\\\\\\_", "_");
        content = content.replaceAll("\\\\\\*", "*");

        if(Config.debug){
            System.out.println("Received message: "
                    + chan.getName() + ": <"
                    + user.getName() + "> "
                    + content);
        }
        BotFunctions.ping(content, chan);
        BotFunctions.post(user, content, chan);
        BotFunctions.picture(user, content, chan);
        BotFunctions.bestgirl(user, content, chan);
        BotFunctions.cancel(content, chan);
        BotFunctions.list(content, chan);
        BotFunctions.exclude(user, content, chan);
        BotFunctions.excludes(user, content, chan);
    }
}
