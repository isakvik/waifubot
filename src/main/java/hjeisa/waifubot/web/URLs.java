package hjeisa.waifubot.web;

import java.util.HashMap;
import java.util.Map;

public class URLs {

    // TODO: add danbooru api
    // TODO: convert and initialize as model objects

    public static Map<String, String> imageboardApis = createImageboardApiMap();
    private static Map<String, String> createImageboardApiMap(){
        Map<String, String> map = new HashMap<>();
        map.put("safebooru", "http://safebooru.org/index.php?page=dapi&s=post&q=index&");
        map.put("gelbooru", "https://gelbooru.com/index.php?page=dapi&s=post&q=index&");
        map.put("konachan", "http://konachan.com/post.xml?");
        map.put("yandere", "https://yande.re/post.xml?");

        return map;
    }

    /*
     * posted as message after picture is sent, never accessed
     */
    public static Map<String, String> imageboardPostUrls = createPostUrlMap();
    private static Map<String, String> createPostUrlMap(){
        Map<String, String> map = new HashMap<>();
        map.put("safebooru", "https://safebooru.org/index.php?page=post&s=view&id=");
        map.put("gelbooru", "https://gelbooru.org/index.php?page=post&s=view&id=");
        map.put("konachan", "https://konachan.com/post/show/");
        map.put("yandere", "https://yande.re/post/show/");

        return map;
    }
}
