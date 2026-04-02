package repository;

import model.ArticleCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArticleCategoryRepository {
    private final List<ArticleCategory> articleCategories = new ArrayList<>();

    public void save(ArticleCategory articleCategory) {
        boolean exists = articleCategories.stream().anyMatch(existing ->
                existing.getArticleId().equalsIgnoreCase(articleCategory.getArticleId())
                        && existing.getCategoryId().equalsIgnoreCase(articleCategory.getCategoryId()));
        if (!exists) {
            articleCategories.add(articleCategory);
        }
    }

    public List<ArticleCategory> findAll() {
        return new ArrayList<>(articleCategories);
    }

    public List<ArticleCategory> findByArticleId(String articleId) {
        return articleCategories.stream()
                .filter(articleCategory -> articleCategory.getArticleId().equalsIgnoreCase(articleId))
                .collect(Collectors.toList());
    }

    public List<ArticleCategory> findByCategoryId(String categoryId) {
        return articleCategories.stream()
                .filter(articleCategory -> articleCategory.getCategoryId().equalsIgnoreCase(categoryId))
                .collect(Collectors.toList());
    }

    public void deleteByArticleId(String articleId) {
        articleCategories.removeIf(articleCategory -> articleCategory.getArticleId().equalsIgnoreCase(articleId));
    }

    public void replaceAll(List<ArticleCategory> updatedArticleCategories) {
        articleCategories.clear();
        articleCategories.addAll(updatedArticleCategories);
    }
}
