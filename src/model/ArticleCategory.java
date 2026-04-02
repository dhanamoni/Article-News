package model;

public class ArticleCategory {
    private String categoryId;
    private String articleId;

    public ArticleCategory(String categoryId, String articleId) {
        this.categoryId = categoryId;
        this.articleId = articleId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }
}
