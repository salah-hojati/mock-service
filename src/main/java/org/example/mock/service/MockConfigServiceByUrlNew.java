package org.example.mock.service;

import org.example.mock.entity.AppUser;
import org.example.mock.entity.MockConfig2;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MockConfigServiceByUrlNew {

    private static final Logger LOGGER = Logger.getLogger(MockConfigServiceByUrlNew.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    @Inject
    private AuthService authService;

    public List<MockConfig2> findAll() {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "SELECT m FROM MockConfig2 m WHERE m.user.id = :userId ORDER BY m.id",
                        MockConfig2.class)
                .setParameter("userId", currentUser.getId())
                .getResultList();
    }



    public List<MockConfig2> findByCategory(Long categoryId) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "SELECT m FROM MockConfig2 m WHERE m.user.id = :userId AND m.category.id = :categoryId ORDER BY m.id",
                        MockConfig2.class)
                .setParameter("userId", currentUser.getId())
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    public void save(MockConfig2 mockConfig) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user");
        }
        mockConfig.setUser(currentUser);

        if (mockConfig.getId() == null) {
            em.persist(mockConfig);
        } else {
            em.merge(mockConfig);
        }
    }

    public MockConfig2 findById(Long id) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        MockConfig2 config = em.find(MockConfig2.class, id);
        // Verify the user owns this config
        if (config != null && !config.getUser().getId().equals(currentUser.getId())) {
            return null;
        }
        return config;
    }

    public void delete(MockConfig2 mockConfig) {
        if (em.contains(mockConfig)) {
            em.remove(mockConfig);
        } else {
            em.remove(em.merge(mockConfig));
        }
    }

    public MockConfig2 findMockConfig(String httpMethod, String urlPattern) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        String jpql = "SELECT m FROM MockConfig2 m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern AND m.user.id = :userId";
        TypedQuery<MockConfig2> query = em.createQuery(jpql, MockConfig2.class);
        query.setParameter("httpMethod", httpMethod);
        query.setParameter("urlPattern", urlPattern);
        query.setParameter("userId", currentUser.getId());

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "No mock configuration found for method: {0}, URL: {1}",
                    new Object[]{httpMethod, urlPattern});
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error finding mock config for method: " + httpMethod + ", URL: " + urlPattern, ex);
            return null;
        }
    }

}