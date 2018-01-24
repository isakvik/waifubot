package hjeisa.waifubot.api;

import hjeisa.waifubot.Config;
import hjeisa.waifubot.exception.FileDeletedException;
import hjeisa.waifubot.exception.ForbiddenTagException;
import hjeisa.waifubot.model.ApiObject;
import hjeisa.waifubot.model.ImageResponse;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.*;
import java.util.*;

import static hjeisa.waifubot.api.Imageboards.imageboards;

public class ApiConnector {

    // gelbooru returns error when getting results past the first 20k results of a search
    private static final int gelbooru_max_image_results = 20000;

    // returns hashmap containing postcount for results on each imageboard
    public static Map<ApiObject, Integer> getPostCounts(String searchTags, int searchTagSize) {
        Map<ApiObject, Integer> postCounts = new HashMap<>();
        String content = "";
        URL url = null;

        for(ApiObject api : imageboards){
            try {
                if(searchTagSize > api.getTagLimit()){
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
                    // danbooru tag page handling
                    Node postCountElement = rootTag.getElementsByTagName("post-count").item(0);
                    postCount = Integer.parseInt(postCountElement.getTextContent());
                    // maximum of 100k results...
                }
                else {
                    postCount = Integer.parseInt(rootTag.getAttribute("count"));
                    if(api.getName().equals("gelbooru")){
                        postCount = Math.min(postCount, gelbooru_max_image_results);
                    }
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
            }
        }
        return postCounts;
    }

    // creates URL based on parameters
    public static URL constructApiUrl(ApiObject api, int limit, int page, String searchTags) {
        try {
            if(api.getName().equals("danbooru")){
                // just use random image functionality, disregard parameters
                return new URL(api.getApiUrl() + "random=true&limit=1" +
                        "&tags="+URLEncoder.encode(searchTags, "UTF-8"));
            }

            String pageKeyword = "pid";

            if(api.getName().equals("konachan") ||
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
    public static ImageResponse parseResponse(ApiObject api, String content, int offset)
            throws FileDeletedException, ForbiddenTagException {
        ImageResponse response = null;

        String ratingStringTag;
        String tagStringTag;
        String fileUrlTag;
        String sampleUrlTag;
        try {
            InputStream is = new ByteArrayInputStream(content.getBytes());
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(is);

            Element posts = doc.getDocumentElement();
            NodeList postNodeList = posts.getChildNodes();
            Node post = postNodeList.item(offset);
            if(api.getName().equals("danbooru")){
                tagStringTag = "tag-string";
                fileUrlTag = "file-url";
                sampleUrlTag = "large-file-url";
            }
            else {
                tagStringTag = "tags";
                fileUrlTag = "file_url";
                sampleUrlTag = "sample_url";
            }
            ratingStringTag = "rating";

            // filter forbidden results
            // TODO: move to own method
            String rating = getContentFromXmlTag(post, ratingStringTag);
            if(rating != null && !rating.equals("s")){
                String postTags = getContentFromXmlTag(post, tagStringTag);
                if(postTags != null){
                    // check if tag list contains any forbidden tags
                    String[] tagList = postTags.split(" ");
                    if(!Collections.disjoint(Arrays.asList(tagList), Config.forbidden_tags)){
                        throw new ForbiddenTagException();
                    }
                }
            }

            String fileUrl = getContentFromXmlTag(post, fileUrlTag);
            if(fileUrl == null)
                throw new FileDeletedException();

            fileUrl = constructFileUrl(api, fileUrl);

            byte[] file = getImageFromUrl(new URL(fileUrl));
            if(file == null){ // if file is too big, get image from sample url instead
                fileUrl = getContentFromXmlTag(post, sampleUrlTag);
                fileUrl = constructFileUrl(api, fileUrl);

                file = getImageFromUrl(new URL(fileUrl));
            }

            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            String postID = getContentFromXmlTag(post, "id");
            String postUrl = api.getPostUrl() + postID;
            String sourceUrl = getContentFromXmlTag(post, "source");

            response = new ImageResponse(file, fileName, postUrl, sourceUrl);

        }
        catch (SAXException | ParserConfigurationException | IOException e) {
            System.err.println("-- Parsing error occurred:");
            System.err.println(content);
            System.err.println("-- Exception:");
            e.printStackTrace();
        }

        return response;
    }

    // get string containing page source
    public static String getPageContent(URL url) {
        String content;
        Scanner in;
        try {
            // TODO: gelbooru has a redirect from http to https while the API is down, leads to xml parsing exception
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", Config.bot_user_agent);
            con.connect();

            in = new Scanner(con.getInputStream(), "UTF-8").useDelimiter("//A");
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

    // private helpers below //

    private static byte[] getImageFromUrl(URL url) throws IOException {
        byte[] temp = new byte[Config.max_image_file_size]; // 4MiB, limited by Discord

        URLConnection con = url.openConnection();
        con.setRequestProperty("User-Agent", Config.bot_user_agent);
        con.connect();
        InputStream in = con.getInputStream();
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

    private static String constructFileUrl(ApiObject api, String fileUrl){
        if(api.getImgUrl() != null){
            fileUrl = api.getImgUrl() + fileUrl.substring(1); // removes unnecessary slash
        }
        // safebooru and konachan do not use protocol in their file links
        if(!fileUrl.startsWith("http")){
            fileUrl = "http:" + fileUrl;
        }
        return fileUrl;
    }

    private static String getContentFromXmlTag(Node post, String tagName){
        if(post.hasAttributes()){
            return post.getAttributes().getNamedItem(tagName).getNodeValue();
        }
        else {
            // danbooru handling
            NodeList postChildNodes = post.getChildNodes();
            for(int i = 0; i < postChildNodes.getLength(); i++){
                Node node = postChildNodes.item(i);
                if(node.getNodeName().equals(tagName)){
                    return node.getTextContent();
                }
            }
        }
        // happens in the case that a file is removed on danbooru, where post has no associated file anymore
        return null;
    }
}
