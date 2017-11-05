package hjeisa.waifubot.web;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ImageResponse;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ConnectionHandler {

    // returns hashmap containing postcount for results on each imageboard
    public Map<String, Integer> getPostCounts(String searchTags) {
        Map<String, Integer> postCounts = new HashMap<>();

        for(Map.Entry<String, String> entry : URLs.imageboardApis.entrySet()){
            try {
                String content = getPageContent(constructApiUrl(entry.getKey(), 0, 0, searchTags, false)); // limit=0 gives us no post results
                if(content == null)
                    content = "";
                InputStream is = new ByteArrayInputStream(content.getBytes());

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(is);

                Element posts = doc.getDocumentElement();
                Integer postCount = Integer.parseInt(posts.getAttribute("count"));
                postCounts.put(entry.getKey(), postCount);

            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
        return postCounts;
    }

    // get string containing page source
    public String getPageContent(URL url) {
        String content = "";
        Scanner in = null;
        try {
            in = new Scanner(url.openStream(), "UTF-8").useDelimiter("//A");
            content = (in.hasNext() ? in.next() : "");
        } catch (Exception e) {
            System.err.println("[ERROR] getPageContent for url " + url +  " failed.");
            e.printStackTrace();
            return null;
        }
        in.close();
        return content.substring(content.lastIndexOf("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
    }

    // creates URL based on parameters
    public URL constructApiUrl(String imageboard, int limit, int page, String searchTags, boolean nsfw) throws MalformedURLException {
        String[] tags = searchTags.split(" ");
        for(String tag : tags)
            if(tag.equals("rating:explicit") || tag.equals("rating:questionable"))
                nsfw = true;

        try {
            if(imageboard.equals("konachan")){
                if(limit == 0)
                    limit++; // limit of 0 doesn't work
                page++; // konachan's pages are indexed at 1
                // rating:safe added to tags to ensure results are safe for work
                return new URL(URLs.imageboardApis.get(imageboard) + "limit=" + limit + "&page=" + page + "&tags=" +
                        URLEncoder.encode((nsfw ? "" : "rating:safe ") + searchTags, "UTF-8"));
            }
            else if(imageboard.equals("safebooru")){
                return new URL(URLs.imageboardApis.get(imageboard) + "limit=" + limit + "&pid=" + page + "&tags=" +
                        URLEncoder.encode(searchTags, "UTF-8"));
            }
            else if(imageboard.equals("gelbooru")){
                // rating:safe added to tags to ensure results are safe for work
                return new URL(URLs.imageboardApis.get(imageboard) + "limit=" + limit + "&pid=" + page + "&tags=" +
                        URLEncoder.encode((nsfw ? "" : "rating:safe ") + searchTags, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    // returns object holding all info needed to post an image
    public ImageResponse parseResponse(String imageboard, String content) throws IOException {
        ImageResponse response = null;

        try {
            InputStream is = new ByteArrayInputStream(content.getBytes());
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            Element posts = doc.getDocumentElement();
            Node post = posts.getFirstChild();

            String fileUrl;
            if(imageboard.equals("safebooru"))  // safebooru does not use protocol in their
                fileUrl = "http:" + post.getAttributes().getNamedItem("file_url").getNodeValue();
            else
                fileUrl = post.getAttributes().getNamedItem("file_url").getNodeValue();

            byte[] file = getImageFromUrl(new URL(fileUrl));
            if(file == null){ // if file is too big, get image from sample url instead
                fileUrl = post.getAttributes().getNamedItem("sample_url").getNodeValue();
                file = getImageFromUrl(new URL(fileUrl));
            }

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            String postID = post.getAttributes().getNamedItem("id").getNodeValue();
            String postUrl = URLs.imageboardPostUrls.get(imageboard) + postID;

            String sourceUrl = post.getAttributes().getNamedItem("source").getNodeValue();

            response = new ImageResponse(file, fileName, postUrl, sourceUrl);

        } catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        return response;
    }

    public byte[] getImageFromUrl(URL url) throws IOException {
        byte[] img = new byte[Config.max_image_file_size]; // 4MiB, limited by Discord

        InputStream in = url.openStream();
        int read;
        for(int i = 0; (read = in.read()) != -1; i++){
            if(i < Config.max_image_file_size){
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
