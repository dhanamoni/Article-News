package service;

import model.ArticleCategory;
import repository.ArticleCategoryRepository;

import java.util.List;

public class ArticleCategoryService {
    private final ArticleCategoryRepository articleCategoryRepository;

    public ArticleCategoryService(ArticleCategoryRepository articleCategoryRepository) {
        this.articleCategoryRepository = articleCategoryRepository;
    }

    public void linkArticleToCategory(String articleId, String categoryId) {
        articleCategoryRepository.save(new ArticleCategory(categoryId, articleId));
    }

    public List<ArticleCategory> getArticleCategoryList() {
        return articleCategoryRepository.findAll();
    }

}
