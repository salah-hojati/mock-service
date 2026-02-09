package org.example.mock.service;

import org.example.mock.entity.MockConfig;
import org.example.mock.entity.MockConfig2;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MockConfigService2 {

    private static final Logger LOGGER = Logger.getLogger(MockConfigService2.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    public List<MockConfig2> findAll() {
        return em.createNamedQuery("MockConfig2.findAll", MockConfig2.class).getResultList();
    }

    public void save(MockConfig2 mockConfig) {
        if (mockConfig.getId() == null) {
            em.persist(mockConfig);
        } else {
            em.merge(mockConfig);
        }
    }
    public MockConfig2 findById(Long id) {
        return em.find(MockConfig2.class, id);
    }
    public void delete(MockConfig2 mockConfig) {
        if (em.contains(mockConfig)) {
            em.remove(mockConfig);
        } else {
            em.remove(em.merge(mockConfig));
        }
    }

    /**
     * Finds a mock configuration based on the HTTP method and URL pattern.
     * @return The matching MockConfig2 object, or null if not found.
     */
    public MockConfig2 findMockConfig(String httpMethod, String urlPattern) {
        String jpql = "SELECT m FROM MockConfig2 m WHERE m.httpMethod = :httpMethod AND m.urlPattern = :urlPattern";
        TypedQuery<MockConfig2> query = em.createQuery(jpql, MockConfig2.class);
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