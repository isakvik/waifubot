package hjeisa.waifubot.web;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ImageResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConnectionHandler {

    // TODO: figure out structure of things
    /*
        three apis
        from postmessagetask, get random image from one booru and retrieve image url(perhaps source)
        how:
            send request to each api(limit 0), collect post counts for tags
            get random int, decide what booru to use
            get picture from api with page id matching int, retrieve image(+ source url)
     */

    // returns hashmap containing postcount for each
    public Map<String, Integer> getPostCounts(String searchTags) {
        Map<String, Integer> postCounts = new HashMap<>();

        for(Map.Entry<String, String> entry : URLs.imageboardApis.entrySet()){
            try {
                String content = getPageContent(constructApiUrl(entry, 0, 0, searchTags)); // limit=0 gives us no post results

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(content);

                Element posts = doc.getElementById("posts");
                Integer postCount = Integer.parseInt(posts.getAttribute("count"));
                postCounts.put(entry.getKey(), postCount);

            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        return postCounts;
    }

    // get string containing page source
    public String getPageContent(URL url) throws IOException {
        String content = "";
        Scanner in = new Scanner(url.openStream(), "UTF-8").useDelimiter("//A");
        content = (in.hasNext() ? in.next() : "");

        in.close();
        return content;
    }

    // creates URL based on parameters
    public URL constructApiUrl(Map.Entry<String, String> imageboard, int limit, int page, String searchTags) {
        try {
            if(imageboard.getKey().equals("konachan")){
                page++; // konachan's pages are indexed at 1
                return new URL(URLEncoder.encode(
                        imageboard.getValue() + "limit=" + limit + "&page=" + page + "&tags=" + searchTags, "UTF-8"));
            }
            else if(imageboard.getKey().equals("safebooru")){
                return new URL(URLEncoder.encode(
                        imageboard.getValue() + "limit=" + limit + "&pid=" + page + "&tags=" + searchTags, "UTF-8"));
            }
            else if(imageboard.getKey().equals("gelbooru")){
                // rating:safe added to tags to ensure results are safe for work
                return new URL(URLEncoder.encode(
                        imageboard.getValue() + "limit=" + limit + "&pid=" + page + "&tags=rating:safe " + searchTags, "UTF-8"));
            }
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // returns object holding all info needed to post an image
    public ImageResponse parseResponse(Map.Entry<String, String> imageboard, String content) {
        ImageResponse response = null;

        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(content);

            Element posts = doc.getElementById("posts");
            Node post = posts.getFirstChild();

            String fileUrl = post.getAttributes().getNamedItem("file_url").getNodeValue();
            byte[] file = getImageFromUrl(new URL(fileUrl));
            if(file == null){ // if file is too big, get image from sample url instead
                fileUrl = post.getAttributes().getNamedItem("sample_url").getNodeValue();
                file = getImageFromUrl(new URL(fileUrl));
            }

            String postID = post.getAttributes().getNamedItem("id").getNodeValue();
            String postUrl = URLs.imageboardPostUrls.get(imageboard.getKey()) + postID;

            String sourceUrl = post.getAttributes().getNamedItem("source").getNodeValue();

            response = new ImageResponse(file, postUrl, sourceUrl);


        } catch (SAXException | IOException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        return response;
    }

    public byte[] getImageFromUrl(URL url) throws IOException {
        byte[] img = new byte[Config.max_image_file_size]; // 8MB, limited by Discord

        InputStream in = url.openStream();
        int read;
        for(int i = 0; (read = in.read()) != -1; i++){
            if(i < 8 * 1024 * 1024){
                img[i] = (byte) read;
            }
            else {
                in.close();
                return null;
            }
        }
        in.close();
        return img;
    }
}
