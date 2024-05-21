package org.example;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Parser {

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

    private Article parseArticle(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            String title = doc.select("h1.tm-title.tm-title_h1 span").text().trim();
            String author = doc.select("a.tm-user-info__username").text().trim();
            String time = doc.select("span.tm-article-datetime-published time").attr("datetime");
            StringBuilder text = new StringBuilder();
            doc.select("div.article-formatted-body p").forEach(paragraph -> text.append(paragraph.text()).append("\n"));
            return new Article(title, author, time, text.toString(), url);
        } catch (IOException e) {
            logger.error("Error parsing article from URL: " + url, e);
        }
        return null;
    }

    private void handleDelivery(GetResponse delivery) throws IOException {
        try {
            String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
            //написать сравнение hash
            JsonObject jsonObject_link_hash = JsonParser.parseString(messageBody).getAsJsonObject();
            String link = jsonObject_link_hash.get("link").getAsString();
            Article article = parseArticle(link);
            if (article != null) {
                String json = new Gson().toJson(article);
                channel.basicPublish("", queryInfo, null, json.getBytes(StandardCharsets.UTF_8));
                logger.info("Published article info: {}", json);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        } catch (Exception e) {
            logger.error("Error handling delivery", e);
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
        }
    }

    public void run() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel(); // Initialize the class-level channel

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
    }
}
