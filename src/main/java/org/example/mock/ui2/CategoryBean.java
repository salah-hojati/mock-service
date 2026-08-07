package org.example.mock.ui2;

import org.example.mock.entity.Category;
import org.example.mock.service.CategoryService;
import org.primefaces.PrimeFaces;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class CategoryBean implements Serializable {

    @Inject
    private CategoryService categoryService;

    private List<Category> categories = new ArrayList<Category>();
    private Category selectedCategory;
    private Category newCategory;

    @PostConstruct
    public void init() {
        loadCategories();
        newCategory = new Category();
    }

    public void loadCategories() {
        try {
            categories = categoryService.findAllForCurrentUser();
        } catch (Exception e) {
            categories = new ArrayList<Category>();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not load categories."));
        }
    }

    public void openNew() {
        newCategory = new Category();
        selectedCategory = null;
    }

    public void saveCategory() {
        System.out.println("Saving new category");
        try {
            if (newCategory.getId() == null) {
                if (categoryService.existsByName(newCategory.getName())) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_WARN, "Category exists",
                                    "A category with this name already exists."));
                    return;
                }
            }

            categoryService.save(newCategory);
            loadCategories();
            newCategory = new Category();
            selectedCategory = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Category Saved",
                            "Category was saved successfully."));

            PrimeFaces.current().ajax().update("form:category-dialog-content", "form:category-table");
            PrimeFaces.current().executeScript("PF('categoryDialog').hide()");

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save Error", e.getMessage()));
        }
    }

    public void deleteCategory(Category category) {
        try {
            categoryService.delete(category);
            loadCategories();
            selectedCategory = null;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Category Deleted",
                            "Category was deleted successfully."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Delete Error", e.getMessage()));
        }
    }

    public void selectCategory(Category category) {
        this.selectedCategory = category;
        this.newCategory = category;
    }

    public void selectAndEdit(Category category) {
        // Select the category
        this.selectedCategory = category;

        // Create a copy for editing
        this.newCategory = new Category();
        this.newCategory.setId(category.getId());
        this.newCategory.setName(category.getName());
        this.newCategory.setColor(category.getColor());
        this.newCategory.setDescription(category.getDescription());

        System.out.println("Category selected and prepared for edit: " + category.getName());
    }

    // Getters and Setters
    public List<Category> getCategories() { return categories; }
    public Category getSelectedCategory() { return selectedCategory; }
    public void setSelectedCategory(Category selectedCategory) { this.selectedCategory = selectedCategory; }
    public Category getNewCategory() { return newCategory; }
    public void setNewCategory(Category newCategory) { this.newCategory = newCategory; }
}