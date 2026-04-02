package service;

import model.Article;
import model.ArticleCategory;
import model.Category;
import repository.ArticleCategoryRepository;
import repository.ArticleRepository;
import repository.CategoryRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ArticleService {
    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleCategoryRepository articleCategoryRepository;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public ArticleService(ArticleRepository articleRepository,
                          CategoryRepository categoryRepository,
                          ArticleCategoryRepository articleCategoryRepository) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.articleCategoryRepository = articleCategoryRepository;
    }

    public int fetchAndStoreArticles(List<ApiArticleRecord> apiArticles) {
        int savedCount = 0;
        for (ApiArticleRecord record : apiArticles) {
            if (articleRepository.save(record.toArticle())) {
                savedCount++;
                Optional<Category> category = categoryRepository.findByName(record.getCategoryName());
                category.ifPresent(value -> articleCategoryRepository.save(
                        new ArticleCategory(value.getCategoryId(), record.getArticleId())));
            }
        }
        return savedCount;
    }

    public List<ArticleView> getLatestArticles() {
        return buildViews(articleRepository.findAll());
    }

    public int fetchLatestBatch(List<ApiArticleRecord> apiArticles) {
        return fetchAndStoreArticles(apiArticles);
    }

    public List<ArticleView> searchByKeyword(String keyword) {
        String normalizedKeyword = keyword.toLowerCase(Locale.ENGLISH);
        List<Article> filtered = new ArrayList<>();
        List<Article> allArticles = articleRepository.findAll();

        for (Article article : allArticles) {
            boolean matches = matchesKeyword(article.getArticleName(), normalizedKeyword)
                    || matchesKeyword(article.getAuthor(), normalizedKeyword)
                    || matchesKeyword(article.getSource(), normalizedKeyword)
                    || matchesKeyword(article.getArticleId(), normalizedKeyword);

            if (!matches) {
                List<String> categoryNames = getCategoryNames(article.getArticleId());
                for (String categoryName : categoryNames) {
                    if (matchesKeyword(categoryName, normalizedKeyword)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                filtered.add(article);
            }
        }

        return buildViews(filtered);
    }

    public List<ArticleView> filterByCategory(String categoryName) {
        Optional<Category> category = categoryRepository.findByName(categoryName);
        if (category.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> articleIds = new ArrayList<>();
        List<ArticleCategory> mappings = articleCategoryRepository.findByCategoryId(category.get().getCategoryId());
        for (ArticleCategory item : mappings) {
            articleIds.add(item.getArticleId());
        }

        List<Article> filtered = new ArrayList<>();
        List<Article> allArticles = articleRepository.findAll();
        for (Article article : allArticles) {
            if (articleIds.contains(article.getArticleId())) {
                filtered.add(article);
            }
        }
        return buildViews(filtered);
    }

    public List<ArticleView> filterByDate(String dateInput) {
        try {
            Date targetDate = dateFormat.parse(dateInput);
            List<Article> filtered = new ArrayList<>();
            List<Article> allArticles = articleRepository.findAll();
            for (Article article : allArticles) {
                if (isSameDay(article.getReleaseDate(), targetDate)) {
                    filtered.add(article);
                }
            }
            return buildViews(filtered);
        } catch (ParseException exception) {
            return new ArrayList<>();
        }
    }

    public List<ArticleView> filterByCategoryAndDate(String categoryName, String dateInput) {
        List<ArticleView> byCategory = filterByCategory(categoryName);
        List<ArticleView> result = new ArrayList<>();
        for (ArticleView articleView : byCategory) {
            if (articleView.getReleaseDate().equals(dateInput)) {
                result.add(articleView);
            }
        }
        return result;
    }

    public List<ArticleView> filterArticles(String keyword, String categoryName, String dateInput) {
        List<ArticleView> filteredArticles = buildViews(articleRepository.findAll());

        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ENGLISH);
            List<ArticleView> keywordFiltered = new ArrayList<>();
            for (ArticleView article : filteredArticles) {
                boolean matches = matchesKeyword(article.getArticleId(), normalizedKeyword)
                        || matchesKeyword(article.getArticleName(), normalizedKeyword)
                        || matchesKeyword(article.getAuthor(), normalizedKeyword)
                        || matchesKeyword(article.getSource(), normalizedKeyword);

                if (!matches) {
                    for (String category : article.getCategories()) {
                        if (matchesKeyword(category, normalizedKeyword)) {
                            matches = true;
                            break;
                        }
                    }
                }

                if (matches) {
                    keywordFiltered.add(article);
                }
            }
            filteredArticles = keywordFiltered;
        }

        if (categoryName != null && !categoryName.isBlank()) {
            List<ArticleView> categoryFiltered = new ArrayList<>();
            for (ArticleView article : filteredArticles) {
                for (String category : article.getCategories()) {
                    if (category.equalsIgnoreCase(categoryName)) {
                        categoryFiltered.add(article);
                        break;
                    }
                }
            }
            filteredArticles = categoryFiltered;
        }

        if (dateInput != null && !dateInput.isBlank()) {
            try {
                Date targetDate = dateFormat.parse(dateInput);
                String normalizedDate = dateFormat.format(targetDate);
                List<ArticleView> dateFiltered = new ArrayList<>();
                for (ArticleView article : filteredArticles) {
                    if (article.getReleaseDate().equals(normalizedDate)) {
                        dateFiltered.add(article);
                    }
                }
                filteredArticles = dateFiltered;
            } catch (ParseException exception) {
                return new ArrayList<>();
            }
        }

        return filteredArticles;
    }

    public boolean updateArticle(String articleId,
                                 String articleName,
                                 String author,
                                 String releaseDateInput,
                                 String articleUrl,
                                 String source,
                                 String categoryName) {
        Optional<Article> existingArticle = articleRepository.findById(articleId);
        if (existingArticle.isEmpty()) {
            return false;
        }

        Optional<Category> category = categoryRepository.findByName(categoryName);
        if (category.isEmpty()) {
            return false;
        }

        try {
            Date releaseDate = dateFormat.parse(releaseDateInput);
            Article updatedArticle = new Article(articleId, articleName, author, releaseDate, articleUrl, source);
            boolean updated = articleRepository.update(updatedArticle);
            if (!updated) {
                return false;
            }

            replaceCategoryLink(articleId, category.get().getCategoryId());
            return true;
        } catch (ParseException exception) {
            return false;
        }
    }

    public int getStoredArticleCount() {
        return articleRepository.findAll().size();
    }

    private List<ArticleView> buildViews(List<Article> articles) {
        List<Article> sortedArticles = new ArrayList<>(articles);
        sortedArticles.sort(Comparator.comparing(Article::getReleaseDate).reversed());

        List<ArticleView> views = new ArrayList<>();
        for (Article article : sortedArticles) {
            ArticleView view = new ArticleView(
                    article.getArticleId(),
                    article.getArticleName(),
                    article.getAuthor(),
                    dateFormat.format(article.getReleaseDate()),
                    article.getArticleUrl(),
                    article.getSource(),
                    getCategoryNames(article.getArticleId()));
            views.add(view);
        }
        return views;
    }

    private List<String> getCategoryNames(String articleId) {
        List<String> categoryNames = new ArrayList<>();
        List<ArticleCategory> mappings = articleCategoryRepository.findByArticleId(articleId);

        for (ArticleCategory mapping : mappings) {
            Optional<Category> category = categoryRepository.findById(mapping.getCategoryId());
            if (category.isPresent()) {
                categoryNames.add(category.get().getCategoryName());
            }
        }

        return categoryNames;
    }

    private boolean isSameDay(Date firstDate, Date secondDate) {
        return dateFormat.format(firstDate).equals(dateFormat.format(secondDate));
    }

    private boolean matchesKeyword(String value, String keyword) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.toLowerCase(Locale.ENGLISH).contains(keyword);
    }

    private void replaceCategoryLink(String articleId, String categoryId) {
        articleCategoryRepository.deleteByArticleId(articleId);
        articleCategoryRepository.save(new ArticleCategory(categoryId, articleId));
    }

    public static class ApiArticleRecord {
        private final String articleId;
        private final String articleName;
        private final String author;
        private final Date releaseDate;
        private final String articleUrl;
        private final String source;
        private final String categoryName;

        public ApiArticleRecord(String articleId, String articleName, String author, Date releaseDate,
                                String articleUrl, String source, String categoryName) {
            this.articleId = articleId;
            this.articleName = articleName;
            this.author = author;
            this.releaseDate = releaseDate;
            this.articleUrl = articleUrl;
            this.source = source;
            this.categoryName = categoryName;
        }

        public String getArticleId() {
            return articleId;
        }

        public String getArticleName() {
            return articleName;
        }

        public String getAuthor() {
            return author;
        }

        public Date getReleaseDate() {
            return releaseDate;
        }

        public String getArticleUrl() {
            return articleUrl;
        }

        public String getSource() {
            return source;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public Article toArticle() {
            return new Article(articleId, articleName, author, releaseDate, articleUrl, source);
        }
    }

    public static class ArticleView {
        private final String articleId;
        private final String articleName;
        private final String author;
        private final String releaseDate;
        private final String articleUrl;
        private final String source;
        private final List<String> categories;

        public ArticleView(String articleId, String articleName, String author, String releaseDate,
                           String articleUrl, String source, List<String> categories) {
            this.articleId = articleId;
            this.articleName = articleName;
            this.author = author;
            this.releaseDate = releaseDate;
            this.articleUrl = articleUrl;
            this.source = source;
            this.categories = categories;
        }

        public String getArticleId() {
            return articleId;
        }

        public String getArticleName() {
            return articleName;
        }

        public String getAuthor() {
            return author;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public String getArticleUrl() {
            return articleUrl;
        }

        public String getSource() {
            return source;
        }

        public List<String> getCategories() {
            return categories;
        }
    }
}
