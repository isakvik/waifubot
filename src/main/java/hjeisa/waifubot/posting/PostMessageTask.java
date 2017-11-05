package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ImageResponse;
import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.web.ConnectionHandler;
import hjeisa.waifubot.web.URLs;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class PostMessageTask implements Runnable {

    private Request request;

    /*
        procedure:
        connect to three apis
        from postmessagetask, get random image from one booru and retrieve image url(perhaps source)
        how:
            send request to each api(limit 0), collect post counts for tags
            get random int, decide what booru to use
            get picture from api with page id matching int, retrieve image(+ source url)
     */

    public PostMessageTask(Request request) {
        this.request = request;
    }

    @Override
    public void run() {
        try {
            MessageChannel chan = request.getChannel();

            // TODO: move relevant bits to own methods
            ConnectionHandler handler = new ConnectionHandler();
            Map<String, Integer> postCounts = handler.getPostCounts(request.getSearchTags());

            int sum = 0;
            for (Integer val : postCounts.values())
                sum += val;

            if (sum == 0) {
                chan.sendMessage("No pictures found matching these tags in any imageboards I'm set to look in.").queue();
                return;
            }

            Random rng = new Random();
            int random = rng.nextInt(sum);

            ArrayList<Map.Entry<String, Integer>> entryList = new ArrayList<>();
            entryList.addAll(postCounts.entrySet());
            entryList.sort((o1, o2) -> o1.getValue() > o2.getValue() ? 1 : -1);

            int pagesSkipped = 0;
            int page = random;
            String selectedImageboard = "";
            for (int i = 0; i < postCounts.size() - 1; i++) {
                if (random > entryList.get(i).getValue() + pagesSkipped) {
                    page -= entryList.get(i).getValue();
                    selectedImageboard = entryList.get(i + 1).getKey();
                }
                pagesSkipped += entryList.get(i).getValue();
            }

            URL url = null;
            try {
                url = handler.constructApiUrl(selectedImageboard, 1, page, request.getSearchTags());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                chan.sendMessage("An unexpected error occurred while processing your request.").queue();
                return;
            }
            String content = handler.getPageContent(url);
            if (content == null || content.isEmpty()) {
                chan.sendMessage("Could not get response from " + selectedImageboard + "'s API.").queue();
                return;
            }

            ImageResponse response = null;
            try {
                response = handler.parseResponse(selectedImageboard, content);
            } catch (IOException e) {
                e.printStackTrace();
                chan.sendMessage("An unexpected error occurred while processing your request.").queue();
                return;
            }

            // TODO: figure out how to use embed for source/post links
            StringBuilder sourceMessage = new StringBuilder("");
            sourceMessage.append("Post: ");
            sourceMessage.append(response.getPostURL());
            sourceMessage.append("\nSource: ");
            sourceMessage.append(response.getPostURL().isEmpty() ? "none" : "<" + response.getSourceURL() + ">");

            chan.sendFile(response.getImageData(), response.getFileName(), null).queue(message1 ->
                    chan.sendMessage(sourceMessage.toString()).queue()
            );

            if (Config.debug) {
                System.out.println("File uploaded: " + response.getFileName());
                System.out.println("Requested (tags: " + request.getSearchTags() + "): #" + chan.getName() + ": " +
                        "<" + Config.bot_name + "> " + sourceMessage);
            }
        }
        catch (Exception e){
            System.out.println("Exception caught.");
            e.printStackTrace();
        }
    }
}
