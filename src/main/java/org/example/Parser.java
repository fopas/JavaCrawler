package org.example;

import com.rabbitmq.client.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Parser implements Runnable {

    private final ConnectionFactory factory;
    private final String queryLink;
    private final String queryInfo;
    private static final Logger logger = LoggerFactory.getLogger(Parser.class);
    private Channel channel;

    public Parser(ConnectionFactory factory, String queryLink, String queryInfo) {
        this.factory = factory;
        this.queryLink = queryLink;
        this.queryInfo = queryInfo;
    }

    private JSONObject parseArticle(String url, String hash) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int statusCode = connection.getResponseCode();

            switch (statusCode) {
                case 200:
                    Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url);
                    String title = doc.select("h1.tm-title.tm-title_h1 span").text().trim();
                    String author = doc.select("a.tm-user-info__username").text().trim();
                    String time = doc.select("span.tm-article-datetime-published time").attr("datetime");
                    StringBuilder text = new StringBuilder();
                    doc.select("div.article-formatted-body p").forEach(paragraph -> text.append(paragraph.text()).append("\n"));

                    JSONObject articleJson = new JSONObject();
                    articleJson.put("hash", hash);
                    articleJson.put("url", url);
                    articleJson.put("title", title);
                    articleJson.put("author", author);
                    articleJson.put("time", time);
                    articleJson.put("text", text.toString());

                    return articleJson;
                case 403:
                    logger.error("HTTP 403 Forbidden: Access is denied for URL {}", url);
                    throw new IOException("HTTP 403 Forbidden: Access is denied");
                case 404:
                    logger.error("HTTP 404 Not Found: The requested URL {} was not found on this server", url);
                    throw new IOException("HTTP 404 Not Found: URL not found");
                case 503:
                    logger.error("HTTP 503 Service Unavailable: The server is currently unable to handle the request for URL {}", url);
                    throw new IOException("HTTP 503 Service Unavailable: Service unavailable");
                default:
                    logger.error("HTTP error code: {}", statusCode);
                    throw new IOException("HTTP error code: " + statusCode);
            }
        } catch (IOException e) {
            logger.error("Error parsing article from URL: " + url, e);
        }
        return null;
    }

    private void handleDelivery(GetResponse delivery) throws IOException {
        try {
            String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
            JSONObject jsonObjectLinkHash = new JSONObject(messageBody);
            String link = jsonObjectLinkHash.getString("link");
            String hash = jsonObjectLinkHash.getString("hash");

            JSONObject articleJson = parseArticle(link, hash);
            if (articleJson != null) {
                String json = articleJson.toString();
                channel.basicPublish("", queryInfo, null, json.getBytes(StandardCharsets.UTF_8));
                logger.info("Published article info: {}", json);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            logger.error("Error handling delivery", e);
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
        }
    }

    @Override
    public void run() {
        try {
            Connection connection = factory.newConnection();
            this.channel = connection.createChannel();

            try {
                channel.queueDeclare(queryLink, false, false, false, null);

                GetResponse response = channel.basicGet(queryLink, false);
                while (response != null) {
                    handleDelivery(response);
                    response = channel.basicGet(queryLink, false);
                }

            } catch (IOException e) {
                logger.error("Error in run method", e);
            } finally {
                channel.close();
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            logger.error("Error establishing connection", e);
        }
    }
}
