package hjeisa.waifubot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.File;

public class Bot {

    public static void main(String[] args) {
        File dataDirectory = new File(Config.data_file_path);
        if(!dataDirectory.exists())
            dataDirectory.mkdir();  // create global data directory, used to store data across installations

        try {
            JDA bot = JDABuilder.createDefault(Config.bot_token)
                    .addEventListeners(new BotMessageListener())
                    .build();

            BotFunctions.initialize();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }
}
