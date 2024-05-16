package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;

public class Parser {
    public void parse() {
        try {
            Document doc = Jsoup.connect("https://habr.com/ru/feed/").get();

            Elements posts = doc.select("article.tm-articles-list__item");

            for (Element post : posts) {
                String author = post.select("a.tm-user-info__username").text();
                String title = post.select("a.tm-title__link span").text().trim();
                String articleText = post.select("div.article-formatted-body").text().trim();
                String time = post.select("a.tm-article-datetime-published").text();
                String link = "https://habr.com" + post.select("a.tm-title__link").attr("href");

                // Вывод информации о публикации
                System.out.println("Author: " + author);
                System.out.println("Title: " + title);
                System.out.println("Article Text: " + articleText);
                System.out.println("Time: " + time);
                System.out.println("Link: " + link);
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
