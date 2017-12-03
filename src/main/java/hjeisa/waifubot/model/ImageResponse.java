package hjeisa.waifubot.model;

public class ImageResponse {
    // value object for holding images

    private byte[] imageData;
    private String fileName;
    private String postURL;
    private String sourceURL;

    public ImageResponse(byte[] imageData, String fileName, String postURL, String sourceURL) {
        this.imageData = imageData;
        this.fileName = fileName;
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

    public String getFileName() {
        return fileName;
    }
}
