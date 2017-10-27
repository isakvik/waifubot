package hjeisa.waifubot;

import hjeisa.waifubot.model.Request;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PostController {
    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public void startPostCycle(Request request) {
        PostMessageTask task = new PostMessageTask(request);
        service.scheduleAtFixedRate(task, 0, request.getTimeInterval(), TimeUnit.SECONDS);
    }
}
