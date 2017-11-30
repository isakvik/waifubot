package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ImageResponse;
import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.web.ConnectionHandler;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

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
                chan.sendMessage("I couldn't find any pictures matching these tags: " + request.getSearchTags()).queue();
                return;
            }

            if(request.alreadyPosted.size() >= sum){
                chan.sendMessage("Pool of images exhausted.").queue();
                request.alreadyPosted.clear();
            }

            Random rng = new Random();
            int random = rng.nextInt(sum);

            // prevent already posted images from reappearing for the same request
            while(request.alreadyPosted.contains(random)){
                random++;
                random %= sum;
            }
            request.alreadyPosted.add(random);

            ArrayList<Map.Entry<String, Integer>> entryList = new ArrayList<>();
            entryList.addAll(postCounts.entrySet());
            entryList.sort((o1, o2) -> o1.getValue() < o2.getValue() ? 1 : Objects.equals(o1.getValue(), o2.getValue()) ? 0 : -1);

            // decide which imageboard to get from
            String selectedImageboard = "";
            int pagesSkipped = 0;
            System.out.println("Random: " + random);
            int page = random;

            for (int i = 0; i <= postCounts.size() - 1; i++) {
                if (random >= entryList.get(i).getValue() + pagesSkipped) {
                    page -= entryList.get(i).getValue();
                    selectedImageboard = entryList.get(i + 1).getKey();
                }
                else {
                    selectedImageboard = entryList.get(i).getKey();
                    break;
                }
                pagesSkipped += entryList.get(i).getValue();
            }

            URL url;
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

            // logs stuff
            if(Config.debug){
                System.out.println("Postcounts: ");
                for(Map.Entry<String, Integer> ent : entryList){
                    System.out.println(" - " + ent.getKey() + " / " + ent.getValue());
                }
                System.out.println("Number generated: " + random);
                System.out.println("Pages skipped: " + pagesSkipped);
                System.out.println("Selected imageboard: " + selectedImageboard);
                System.out.println("Generated URL: " + url);
            }

            ImageResponse response = null;
            try {
                response = handler.parseResponse(selectedImageboard, content);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                chan.sendMessage("An unexpected error occurred while processing your request.").queue();
                System.out.println("tags: " + request.getSearchTags());
                System.out.println("selected imageboard: " + selectedImageboard);

                System.out.println(page);
                return;
            }

            // TODO: figure out how to use embed for source/post links
            // links cased in <> removes thumbnails/embeds
            StringBuilder sourceMessage = new StringBuilder("");
            sourceMessage.append("Post: <")
                    .append(response.getPostURL())
                    .append(">\nSource: ")
                    .append(response.getSourceURL().isEmpty() ? "none" : "<" + response.getSourceURL() + ">");

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
            System.out.println("Tags: " + request.getSearchTags());
            e.printStackTrace();
        }
    }
}
