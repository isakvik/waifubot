package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.exception.FileDeletedException;
import hjeisa.waifubot.exception.ForbiddenTagException;
import hjeisa.waifubot.model.ApiObject;
import hjeisa.waifubot.model.ImageResponse;
import hjeisa.waifubot.model.Request;
import hjeisa.waifubot.api.ApiConnector;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PostMessageTask implements Runnable {

    private Request request;
    private PostController postController;

    private int requestPage;
    private int retryCounter;

    /*
        procedure:
        connect to all apis
        from postmessagetask, get random image from one booru and retrieve image url(perhaps source)
        how:
            send request to each api(limit 0), collect post counts for tags
            get random int in bound of sum of all images, decide what booru to use
            get picture from api with page id matching int, retrieve image
     */

    public PostMessageTask(PostController postController, Request request) {
        this.postController = postController;
        this.request = request;
    }

    @Override
    public void run() {
        MessageChannel chan = request.getChannel();
        if(retryCounter >= Config.max_post_retry_attempts){
            chan.sendMessage("Exhausted amount of retries while trying to find a suitable image. I'm sorry!").queue();
            return;
        }

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
            // this does not work for danbooru requests because we let the API handle the random part
            // but it shouldn't matter unless the tag is very rare
            while(request.alreadyPosted.contains(random)){
                random++;
                random %= sum;
            }
            request.alreadyPosted.add(random);

            ImageResponse response = getApiResponse(postCounts, random);
            retryCounter = 0;
            postResponse(chan, response);
        }
        catch (FileDeletedException fde){
            chan.sendMessage("Looked up deleted file, retrying for new post...").queue();
            retryCounter++;
            run();
        }
        catch (ForbiddenTagException fte){
            //chan.sendMessage("Result contained forbidden tag, retrying for new post...").queue();
            System.out.println("ForbiddenTagException thrown. Retrying for new post...");
            retryCounter++;
            run();
        }
        catch (Exception e){
            chan.sendMessage("An unexpected error occurred while processing your request.").queue();
            e.printStackTrace();
        }
    }

    private void postResponse(MessageChannel chan, ImageResponse response){
        // TODO: figure out how to use embed for source/post links
        // links cased in <> removes thumbnails/embeds
        StringBuilder sourceMessageContent = new StringBuilder("");
        sourceMessageContent.append("Post: <")
                .append(response.getPostURL())
                .append(">\nSource: ")
                .append(response.getSourceURL().isEmpty() ? "none given" : "<" + response.getSourceURL() + ">");

        try {
            CompletableFuture<Message> imageMessageAction = chan.sendFile(response.getImageData(), response.getFileName()).submit();
            Message imageUploadMessage = imageMessageAction.get(); // blocks

            CompletableFuture<Message> sourceMessageAction = chan.sendMessage(sourceMessageContent.toString()).submit();
            Message sourceMessage = sourceMessageAction.get();

            // store
            postController.getLastRequestResponseByUser().put(
                    new ImmutablePair<>(request.getChannel().getIdLong(), request.getUser().getIdLong()),
                    new ImmutablePair<>(imageUploadMessage.getIdLong(), sourceMessage.getIdLong())
            );
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (Config.debug) {
            System.out.println("File uploaded: " + response.getFileName());
            System.out.println("Requested (tags: " + request.getSearchTags() + "): #" + chan.getName() + ": " +
                    "<" + Config.bot_name + "> " + sourceMessageContent);
        }
    }

    private ImageResponse getApiResponse(Map<ApiObject, Integer> postCounts, int random)
            throws FileDeletedException, ForbiddenTagException {

        ApiObject api = decideApi(postCounts, random);
        int page = requestPage;
        final int offset = 0;
        int limit = 1;

        URL postUrl = ApiConnector.constructApiUrl(api, limit, page, request.getSearchTags());
        String content = ApiConnector.getPageContent(postUrl);

        // logs stuff
        if(Config.debug){
            System.out.println("Postcounts: ");
            for(Map.Entry<ApiObject, Integer> ent : postCounts.entrySet()){
                System.out.println(" - " + ent.getKey().getName() + " / " + ent.getValue());
            }
            System.out.println("Number generated: " + random);
            System.out.println("Resulting page: " + page);
            System.out.println("Generated URL: " + postUrl);
            System.out.println("Selected imageboard: " + api.getName());
        }

        return ApiConnector.parseResponse(api, content, offset);
    }

    /*
     * decides which API to use from given postcounts and index.
     * returns a pair of the chosen API and the resulting offset of the given index
     */
    private ApiObject decideApi(Map<ApiObject, Integer> postCounts, int random){
        // create iterable list from postcounts map
        ArrayList<Map.Entry<ApiObject, Integer>> entryList = new ArrayList<>(postCounts.entrySet());
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

        // TODO: fix this dumb shit
        requestPage = page;
        return chosenApi;
    }
}
