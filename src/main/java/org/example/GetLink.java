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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

/**
 * Класс для сбора ссылок с заданной страницы.
 * Скачивает страницу, извлекает ссылки на статьи и публикует их в очередь RabbitMQ.
 */
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

    /**
     * Вычисление хеша SHA-256 для строки.
     * @param input Входная строка.
     * @return Хешированная строка.
     * @throws NoSuchAlgorithmException Если алгоритм SHA-256 недоступен.
     */
    private String computeHash(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    /**
     * Получение HTML-документа по URL.
     * @param url URL страницы.
     * @return HTML-документ.
     * @throws IOException Если возникает ошибка при загрузке страницы.
     */
    private Document getDocument(String url) throws IOException {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int statusCode = connection.getResponseCode();
        switch (statusCode) {
            case 200:
                logger.info("HTTP 200 for URL {}", url);
                return Jsoup.parse(connection.getInputStream(), "UTF-8", url);
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
    }

    /**
     * Метод для запуска процесса сбора ссылок.
     * Загружает страницу, извлекает ссылки на статьи, вычисляет их хеши и публикует в очередь RabbitMQ.
     */
    public void run() {
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            Document doc = getDocument(url);
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
