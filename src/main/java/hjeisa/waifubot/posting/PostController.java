package hjeisa.waifubot.posting;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.Util;
import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.utils.tuple.Pair;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PostController {

    // holds all cycle instances
    private Map<Request, ScheduledFuture> futures = new HashMap<>();
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    // used for !delete
    private Map<Pair<Long,Long>, Pair<Long,Long>> lastRequestResponseByUser = new HashMap<>();

    public void schedulePostCycle(Request request) {
        if(requestContainsForbiddenTag(request)){
            request.getChannel().sendMessage("Please do not do that.").queue();
            return;
        }

        // TODO: see if you can store the schedules to file
        PostMessageTask task = new PostMessageTask(this, request);
        ScheduledFuture future = service.scheduleAtFixedRate(task, 0, request.getTimeInterval(), TimeUnit.SECONDS);
        futures.put(request, future);

        if(Config.debug){
            System.out.println("Scheduled: \"" + request.getSearchTags() + "\" for #" + request.getChannel().getName() +
                    " every " + Util.parseDuration(request.getTimeInterval()));
        }
    }

    public void schedulePostOnce(Request request) {
        if(requestContainsForbiddenTag(request)){
            request.getChannel().sendMessage("Please do not do that.").queue();
            return;
        }

        PostMessageTask task = new PostMessageTask(this, request);
        service.schedule(task, 0, TimeUnit.SECONDS);
        if(Config.debug){
            System.out.println("Posting once: \"" + request.getSearchTags() + "\" for #" + request.getChannel().getName());
        }
    }

    public boolean cancelPostCycle(List<Request> requestList, Request request) {
        ScheduledFuture future = futures.get(request);

        if(Config.debug){
            System.out.println("Cancelled: \"" + request.getSearchTags() + "\" for #" + request.getChannel().getName() +
                    " every " + Util.parseDuration(request.getTimeInterval()));
        }
        requestList.remove(request);
        return future.cancel(true);
    }

    // returns the number of requests cancelled
    public int cancelChannelPostCycles(List<Request> requestList, MessageChannel channel) {
        List<Request> channelRequests = new ArrayList<>();
        int amountCancelled = 0;

        for (Request request : futures.keySet()){
            if(channel.equals(request.getChannel())){
                channelRequests.add(request);
            }
        }
        for(Request request : channelRequests){
            ScheduledFuture future = futures.get(request);
            if(future.cancel(true)){
                requestList.remove(request);
                amountCancelled++;

                if(Config.debug){
                    System.out.println("Cancelled: \"" + request.getSearchTags() + "\" for #" + request.getChannel().getName() +
                            " every " + Util.parseDuration(request.getTimeInterval()));
                }
            }
        }

        return amountCancelled;
    }

    private boolean requestContainsForbiddenTag(Request request) {
        String[] tagArray = request.getSearchTagsWithoutExcludes().split(" ");
        return !Arrays.asList(tagArray).contains("rating:safe") && !Collections.disjoint(Arrays.asList(tagArray), Config.forbidden_tags);
    }

    public Map<Pair<Long,Long>, Pair<Long, Long>> getLastRequestResponseByUser() {
        return lastRequestResponseByUser;
    }
}
