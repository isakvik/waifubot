package hjeisa.waifubot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

public class Bot {

    public static void main(String[] args) {
        try {
            JDA api = new JDABuilder(AccountType.BOT).setToken(Config.bot_token).buildAsync();
            api.addEventListener(new BotMessageListener());
        } catch (LoginException | RateLimitedException e) {
            e.printStackTrace();
        }
    }
}
