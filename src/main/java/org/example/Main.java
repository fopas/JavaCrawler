package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String URL = "https://habr.com/ru/news/";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static final String QUERY_LINK = "link";
    public static final String QUERY_INFO = "info";
    public static final int NUMBER_THREAD = 3;

    public static void main(String[] args) throws Exception {
        logger.info("Start app");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setVirtualHost("/");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUERY_LINK, false, false, false, null);
        channel.queueDeclare(QUERY_INFO, false, false, false, null);
        channel.close();
        connection.close();

        logger.info("The collection of links from the main url page has begun");
        GetLink getLink = new GetLink(URL, factory, QUERY_LINK);
        getLink.run();
        logger.info("The collection of links from the main url page has ended");

        logger.info("The collection of information from each URL page has begun");

        // Создаем пул из NUMBER_THREAD потоков
        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREAD);
        for (int i = 0; i < NUMBER_THREAD; i++) {
            executorService.submit(new Parser(factory, QUERY_LINK, QUERY_INFO));
        }

        // Ожидаем завершения работы всех парсеров
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        logger.info("The collection of information from each URL page is completed");

        ElasticSearchManager elasticsearchManager = new ElasticSearchManager();
        elasticsearchManager.init();
        logger.info("The index in the ElasticSearch database is initialized");

        logger.info("Sending data to the database has started");
        PublishInfo publishInfo = new PublishInfo(factory, QUERY_INFO, elasticsearchManager);
        publishInfo.run();
        System.out.print("\nувуууууууууууу");
        logger.info("Sending data to the database has started");

        logger.info("App stopped");
    }
}
