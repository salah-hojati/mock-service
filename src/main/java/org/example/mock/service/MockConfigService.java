package org.example.mock.service;

import org.example.mock.entity.MockConfig;
// Import the new utility class
import org.example.mock.util.JsonUtil;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MockConfigService {

    private static final Logger LOGGER = Logger.getLogger(MockConfigService.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    // ... (findAll, save, delete methods remain the same) ...
    public List<MockConfig> findAll() {
        return em.createNamedQuery("MockConfig.findAll", MockConfig.class).getResultList();
    }
    public MockConfig findById(Long id) {
        return em.find(MockConfig.class, id);
    }
    public void save(MockConfig mockConfig) {
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


    /**
     * Finds a mock configuration based on the URL pattern and request body.
     * @return The matching MockConfig object, or null if not found.
     */
    public MockConfig findMockConfig(String httpMethod, String urlPattern, String requestPayload) {
        String payloadToSearch = JsonUtil.normalize(requestPayload);

        TypedQuery<MockConfig> query;
        String jpql;

        if (payloadToSearch == null) {
            jpql = "SELECT m FROM MockConfig m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern AND m.requestPayload IS NULL";
            query = em.createQuery(jpql, MockConfig.class);
        } else {
            jpql = "SELECT m FROM MockConfig m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern AND CAST(m.requestPayload AS VARCHAR2(4000)) = :payload";
            query = em.createQuery(jpql, MockConfig.class);
            query.setParameter("payload", payloadToSearch);
        }

        query.setParameter("httpMethod", httpMethod);
        query.setParameter("urlPattern", urlPattern);

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