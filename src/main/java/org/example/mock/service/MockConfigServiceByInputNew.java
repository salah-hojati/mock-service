package org.example.mock.service;

import org.example.mock.entity.MockConfig;
import org.example.mock.entity.AppUser;
import org.example.mock.util.JsonUtil;

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
public class MockConfigServiceByInputNew {

    private static final Logger LOGGER = Logger.getLogger(MockConfigServiceByInputNew.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    @Inject
    private AuthService authService;

    public List<MockConfig> findAll() {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "SELECT m FROM MockConfig m WHERE m.user.id = :userId ORDER BY m.id", MockConfig.class)
                .setParameter("userId", currentUser.getId())
                .getResultList();
    }

    public List<MockConfig> findByCategory(Long categoryId) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        return em.createQuery(
                        "SELECT m FROM MockConfig m WHERE m.user.id = :userId AND m.category.id = :categoryId ORDER BY m.id", MockConfig.class)
                .setParameter("userId", currentUser.getId())
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    public MockConfig findById(Long id) {
        return em.find(MockConfig.class, id);
    }

    public void save(MockConfig mockConfig) {
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

    public void delete(MockConfig mockConfig) {
        if (em.contains(mockConfig)) {
            em.remove(mockConfig);
        } else {
            em.remove(em.merge(mockConfig));
        }
    }

    public MockConfig findMockConfig(String httpMethod, String urlPattern, String requestPayload) {
        AppUser currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        String payloadToSearch = JsonUtil.normalize(requestPayload);

        TypedQuery<MockConfig> query;
        String jpql;

        if (payloadToSearch == null) {
            jpql = "SELECT m FROM MockConfig m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern " +
                    "AND m.requestPayload IS NULL AND m.user.id = :userId";
            query = em.createQuery(jpql, MockConfig.class);
        } else {
            jpql = "SELECT m FROM MockConfig m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern " +
                    "AND CAST(m.requestPayload AS VARCHAR2(4000)) = :payload AND m.user.id = :userId";
            query = em.createQuery(jpql, MockConfig.class);
            query.setParameter("payload", payloadToSearch);
        }

        query.setParameter("httpMethod", httpMethod);
        query.setParameter("urlPattern", urlPattern);
        query.setParameter("userId", currentUser.getId());

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "No mock configuration found for method: {0}, URL: {1}", new Object[]{httpMethod, urlPattern});
            return null;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error finding mock config for method: " + httpMethod + ", URL: " + urlPattern, ex);
            return null;
        }
    }
}