package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;
import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PostController {

    // holds all cycle instances
    private Map<Request, ScheduledFuture> futures = new HashMap<>();
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public void schedulePostCycle(Request request) {
        PostMessageTask task = new PostMessageTask(request);
        ScheduledFuture future = service.scheduleAtFixedRate(task, 0, request.getTimeInterval(), TimeUnit.SECONDS);
        futures.put(request, future);

        if(Config.debug){
            System.out.println("Scheduled: \"" + request.getSearchText() + "\" for #" + request.getChannel().getName() +
                    " every " + Util.parseDuration(request.getTimeInterval()));
        }
    }

    public boolean cancelPostCycle(Request request) {
        ScheduledFuture future = futures.get(request);

        if(Config.debug){
            System.out.println("Cancelled: \"" + request.getSearchText() + "\" for #" + request.getChannel().getName() +
                    " every " + Util.parseDuration(request.getTimeInterval()));
        }
        return future.cancel(true);
    }

    // returns the number of requests cancelled
    public int cancelChannelPostCycles(MessageChannel channel) {
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
                amountCancelled++;

                if(Config.debug){
                    System.out.println("Cancelled: \"" + request.getSearchText() + "\" for #" + request.getChannel().getName() +
                            " every " + Util.parseDuration(request.getTimeInterval()));
                }
            }
        }

        return amountCancelled;
    }
}
