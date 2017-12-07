package hjeisa.waifubot.model;

public class ApiObject {

    private String name;
    private String apiUrl;
    private String postUrl;
    private int tagLimit;

    // only danbooru uses these
    private String imgUrl;
    private String tagUrl;

    public ApiObject(String name, String apiUrl, String postUrl, int tagLimit){
        this.name = name;
        this.apiUrl = apiUrl;
        this.postUrl = postUrl;
        this.imgUrl = null;
        this.tagUrl = null;
        this.tagLimit = tagLimit;
    }
    public ApiObject(String name, String apiUrl, String postUrl, String imgUrl, String tagUrl, int tagLimit){
        this.name = name;
        this.apiUrl = apiUrl;
        this.postUrl = postUrl;
        this.imgUrl = imgUrl;
        this.tagUrl = tagUrl;
        this.tagLimit = tagLimit;
    }

    public String getName() {
        return name;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getTagUrl() {
        return tagUrl;
    }

    public int getTagLimit() {
        return tagLimit;
    }
}
