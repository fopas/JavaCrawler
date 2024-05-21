package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class GetLink {

    private final String url;
    private final String query;
    private final ConnectionFactory factory;
    private static final Logger logger = LoggerFactory.getLogger(GetLink.class);

    public GetLink(String url, ConnectionFactory factory, String query) {
        this.url = url;
        this.factory = factory;
        this.query = query;
    }

    private String computeHash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public void run() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            Document doc = Jsoup.connect(url).get();
            Elements posts = doc.select("article.tm-articles-list__item");

            for (Element post : posts) {
                String link = "https://habr.com" + post.select("a.tm-title__link").attr("href");
                String hash = computeHash(link);

                JSONObject message = new JSONObject();
                message.put("link", link);
                message.put("hash", hash);
                channel.basicPublish("", query, null, message.toString().getBytes(StandardCharsets.UTF_8));
                logger.info("Published link: {}, hash: {}", link, hash);
            }
        } catch (IOException | TimeoutException | NoSuchAlgorithmException e) {
            logger.error("Exception: ", e);
        }
    }
}