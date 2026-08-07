package org.example.mock.service;

import org.example.mock.entity.Category;
import org.example.mock.entity.AppUser;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class CategoryService {

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    @Inject
    private AuthService authService;

    public List<Category> findAllForCurrentUser() {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user");
        }
        return em.createNamedQuery("Category.findAll", Category.class)
                .getResultList();
    }

    public List<Category> findAll() {
        return em.createNamedQuery("Category.findAll", Category.class).getResultList();
    }

    public Category findById(Long id) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        Category category = em.find(Category.class, id);
        // Verify ownership
//        if (category != null && !category.getUser().getId().equals(currentUser.getId())) {
//            return null;
//        }
        return category;
    }

    public Category save(Category category) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user");
        }
       // category.setUser(currentUser);

        if (category.getId() == null) {
            em.persist(category);
            return category;
        } else {
            return em.merge(category);
        }


    }

    public void delete(Category category) {
        if (em.contains(category)) {
            em.remove(category);
        } else {
            em.remove(em.merge(category));
        }
    }

    public boolean existsByName(String name) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(c) FROM Category c WHERE c.name = :name ", Long.class)
                    .setParameter("name", name)
                   // .setParameter("userId", currentUser.getId())
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}