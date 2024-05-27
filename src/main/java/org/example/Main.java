package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Главный класс для запуска приложения.
 * Содержит основной метод main, который инициирует все процессы:
 * - сбор ссылок
 * - парсинг страниц по ссылкам
 * - отправка данных в базу данных Elasticsearch
 */
public class Main {

    private static final String URL = "https://habr.com/ru/news/";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static final String QUERY_LINK = "link";
    public static final String QUERY_INFO = "info";

    public static void main(String[] args) throws Exception {
        logger.info("Start app");

        // Настройка фабрики соединений с RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Объявление очередей в RabbitMQ
        channel.queueDeclare(QUERY_LINK, false, false, false, null);
        channel.queueDeclare(QUERY_INFO, false, false, false, null);
        channel.close();
        connection.close();

        // Сбор ссылок с основной страницы
        logger.info("The collection of links from the main URL page has begun");
        GetLink getLink = new GetLink(URL, factory, QUERY_LINK);
        getLink.run();
        logger.info("The collection of links from the main URL page has ended");

        // Парсинг информации по каждой ссылке
        logger.info("The collection of information from each URL page has begun");

        // Создаем пул из 3 потоков для парсинга
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            executorService.submit(new Parser(factory, QUERY_LINK, QUERY_INFO));
        }

        // Ожидание завершения работы всех парсеров
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        logger.info("The collection of information from each URL page is completed");

        // Инициализация и отправка данных в Elasticsearch
        ElasticSearchManager elasticsearchManager = new ElasticSearchManager();
        try {
            elasticsearchManager.init();
            logger.info("The index in the ElasticSearch database is initialized");

            logger.info("Sending data to the database has started");
            PublishInfo publishInfo = new PublishInfo(factory, QUERY_INFO, elasticsearchManager);
            publishInfo.run();
            logger.info("Sending data to the database has started");
        } finally {
            elasticsearchManager.close();
        }

        logger.info("App stopped");
    }
}
