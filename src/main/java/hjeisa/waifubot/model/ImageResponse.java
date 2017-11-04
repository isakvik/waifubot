package hjeisa.waifubot.model;

public class ImageResponse {
    // value object for holding URLs

    private byte[] imageData;
    private String postURL;
    private String sourceURL;

    public ImageResponse(byte[] imageData, String postURL, String sourceURL) {
        this.imageData = imageData;
        this.postURL = postURL;
        this.sourceURL = sourceURL;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public String getPostURL() {
        return postURL;
    }

    public String getSourceURL() {
        return sourceURL;
    }
}
