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
        String content = "";
        URL url = null;

        for(Map.Entry<String, String> entry : URLs.imageboardApis.entrySet()){
            try {
                url = constructApiUrl(entry.getKey(), 0, 0, searchTags);
                content = getPageContent(url);
                if(content == null){
                    content = "";
                    continue;
                }
                InputStream is = new ByteArrayInputStream(content.getBytes());

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(is);

                Element posts = doc.getDocumentElement();
                Integer postCount = Integer.parseInt(posts.getAttribute("count"));
                postCounts.put(entry.getKey(), postCount);

            } catch (Exception e) {
                if(url != null){
                    System.err.println("[getPostCounts] URL: " + url.toString());
                }
                System.err.println("[getPostCounts] content: " + content);
                System.err.println("[getPostCounts] error message: " +
                        e.getClass().getSimpleName() + ": " + e.getMessage());
                // postCounts.put(entry.getKey(), 0); // seems unnecessary...
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
        /*  scrubs unnecessary whitespace from api results
            this is necessary because konachan has whitespace in their api results,
            and that breaks the XML parsing as it considers <posts> a text content tag */
        return content.replaceAll(">\\s+?<","><");
    }

    // creates URL based on parameters
    public URL constructApiUrl(String imageboard, int limit, int page, String searchTags) throws MalformedURLException {
        try {
            String pageKeyword = "pid";
            if(imageboard.equals("konachan") || imageboard.equals("yandere")){
                if(limit == 0)
                    limit = 1;          // limit of 0 gives default amount of results (50 or so)
                page++;                 // pages are indexed at 1
                pageKeyword = "page";   // key for page ID/offset is page
            }
            return new URL(URLs.imageboardApis.get(imageboard) + "limit="+limit + "&" + pageKeyword+"="+page +
                    "&tags="+URLEncoder.encode(searchTags, "UTF-8"));
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
            fileUrl = post.getAttributes().getNamedItem("file_url").getNodeValue();
            // safebooru and konachan do not use protocol in their file links
            if(!fileUrl.startsWith("http"))
                fileUrl = "http:" + fileUrl;

            byte[] file = getImageFromUrl(new URL(fileUrl));
            if(file == null){ // if file is too big, get image from sample url instead
                fileUrl = post.getAttributes().getNamedItem("sample_url").getNodeValue();
                // redo check
                if(!fileUrl.startsWith("http"))
                    fileUrl = "http:" + fileUrl;

                file = getImageFromUrl(new URL(fileUrl));
            }

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            String postID = post.getAttributes().getNamedItem("id").getNodeValue();
            String postUrl = URLs.imageboardPostUrls.get(imageboard) + postID;

            String sourceUrl = post.getAttributes().getNamedItem("source").getNodeValue();

            response = new ImageResponse(file, fileName, postUrl, sourceUrl);

        }
        catch (SAXException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        return response;
    }

    public byte[] getImageFromUrl(URL url) throws IOException {
        byte[] temp = new byte[Config.max_image_file_size]; // 4MiB, limited by Discord

        InputStream in = url.openStream();
        int i;
        int read;
        for(i = 0; (read = in.read()) != -1; i++){
            if(i < Config.max_image_file_size){
                temp[i] = (byte) read;
            }
            else {
                in.close();
                return null;
            }
        }

        byte[] img = new byte[i+1];
        System.arraycopy(temp,0,img,0,i);
        in.close();
        return img;
    }
}
