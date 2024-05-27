package org.example;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * Класс для публикации информации о статьях в базу данных Elasticsearch.
 * Считывает сообщения из очереди RabbitMQ и добавляет их в Elasticsearch.
 */
public class PublishInfo {
    private final ConnectionFactory factory;
    private final String queryInfo;
    private static final Logger logger = LoggerFactory.getLogger(PublishInfo.class);
    private Channel channel;
    private final ElasticSearchManager elasticsearchManager;

    /**
     * Конструктор.
     *
     * @param factory             Фабрика соединений с RabbitMQ.
     * @param queryInfo           Очередь, из которой считываются сообщения.
     * @param elasticsearchManager Менеджер для работы с Elasticsearch.
     */
    public PublishInfo(ConnectionFactory factory, String queryInfo, ElasticSearchManager elasticsearchManager) {
        this.factory = factory;
        this.queryInfo = queryInfo;
        this.elasticsearchManager = elasticsearchManager;
    }

    /**
     * Обрабатывает доставку сообщения из очереди.
     *
     * @param delivery Сообщение из очереди.
     * @throws IOException Если возникает ошибка при обработке сообщения.
     */
    private void handleDelivery(GetResponse delivery) throws IOException {
        try {
            String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
            JSONObject jsonObjectInfo = new JSONObject(messageBody);
            Article article = Article.fromJsonString(jsonObjectInfo);

            boolean documentExists = elasticsearchManager.checkDocumentExists(article);

            if (!documentExists) {
                elasticsearchManager.addDocument(article);
            } else {
                logger.info("Document with hash " + article.getHash() + " already exists in Elasticsearch. Skipping processing.");
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error("Error handling delivery", e);
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
        }
    }

    /**
     * Основной метод выполнения класса.
     * Устанавливает соединение с RabbitMQ, считывает сообщения из очереди и обрабатывает их.
     *
     * @throws IOException Если возникает ошибка при установлении соединения.
     * @throws TimeoutException Если возникает ошибка тайм-аута при установлении соединения.
     */
    public void run() throws IOException, TimeoutException {
        Connection connection = factory.newConnection();
        this.channel = connection.createChannel();

        try {
            channel.queueDeclare(queryInfo, false, false, false, null);

            GetResponse response = channel.basicGet(queryInfo, false);
            while (response != null) {
                handleDelivery(response);
                response = channel.basicGet(queryInfo, false);
            }

        } catch (IOException e) {
            logger.error("Error in run method", e);
        } finally {
            channel.close();
            connection.close();
        }
    }
}
