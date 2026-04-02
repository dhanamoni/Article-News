package model;

import java.util.Date;

public class Article {
    private String articleId;
    private String articleName;
    private String author;
    private Date releaseDate;
    private String articleUrl;
    private String source;

    public Article(String articleId, String articleName, String author, Date releaseDate, String articleUrl, String source) {
        this.articleId = articleId;
        this.articleName = articleName;
        this.author = author;
        this.releaseDate = releaseDate;
        this.articleUrl = articleUrl;
        this.source = source;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

}
