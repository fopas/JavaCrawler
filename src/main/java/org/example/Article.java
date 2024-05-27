package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

/**
 * Класс, представляющий статью с её основными атрибутами.
 */
public class Article {
    private String hash;
    private String url;
    private String title;
    private String text;
    private String author;
    private String time;

    /**
     * Конструктор класса Article.
     *
     * @param hash   Хеш статьи.
     * @param url    URL статьи.
     * @param title  Заголовок статьи.
     * @param text   Текст статьи.
     * @param author Автор статьи.
     * @param time   Время публикации статьи.
     */
    public Article(String hash, String url, String title, String text, String author, String time) {
        this.hash = hash;
        this.url = url;
        this.title = title;
        this.text = text;
        this.author = author;
        this.time = time;
    }

    // Геттеры и сеттеры для каждого атрибута

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    /**
     * Метод для создания объекта Article из JSON-строки.
     *
     * @param jsonData JSON-объект с данными статьи.
     * @return Объект Article.
     * @throws JsonProcessingException Если произошла ошибка при обработке JSON.
     */
    public static Article fromJsonString(JSONObject jsonData) throws JsonProcessingException {
        String hash = jsonData.getString("hash");
        String url = jsonData.getString("url");
        String title = jsonData.getString("title");
        String text = jsonData.getString("text");
        String author = jsonData.getString("author");
        String time = jsonData.getString("time");

        return new Article(hash, url, title, text, author, time);
    }

    @Override
    public String toString() {
        return "Article{" +
                "hash='" + hash + '\'' +
                ", url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", author='" + author + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
