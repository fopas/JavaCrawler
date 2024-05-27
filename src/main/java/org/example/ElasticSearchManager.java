package org.example;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;

/**
 * Класс для управления взаимодействием с базой данных Elasticsearch.
 * Включает методы для проверки существования индекса, создания индекса,
 * проверки существования документа и добавления документа.
 */
public class ElasticSearchManager {
    private static final Logger logger = LogManager.getLogger(ElasticSearchManager.class);
    private final ElasticsearchClient client;
    private static final String INDEX_NAME = "article";
    private static final String SERVER_URL = "http://localhost:9200";

    /**
     * Конструктор.
     * Инициализирует клиент для взаимодействия с Elasticsearch.
     */
    public ElasticSearchManager() {
        this.client = new ElasticsearchClient(
                new RestClientTransport(
                        RestClient.builder(HttpHost.create(SERVER_URL)).build(),
                        new JacksonJsonpMapper(JsonMapper.builder().build())
                )
        );
    }

    /**
     * Инициализация Elasticsearch.
     * Проверяет существование индекса и создает его при отсутствии.
     *
     * @throws IOException Если возникает ошибка при взаимодействии с Elasticsearch.
     */
    public void init() throws IOException {
        if (!checkIfIndexExists()) {
            createIndex();
        }
    }

    /**
     * Проверяет существование индекса.
     *
     * @return true, если индекс существует; false, если индекс отсутствует.
     * @throws IOException Если возникает ошибка при проверке индекса.
     */
    private boolean checkIfIndexExists() throws IOException {
        BooleanResponse response = client.indices().exists(i -> i.index(INDEX_NAME));
        return response.value();
    }

    /**
     * Создает индекс в Elasticsearch.
     */
    private void createIndex() {
        try {
            client.indices().create(i -> i.index(INDEX_NAME)
                    .mappings(m -> m.properties("hash", p -> p.keyword(d -> d))
                            .properties("url", p -> p.text(d -> d))
                            .properties("title", p -> p.text(d -> d))
                            .properties("text", p -> p.text(d -> d))
                            .properties("author", p -> p.keyword(d -> d))
                            .properties("time", p -> p.date(d -> d))
                    ));
            logger.info(String.format("Index %s created successfully", INDEX_NAME));
        } catch (IOException e) {
            logger.error(String.format("Error creating index %s: %s", INDEX_NAME, e.getMessage()));
        }
    }

    /**
     * Проверяет существование документа в Elasticsearch по его хешу.
     *
     * @param article Статья, для которой проверяется существование.
     * @return true, если документ существует; false, если документ отсутствует.
     * @throws IOException Если возникает ошибка при проверке документа.
     */
    public boolean checkDocumentExists(Article article) throws IOException {
        SearchResponse<Object> searchResponse = client.search(builder ->
                        builder.index(INDEX_NAME)
                                .query(query -> query.term(termQuery -> termQuery.field("hash").value(article.getHash()))),
                Object.class);

        return searchResponse.hits().total().value() != 0;
    }

    /**
     * Добавляет документ в Elasticsearch.
     *
     * @param article Статья, которая добавляется в Elasticsearch.
     */
    public void addDocument(Article article) {
        try {
            IndexResponse response = client.index(index -> index
                    .index(INDEX_NAME)
                    .document(article));
            logger.info("Document indexed successfully with hash: " + article.getHash());
        } catch (IOException e) {
            logger.error("Error indexing document with hash " + article.getHash(), e);
        }
    }

    /**
     * Закрывает соединение с Elasticsearch.
     */
    public void close() {
        try {
            if (client != null) {
                ((RestClientTransport) client._transport()).restClient().close();
                logger.info("Elasticsearch client closed");
            }
        } catch (IOException e) {
            logger.error("Error closing Elasticsearch client", e);
        }
    }
}
