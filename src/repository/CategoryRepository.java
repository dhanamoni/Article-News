package repository;

import model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {
    private final List<Category> categories = new ArrayList<>();

    public void save(Category category) {
        if (findById(category.getCategoryId()).isEmpty()) {
            categories.add(category);
        }
    }

    public List<Category> findAll() {
        return new ArrayList<>(categories);
    }

    public Optional<Category> findById(String categoryId) {
        return categories.stream()
                .filter(category -> category.getCategoryId().equalsIgnoreCase(categoryId))
                .findFirst();
    }

    public Optional<Category> findByName(String categoryName) {
        return categories.stream()
                .filter(category -> category.getCategoryName().equalsIgnoreCase(categoryName))
                .findFirst();
    }

    public void replaceAll(List<Category> updatedCategories) {
        categories.clear();
        categories.addAll(updatedCategories);
    }
}
