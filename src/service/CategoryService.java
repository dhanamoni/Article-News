package service;

import model.Category;
import repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void seedDefaultCategories() {
        saveCategory(new Category("C1", "Technology"));
        saveCategory(new Category("C2", "Business"));
        saveCategory(new Category("C3", "Sports"));
        saveCategory(new Category("C4", "Health"));
        saveCategory(new Category("C5", "Science"));
    }

    public void saveCategory(Category category) {
        categoryRepository.save(category);
    }

    public List<Category> getCategoryList() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findByName(String categoryName) {
        return categoryRepository.findByName(categoryName);
    }

}
