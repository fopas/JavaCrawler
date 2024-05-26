package org.example;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class PublishInfo {
    private final ConnectionFactory factory;
    private final String queryInfo;
    private static final Logger logger = LoggerFactory.getLogger(PublishInfo.class);
    private Channel channel;
    private final ElasticSearchManager elasticsearchManager;//тут он желтым выделяет

    public PublishInfo(ConnectionFactory factory, String queryInfo, ElasticSearchManager elasticsearchManager) {
        this.factory = factory;
        this.queryInfo = queryInfo;
        this.elasticsearchManager = elasticsearchManager;
    }

    private void handleDelivery(GetResponse delivery) throws IOException {
        try {
            String messageBody = new String(delivery.getBody(), StandardCharsets.UTF_8);
            JSONObject jsonObjectInfo = new JSONObject(messageBody);
            Article article = Article.fromJsonString(jsonObjectInfo);

            // Проверяем наличие документа в Elasticsearch по его идентификатору (хешу)
            boolean documentExists = elasticsearchManager.checkDocumentExists(article);

            // Если документа нет, добавляем его в Elasticsearch
            if (!documentExists) elasticsearchManager.addDocument(article);
            else {
                // Документ уже существует, можно пропустить его обработку
                logger.info("Document with hash " + article.getHash() + " already exists in Elasticsearch. Skipping processing.");
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            System.out.print("\naaald!");
        } catch (Exception e) {
            logger.error("Error handling delivery", e);
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
        }
    }


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
