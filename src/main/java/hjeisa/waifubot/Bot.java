package hjeisa.waifubot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.File;

public class Bot {

    public static void main(String[] args) {
        File dataDirectory = new File(Config.data_file_path);
        if(!dataDirectory.exists())
            dataDirectory.mkdir();  // create global data directory, used to store data across installations

        try {
            JDA api = new JDABuilder(AccountType.BOT).setToken(Config.bot_token).buildAsync();
            api.addEventListener(new BotMessageListener());

            BotFunctions.loadBestGirls();
        } catch (LoginException | RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
