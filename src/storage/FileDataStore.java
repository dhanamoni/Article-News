package storage;

import model.Article;
import model.ArticleCategory;
import model.Category;
import model.User;
import repository.ArticleCategoryRepository;
import repository.ArticleRepository;
import repository.CategoryRepository;
import repository.UserRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileDataStore {
    private static final String ESCAPED_SEPARATOR = "\\|";
    private static final String SEPARATOR = "|";
    private static final String ESCAPED_NEWLINE = "\\n";
    private static final String DATA_DIRECTORY = "data";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public void loadAll(ArticleRepository articleRepository,
                        CategoryRepository categoryRepository,
                        ArticleCategoryRepository articleCategoryRepository,
                        UserRepository userRepository) {
        categoryRepository.replaceAll(loadCategories());
        userRepository.replaceAll(loadUsers());
        articleRepository.replaceAll(loadArticles());
        articleCategoryRepository.replaceAll(loadArticleCategories());
    }

    public void saveAll(ArticleRepository articleRepository,
                        CategoryRepository categoryRepository,
                        ArticleCategoryRepository articleCategoryRepository,
                        UserRepository userRepository) {
        ensureDataDirectory();
        saveCategories(categoryRepository.findAll());
        saveUsers(userRepository.findAll());
        saveArticles(articleRepository.findAll());
        saveArticleCategories(articleCategoryRepository.findAll());
    }

    private List<Category> loadCategories() {
        List<Category> categories = new ArrayList<>();
        for (String line : readLines("categories.txt")) {
            String[] parts = line.split(ESCAPED_SEPARATOR, -1);
            if (parts.length == 2) {
                categories.add(new Category(unescape(parts[0]), unescape(parts[1])));
            }
        }
        return categories;
    }

    private List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        for (String line : readLines("users.txt")) {
            String[] parts = line.split(ESCAPED_SEPARATOR, -1);
            if (parts.length == 4) {
                users.add(new User(unescape(parts[0]), unescape(parts[1]), unescape(parts[2]), unescape(parts[3])));
            }
        }
        return users;
    }

    private List<Article> loadArticles() {
        List<Article> articles = new ArrayList<>();
        for (String line : readLines("articles.txt")) {
            String[] parts = line.split(ESCAPED_SEPARATOR, -1);
            if (parts.length == 6) {
                try {
                    articles.add(new Article(
                            unescape(parts[0]),
                            unescape(parts[1]),
                            unescape(parts[2]),
                            DATE_FORMAT.parse(unescape(parts[3])),
                            unescape(parts[4]),
                            unescape(parts[5])));
                } catch (ParseException ignored) {
                }
            }
        }
        return articles;
    }

    private List<ArticleCategory> loadArticleCategories() {
        List<ArticleCategory> articleCategories = new ArrayList<>();
        for (String line : readLines("article_categories.txt")) {
            String[] parts = line.split(ESCAPED_SEPARATOR, -1);
            if (parts.length == 2) {
                articleCategories.add(new ArticleCategory(unescape(parts[0]), unescape(parts[1])));
            }
        }
        return articleCategories;
    }

    private void saveCategories(List<Category> categories) {
        List<String> lines = categories.stream()
                .map(category -> escape(category.getCategoryId()) + SEPARATOR + escape(category.getCategoryName()))
                .toList();
        writeLines("categories.txt", lines);
    }

    private void saveUsers(List<User> users) {
        List<String> lines = users.stream()
                .map(user -> escape(user.getUsername()) + SEPARATOR
                        + escape(user.getUserid()) + SEPARATOR
                        + escape(user.getEmail()) + SEPARATOR
                        + escape(user.getPassword()))
                .toList();
        writeLines("users.txt", lines);
    }

    private void saveArticles(List<Article> articles) {
        List<String> lines = articles.stream()
                .map(article -> escape(article.getArticleId()) + SEPARATOR
                        + escape(article.getArticleName()) + SEPARATOR
                        + escape(article.getAuthor()) + SEPARATOR
                        + escape(DATE_FORMAT.format(article.getReleaseDate())) + SEPARATOR
                        + escape(article.getArticleUrl()) + SEPARATOR
                        + escape(article.getSource()))
                .toList();
        writeLines("articles.txt", lines);
    }

    private void saveArticleCategories(List<ArticleCategory> articleCategories) {
        List<String> lines = articleCategories.stream()
                .map(articleCategory -> escape(articleCategory.getCategoryId()) + SEPARATOR + escape(articleCategory.getArticleId()))
                .toList();
        writeLines("article_categories.txt", lines);
    }

    private List<String> readLines(String fileName) {
        Path path = resolve(fileName);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }

    private void writeLines(String fileName, List<String> lines) {
        try {
            Files.write(resolve(fileName), lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write " + fileName, exception);
        }
    }

    private void ensureDataDirectory() {
        try {
            Files.createDirectories(Path.of(DATA_DIRECTORY));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create data directory.", exception);
        }
    }

    private Path resolve(String fileName) {
        return Path.of(DATA_DIRECTORY, fileName);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace(SEPARATOR, "\\p")
                .replace("\n", ESCAPED_NEWLINE)
                .replace("\r", "");
    }

    private String unescape(String value) {
        return value.replace(ESCAPED_NEWLINE, "\n")
                .replace("\\p", SEPARATOR)
                .replace("\\\\", "\\");
    }
}
