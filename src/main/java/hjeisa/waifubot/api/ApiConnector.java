package hjeisa.waifubot.api;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.model.ApiObject;
import hjeisa.waifubot.model.ImageResponse;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static hjeisa.waifubot.api.Imageboards.imageboards;

public class ApiConnector {

    // returns hashmap containing postcount for results on each imageboard
    public static Map<ApiObject, Integer> getPostCounts(String searchTags, int searchTagSize) {
        Map<ApiObject, Integer> postCounts = new HashMap<>();
        String content = "";
        URL url = null;

        for(ApiObject api : imageboards){
            try {
                if(searchTagSize > api.getTagLimit()){
                    postCounts.put(api, 0);
                    if(Config.debug)
                        System.out.println("Skipped api " + api.getName() + " because of tag limit.");
                    continue;
                }
                if(api.getTagUrl() != null){
                    // danbooru handling. done here because tag URL is only used for post count retrieval
                    if(!searchTags.startsWith("rating"))
                        url = new URL(api.getTagUrl() + searchTags);
                    else
                        continue;
                }
                else {
                    url = constructApiUrl(api, 0, 0, searchTags);
                }

                content = getPageContent(url);
                if(content == null){
                    content = "";
                    continue;
                }
                InputStream is = new ByteArrayInputStream(content.getBytes());

                DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = dBuilder.parse(is);

                Element rootTag = doc.getDocumentElement();
                Integer postCount;

                if(rootTag.getTagName().equals("tags")){
                    // danbooru handling
                    Node postCountElement = rootTag.getElementsByTagName("post-count").item(0);
                    postCount = Integer.parseInt(postCountElement.getTextContent());
                }
                else {
                    postCount = Integer.parseInt(rootTag.getAttribute("count"));
                }

                postCounts.put(api, postCount);

            }
            catch (SAXException saxe){
                if(url != null){
                    System.err.println("[getPostCounts] URL: " + url.toString());
                }
            }
            catch (Exception e) {
                System.err.println("[getPostCounts] General exception occurred.");
                if(url != null){
                    System.err.println("[getPostCounts] URL: " + url.toString());
                }
                System.err.println("[getPostCounts] content: " + content);
                System.err.println("[getPostCounts] error message: " +
                        e.getClass().getSimpleName() + ": " + e.getMessage());
                // postCounts.put(api, 0); // seems unnecessary...
            }
        }
        return postCounts;
    }

    // get string containing page source
    public static String getPageContent(URL url) {
        String content;
        Scanner in;
        try {
            // TODO: gelbooru has a redirect from http to https while the API is down, leads to xml parsing exception
            in = new Scanner(url.openStream(), "UTF-8").useDelimiter("//A");
            content = (in.hasNext() ? in.next() : "");
        } catch (Exception e) {
            System.err.println("[ERROR] getPageContent for url " + url +  " failed.");
            e.printStackTrace();
            return null;
        }
        in.close();
        /*  scrubs unnecessary whitespace from api results
            this is necessary because konachan/danbooru have whitespace in their api results,
            and that breaks the XML parsing as it considers <posts> a text content tag */
        return content.replaceAll(">\\s+?<","><");
    }

    // creates URL based on parameters
    public static URL constructApiUrl(ApiObject api, int limit, int page, String searchTags) {
        try {
            String pageKeyword = "pid";
            if(api.getName().equals("danbooru") ||
               api.getName().equals("konachan") ||
               api.getName().equals("yandere")){
                if(limit == 0)
                    limit = 1;          // limit of 0 gives default amount of results (20)
                page++;                 // pages are indexed at 1
                pageKeyword = "page";   // key for page ID/offset is page
            }
            return new URL(api.getApiUrl() + "limit="+limit + "&" + pageKeyword+"="+page +
                    "&tags="+URLEncoder.encode(searchTags, "UTF-8"));
        }
        catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // returns object holding all info needed to post an image
    public static ImageResponse parseResponse(ApiObject api, String content) {
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

                file = getImageFromUrl(new URL(fileUrl));
            }

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            String postID = post.getAttributes().getNamedItem("id").getNodeValue();
            String postUrl = api.getPostUrl() + postID;

            String sourceUrl = post.getAttributes().getNamedItem("source").getNodeValue();

            response = new ImageResponse(file, fileName, postUrl, sourceUrl);

        }
        catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    public static byte[] getImageFromUrl(URL url) throws IOException {
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
