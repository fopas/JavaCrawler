package org.example;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Parser implements Runnable {

    private final ConnectionFactory factory;
    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    public Parser(ConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.basicQos(1);

            // Начало прослушивания очереди "link"
//            channel.basicConsume("link", false, deliverCallback, consumerTag -> { });
//
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String link = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    // Подтверждение получения сообщения
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    // Парсинг и публикация статьи
                    Article article = parseArticle(link);
                    if (article != null) {
                        String json = new Gson().toJson(article);
                        channel.basicPublish("", "info", null, json.getBytes());
                        logger.info("Published article info: {}", json);
                    }

                    // Подтверждение получения сообщения
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException e) {
                    logger.error("Error occurred in parsing: ", e);
                }
            };
        } catch (IOException | TimeoutException e) {
            logger.error("Error occurred in parsing: ", e);
        }
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
}
