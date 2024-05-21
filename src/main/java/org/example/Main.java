package org.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    private static final String URL = "https://habr.com/ru/news/";
    private static final String QUERY_LINK = "link";
    private static final String QUERY_INFO_LINK = "info";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

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

//        Connection connection = factory.newConnection();
//        Channel channel = connection.createChannel();
//        channel.queueDeclare("info", false, false, false, null);
//        String message = "ААА";
//        channel.basicPublish("", "info", null, message.getBytes());
//        channel.close();
//        connection.close();

        channel.queueDeclare(QUERY_LINK, false, false, false, null);
        channel.queueDeclare(QUERY_INFO_LINK, false, false, false, null);
        channel.close();
        connection.close();

        GetLink getLink = new GetLink(URL, factory, QUERY_LINK);
        getLink.run();

//        Parser parser = new Parser(factory);
//        parser.run();
//
//        Thread linkThread = new Thread(getLink);
//        Thread parserThread = new Thread(parser);
//
//        linkThread.start();
//        parserThread.start();
//
//        linkThread.join();
//        parserThread.join();

        logger.info("Service stopped");
    }
}
