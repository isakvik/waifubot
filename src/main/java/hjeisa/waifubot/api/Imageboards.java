package hjeisa.waifubot.api;

import hjeisa.waifubot.model.ApiObject;

import java.util.*;

public class Imageboards {

    // TODO: convert and initialize as model objects

    public static final List<ApiObject> imageboards;
    static {
        List<ApiObject> list = new ArrayList<>();
        list.add(new ApiObject("safebooru",
                "http://safebooru.org/index.php?page=dapi&s=post&q=index&",
                "https://safebooru.org/index.php?page=post&s=view&id=",
                50));
        list.add(new ApiObject("gelbooru",
                "http://gelbooru.com/index.php?page=dapi&s=post&q=index&",
                "https://gelbooru.org/index.php?page=post&s=view&id=",
                50));
        list.add(new ApiObject("konachan",
                "http://konachan.com/post.xml?",
                "https://konachan.com/post/show/",
                6));
        list.add(new ApiObject("yandere",
                "https://yande.re/post.xml?",
                "https://yande.re/post/show/",
                6));

        list.add(new ApiObject("danbooru",
                "https://danbooru.donmai.us/posts.xml?",
                "https://danbooru.donmai.us/posts/",
                "https://danbooru.donmai.us/tags.xml?search[name]=",
                1));

        imageboards = Collections.unmodifiableList(list);
    }
}
