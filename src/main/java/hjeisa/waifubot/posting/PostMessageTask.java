package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ApiObject;
import hjeisa.waifubot.model.ImageResponse;
import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.api.ApiConnector;
import javafx.util.Pair;
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
        connect to all apis
        from postmessagetask, get random image from one booru and retrieve image url(perhaps source)
        how:
            send request to each api(limit 0), collect post counts for tags
            get random int in bound of sum of all images, decide what booru to use
            get picture from api with page id matching int, retrieve image
     */

    public PostMessageTask(Request request) {
        this.request = request;
    }

    @Override
    public void run() {
        MessageChannel chan = request.getChannel();

        try {
            Map<ApiObject, Integer> postCounts = ApiConnector.getPostCounts(request.getSearchTags(), request.getSearchTagSize());

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

            ImageResponse response = getApiResponse(postCounts, random);
            postResponse(chan, response);
        }
        catch (Exception e){
            chan.sendMessage("An unexpected error occurred while processing your request.").queue();
            System.out.println("Exception caught.");
            System.out.println("Tags: " + request.getSearchTags());
            e.printStackTrace();
        }
    }

    private void postResponse(MessageChannel chan, ImageResponse response){
        // TODO: figure out how to use embed for source/post links
        // links cased in <> removes thumbnails/embeds
        StringBuilder sourceMessage = new StringBuilder("");
        sourceMessage.append("Post: <")
                .append(response.getPostURL())
                .append(">\nSource: ")
                .append(response.getSourceURL().isEmpty() ? "none" : "<" + response.getSourceURL() + ">");

        chan.sendFile(response.getImageData(), response.getFileName(), null).queue(message1 ->
                // post sources directly after file is done uploading
                chan.sendMessage(sourceMessage.toString()).queue()
        );

        if (Config.debug) {
            System.out.println("File uploaded: " + response.getFileName());
            System.out.println("Requested (tags: " + request.getSearchTags() + "): #" + chan.getName() + ": " +
                    "<" + Config.bot_name + "> " + sourceMessage);
        }
    }

    private ImageResponse getApiResponse(Map<ApiObject, Integer> postCounts, int random){

        Pair<ApiObject, Integer> pair = decideApi(postCounts, random);
        ApiObject api = pair.getKey();
        int page = pair.getValue();
        URL postUrl = ApiConnector.constructApiUrl(api, 1, page, request.getSearchTags());

        String content = ApiConnector.getPageContent(postUrl);
        ImageResponse response = ApiConnector.parseResponse(api, content);

        // logs stuff
        if(Config.debug){
            System.out.println("Postcounts: ");
            for(Map.Entry<ApiObject, Integer> ent : postCounts.entrySet()){
                System.out.println(" - " + ent.getKey().getName() + " / " + ent.getValue());
            }
            System.out.println("Number generated: " + random);
            System.out.println("Selected imageboard: " + api.getName());
            System.out.println("Resulting page: " + page);
            System.out.println("Generated URL: " + postUrl);
        }

        return response;
    }

    /*
     * decides which API to use from given postcounts and index.
     * returns a pair of the chosen API and the resulting offset of the given index
     */
    private Pair<ApiObject, Integer> decideApi(Map<ApiObject, Integer> postCounts, int random){
        // create iterable list from postcounts map
        ArrayList<Map.Entry<ApiObject, Integer>> entryList = new ArrayList<>();
        entryList.addAll(postCounts.entrySet());
        entryList.sort((o1, o2) -> o1.getValue() < o2.getValue() ? 1 : Objects.equals(o1.getValue(), o2.getValue()) ? 0 : -1);

        ApiObject chosenApi = null;
        int page = random;
        int pagesSkipped = 0;

        for (int i = 0; i <= postCounts.size() - 1; i++) {
            if (random >= entryList.get(i).getValue() + pagesSkipped) {
                page -= entryList.get(i).getValue();
                chosenApi = entryList.get(i + 1).getKey();
            }
            else {
                chosenApi = entryList.get(i).getKey();
                break;
            }
            pagesSkipped += entryList.get(i).getValue();
        }

        return new Pair<ApiObject, Integer>(chosenApi, page);
    }
}
