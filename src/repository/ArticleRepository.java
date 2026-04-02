package repository;

import model.Article;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ArticleRepository {
    private final List<Article> articles = new ArrayList<>();

    public boolean save(Article article) {
        if (existsById(article.getArticleId()) || existsByUrl(article.getArticleUrl())) {
            return false;
        }
        articles.add(article);
        return true;
    }

    public boolean update(Article updatedArticle) {
        Optional<Article> existingArticle = findById(updatedArticle.getArticleId());
        if (existingArticle.isEmpty()) {
            return false;
        }

        boolean duplicateUrl = articles.stream()
                .anyMatch(article -> !article.getArticleId().equalsIgnoreCase(updatedArticle.getArticleId())
                        && article.getArticleUrl().equalsIgnoreCase(updatedArticle.getArticleUrl()));
        if (duplicateUrl) {
            return false;
        }

        Article article = existingArticle.get();
        article.setArticleName(updatedArticle.getArticleName());
        article.setAuthor(updatedArticle.getAuthor());
        article.setReleaseDate(updatedArticle.getReleaseDate());
        article.setArticleUrl(updatedArticle.getArticleUrl());
        article.setSource(updatedArticle.getSource());
        return true;
    }

    public boolean existsById(String articleId) {
        return articles.stream().anyMatch(article -> article.getArticleId().equalsIgnoreCase(articleId));
    }

    public boolean existsByUrl(String articleUrl) {
        return articles.stream().anyMatch(article -> article.getArticleUrl().equalsIgnoreCase(articleUrl));
    }

    public List<Article> findAll() {
        List<Article> sortedArticles = new ArrayList<>(articles);
        sortedArticles.sort(Comparator.comparing(Article::getReleaseDate).reversed());
        return sortedArticles;
    }

    public Optional<Article> findById(String articleId) {
        return articles.stream().filter(article -> article.getArticleId().equalsIgnoreCase(articleId)).findFirst();
    }

    public void replaceAll(List<Article> updatedArticles) {
        articles.clear();
        articles.addAll(updatedArticles);
    }
}
