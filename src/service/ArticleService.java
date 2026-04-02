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
import java.util.stream.Collectors;

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
                Optional<Category> category = categoryRepository.findByName(record.categoryName());
                category.ifPresent(value -> articleCategoryRepository.save(
                        new ArticleCategory(value.getCategoryId(), record.articleId())));
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
        List<Article> filtered = articleRepository.findAll().stream()
                .filter(article ->
                        matchesKeyword(article.getArticleName(), normalizedKeyword)
                                || matchesKeyword(article.getAuthor(), normalizedKeyword)
                                || matchesKeyword(article.getSource(), normalizedKeyword)
                                || matchesKeyword(article.getArticleId(), normalizedKeyword)
                                || getCategoryNames(article.getArticleId()).stream()
                                .anyMatch(category -> matchesKeyword(category, normalizedKeyword)))
                .collect(Collectors.toList());
        return buildViews(filtered);
    }

    public List<ArticleView> filterByCategory(String categoryName) {
        Optional<Category> category = categoryRepository.findByName(categoryName);
        if (category.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> articleIds = articleCategoryRepository.findByCategoryId(category.get().getCategoryId()).stream()
                .map(ArticleCategory::getArticleId)
                .collect(Collectors.toList());

        List<Article> filtered = articleRepository.findAll().stream()
                .filter(article -> articleIds.contains(article.getArticleId()))
                .collect(Collectors.toList());
        return buildViews(filtered);
    }

    public List<ArticleView> filterByDate(String dateInput) {
        try {
            Date targetDate = dateFormat.parse(dateInput);
            List<Article> filtered = articleRepository.findAll().stream()
                    .filter(article -> isSameDay(article.getReleaseDate(), targetDate))
                    .collect(Collectors.toList());
            return buildViews(filtered);
        } catch (ParseException exception) {
            return new ArrayList<>();
        }
    }

    public List<ArticleView> filterByCategoryAndDate(String categoryName, String dateInput) {
        List<ArticleView> byCategory = filterByCategory(categoryName);
        return byCategory.stream()
                .filter(articleView -> articleView.releaseDate().equals(dateInput))
                .collect(Collectors.toList());
    }

    public List<ArticleView> filterArticles(String keyword, String categoryName, String dateInput) {
        List<ArticleView> filteredArticles = buildViews(articleRepository.findAll());

        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ENGLISH);
            filteredArticles = filteredArticles.stream()
                    .filter(article ->
                            matchesKeyword(article.articleName(), normalizedKeyword)
                                    || matchesKeyword(article.author(), normalizedKeyword)
                                    || matchesKeyword(article.source(), normalizedKeyword)
                                    || matchesKeyword(article.articleId(), normalizedKeyword)
                                    || article.categories().stream().anyMatch(category -> matchesKeyword(category, normalizedKeyword)))
                    .collect(Collectors.toList());
        }

        if (categoryName != null && !categoryName.isBlank()) {
            filteredArticles = filteredArticles.stream()
                    .filter(article -> article.categories().stream()
                            .anyMatch(category -> category.equalsIgnoreCase(categoryName)))
                    .collect(Collectors.toList());
        }

        if (dateInput != null && !dateInput.isBlank()) {
            try {
                Date targetDate = dateFormat.parse(dateInput);
                String normalizedDate = dateFormat.format(targetDate);
                filteredArticles = filteredArticles.stream()
                        .filter(article -> article.releaseDate().equals(normalizedDate))
                        .collect(Collectors.toList());
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
        return articles.stream()
                .sorted(Comparator.comparing(Article::getReleaseDate).reversed())
                .map(article -> new ArticleView(
                        article.getArticleId(),
                        article.getArticleName(),
                        article.getAuthor(),
                        dateFormat.format(article.getReleaseDate()),
                        article.getArticleUrl(),
                        article.getSource(),
                        getCategoryNames(article.getArticleId())))
                .collect(Collectors.toList());
    }

    private List<String> getCategoryNames(String articleId) {
        return articleCategoryRepository.findByArticleId(articleId).stream()
                .map(ArticleCategory::getCategoryId)
                .map(categoryRepository::findById)
                .filter(Optional::isPresent)
                .map(optionalCategory -> optionalCategory.get().getCategoryName())
                .collect(Collectors.toList());
    }

    private boolean isSameDay(Date firstDate, Date secondDate) {
        return dateFormat.format(firstDate).equals(dateFormat.format(secondDate));
    }

    private boolean matchesKeyword(String value, String keyword) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalizedValue = value.toLowerCase(Locale.ENGLISH);
        if (normalizedValue.contains(keyword)) {
            return true;
        }

        String[] valueTokens = normalizedValue.split("[^a-z0-9]+");
        String[] keywordTokens = keyword.toLowerCase(Locale.ENGLISH).split("[^a-z0-9]+");
        for (String keywordToken : keywordTokens) {
            if (keywordToken.isBlank()) {
                continue;
            }

            for (String valueToken : valueTokens) {
                if (valueToken.equals(keywordToken) || valueToken.startsWith(keywordToken)) {
                    return true;
                }
            }
        }

        for (String valueToken : valueTokens) {
            if (valueToken.equals(keyword) || valueToken.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void replaceCategoryLink(String articleId, String categoryId) {
        articleCategoryRepository.deleteByArticleId(articleId);
        articleCategoryRepository.save(new ArticleCategory(categoryId, articleId));
    }

    public record ApiArticleRecord(
            String articleId,
            String articleName,
            String author,
            Date releaseDate,
            String articleUrl,
            String source,
            String categoryName) {
        public Article toArticle() {
            return new Article(articleId, articleName, author, releaseDate, articleUrl, source);
        }
    }

    public record ArticleView(
            String articleId,
            String articleName,
            String author,
            String releaseDate,
            String articleUrl,
            String source,
            List<String> categories) {
    }
}
