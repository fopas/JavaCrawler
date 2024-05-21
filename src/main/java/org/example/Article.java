package org.example;

public class Article {
    private String title;
    private String author;
    private String time;
    private String text;
    private String url;

    public Article(String title, String author, String time, String text, String url) {
        this.title = title;
        this.author = author;
        this.time = time;
        this.text = text;
        this.url = url;
    }

    @Override
    public String toString() {
        return "Article{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", time='" + time + '\'' +
                ", text='" + text + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
