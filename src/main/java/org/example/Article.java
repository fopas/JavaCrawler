package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.JSONObject;

public class Article {
    private String hash;
    private String url;
    private String title;
    private String text;
    private String author;
    private String time;


    public Article(String hash, String url, String title, String text, String author, String time) {
        this.hash = hash;
        this.url = url;
        this.title = title;
        this.text = text;
        this.author = author;
        this.time = time;
    }

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

    public static Article fromJsonString(JSONObject jsonData) throws JsonProcessingException, JsonProcessingException {
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
