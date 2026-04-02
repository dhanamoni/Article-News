import model.Category;
import repository.ArticleCategoryRepository;
import repository.ArticleRepository;
import repository.CategoryRepository;
import repository.UserRepository;
import service.ArticleService;
import service.CategoryService;
import service.UserService;
import storage.FileDataStore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Main {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    public static void main(String[] args) {
        FileDataStore fileDataStore = new FileDataStore();
        CategoryRepository categoryRepository = new CategoryRepository();
        ArticleRepository articleRepository = new ArticleRepository();
        ArticleCategoryRepository articleCategoryRepository = new ArticleCategoryRepository();
        UserRepository userRepository = new UserRepository();
        fileDataStore.loadAll(articleRepository, categoryRepository, articleCategoryRepository, userRepository);

        CategoryService categoryService = new CategoryService(categoryRepository);
        if (categoryService.getCategoryList().isEmpty()) {
            categoryService.seedDefaultCategories();
        }

        UserService userService = new UserService(userRepository);
        if (userService.getUsers().isEmpty()) {
            userService.registerDefaultUser();
        }

        ArticleService articleService = new ArticleService(articleRepository, categoryRepository, articleCategoryRepository);
        int savedCount = 0;
        if (articleService.getStoredArticleCount() == 0) {
            savedCount = articleService.fetchAndStoreArticles(loadMockApiData());
        }
        fileDataStore.saveAll(articleRepository, categoryRepository, articleCategoryRepository, userRepository);

        if (args.length == 0) {
            printHelp(savedCount, articleService.getStoredArticleCount(), categoryService.getCategoryList());
            return;
        }

        String command = args[0].toLowerCase(Locale.ENGLISH);
        switch (command) {
            case "latest" -> printArticles(articleService.getLatestArticles());
            case "fetch" -> {
                int newArticles = articleService.fetchLatestBatch(loadIncrementalMockApiData());
                fileDataStore.saveAll(articleRepository, categoryRepository, articleCategoryRepository, userRepository);
                System.out.println("Fetched and stored new articles: " + newArticles);
                System.out.println("Articles in database: " + articleService.getStoredArticleCount());
            }
            case "search" -> {
                if (args.length < 2) {
                    System.out.println("Usage: java Main search <keyword>");
                    return;
                }
                printArticles(articleService.searchByKeyword(joinArgs(args, 1)));
            }
            case "category" -> {
                if (args.length < 2) {
                    System.out.println("Usage: java Main category <categoryName>");
                    return;
                }
                printArticles(articleService.filterByCategory(joinArgs(args, 1)));
            }
            case "date" -> {
                if (args.length < 2) {
                    System.out.println("Usage: java Main date <yyyy-MM-dd>");
                    return;
                }
                printArticles(articleService.filterByDate(args[1]));
            }
            case "filter" -> {
                if (args.length < 2) {
                    System.out.println("Usage: java Main filter [keyword=<text>] [category=<name>] [date=<yyyy-MM-dd>]");
                    return;
                }
                String keyword = null;
                String category = null;
                String date = null;

                for (int index = 1; index < args.length; index++) {
                    String argument = args[index];
                    if (argument.startsWith("keyword=")) {
                        keyword = argument.substring("keyword=".length());
                    } else if (argument.startsWith("category=")) {
                        category = argument.substring("category=".length());
                    } else if (argument.startsWith("date=")) {
                        date = argument.substring("date=".length());
                    }
                }

                printArticles(articleService.filterArticles(keyword, category, date));
            }
            case "update" -> {
                if (args.length < 8) {
                    System.out.println("Usage: java Main update <articleId> <title> <author> <yyyy-MM-dd> <url> <source> <categoryName>");
                    System.out.println("Use quotes for values containing spaces.");
                    return;
                }

                boolean updated = articleService.updateArticle(
                        args[1],
                        args[2],
                        args[3],
                        args[4],
                        args[5],
                        args[6],
                        args[7]);
                if (updated) {
                    fileDataStore.saveAll(articleRepository, categoryRepository, articleCategoryRepository, userRepository);
                }
                System.out.println(updated
                        ? "Article updated successfully."
                        : "Article update failed. Check article ID, category, date, or duplicate URL.");
            }
            case "categories" -> categoryService.getCategoryList()
                    .forEach(category -> System.out.println(category.getCategoryId() + " - " + category.getCategoryName()));
            default -> printHelp(savedCount, articleService.getStoredArticleCount(), categoryService.getCategoryList());
        }
    }

    private static void printHelp(int savedCount, int storedCount, List<Category> categories) {
        System.out.println("News CLI Project");
        System.out.println("Fetched from external API pages and stored unique records.");
        System.out.println("New articles saved: " + savedCount);
        System.out.println("Articles in database: " + storedCount);
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  java Main latest");
        System.out.println("  java Main fetch");
        System.out.println("  java Main search <keyword>");
        System.out.println("  java Main category <categoryName>");
        System.out.println("  java Main date <yyyy-MM-dd>");
        System.out.println("  java Main filter [keyword=<text>] [category=<name>] [date=<yyyy-MM-dd>]");
        System.out.println("  java Main update <articleId> <title> <author> <yyyy-MM-dd> <url> <source> <categoryName>");
        System.out.println("  java Main categories");
        System.out.println();
        System.out.println("Available categories:");
        categories.forEach(category -> System.out.println("  " + category.getCategoryName()));
    }

    private static void printArticles(List<ArticleService.ArticleView> articles) {
        if (articles.isEmpty()) {
            System.out.println("No articles found for the given command.");
            return;
        }

        for (ArticleService.ArticleView article : articles) {
            System.out.println("ID: " + article.articleId());
            System.out.println("Title: " + article.articleName());
            System.out.println("Author: " + article.author());
            System.out.println("Date: " + article.releaseDate());
            System.out.println("Source: " + article.source());
            System.out.println("URL: " + article.articleUrl());
            System.out.println("Category: " + String.join(", ", article.categories()));
            System.out.println();
        }
    }

    private static String joinArgs(String[] args, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(args, startIndex, args.length));
    }

    private static List<ArticleService.ApiArticleRecord> loadMockApiData() {
        List<ArticleService.ApiArticleRecord> articles = new ArrayList<>();
        try {
            articles.add(new ArticleService.ApiArticleRecord("A1", "AI Reshapes Software Development", "Ravi Kumar",
                    DATE_FORMAT.parse("2026-04-01"), "https://news.example.com/ai-development", "Tech Daily", "Technology"));
            articles.add(new ArticleService.ApiArticleRecord("A2", "Stock Markets End Higher on Global Optimism", "Neha Shah",
                    DATE_FORMAT.parse("2026-04-02"), "https://news.example.com/markets-higher", "Business Times", "Business"));
            articles.add(new ArticleService.ApiArticleRecord("A3", "Local Team Wins National Championship", "Arjun Mehta",
                    DATE_FORMAT.parse("2026-03-31"), "https://news.example.com/championship", "Sports Network", "Sports"));
            articles.add(new ArticleService.ApiArticleRecord("A4", "New Vaccine Research Shows Promising Results", "Priya Nair",
                    DATE_FORMAT.parse("2026-04-02"), "https://news.example.com/vaccine-research", "Health World", "Health"));
            articles.add(new ArticleService.ApiArticleRecord("A5", "Space Telescope Captures Rare Deep-Sky Event", "Isha Roy",
                    DATE_FORMAT.parse("2026-03-30"), "https://news.example.com/space-event", "Science Post", "Science"));
            articles.add(new ArticleService.ApiArticleRecord("A2", "Stock Markets End Higher on Global Optimism", "Neha Shah",
                    DATE_FORMAT.parse("2026-04-02"), "https://news.example.com/markets-higher", "Business Times", "Business"));
        } catch (ParseException exception) {
            throw new IllegalStateException("Mock API data contains an invalid date.", exception);
        }
        return articles;
    }

    private static List<ArticleService.ApiArticleRecord> loadIncrementalMockApiData() {
        List<ArticleService.ApiArticleRecord> articles = new ArrayList<>();
        try {
            articles.add(new ArticleService.ApiArticleRecord("A6", "Electric Cars Reach Record Global Sales", "Maya Sen",
                    DATE_FORMAT.parse("2026-04-03"), "https://news.example.com/ev-sales", "Auto Brief", "Business"));
            articles.add(new ArticleService.ApiArticleRecord("A7", "Researchers Build Faster Quantum Networking Link", "Karan Patel",
                    DATE_FORMAT.parse("2026-04-03"), "https://news.example.com/quantum-link", "Lab Wire", "Science"));
            articles.add(new ArticleService.ApiArticleRecord("A1", "AI Reshapes Software Development", "Ravi Kumar",
                    DATE_FORMAT.parse("2026-04-01"), "https://news.example.com/ai-development", "Tech Daily", "Technology"));
        } catch (ParseException exception) {
            throw new IllegalStateException("Incremental mock API data contains an invalid date.", exception);
        }
        return articles;
    }
}
