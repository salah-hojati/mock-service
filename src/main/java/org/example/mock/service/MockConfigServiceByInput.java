package org.example.mock.service;

import org.example.mock.entity.MockConfig;
import org.example.mock.util.JsonUtil;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class MockConfigServiceByInput {

    private static final Logger LOGGER = Logger.getLogger(MockConfigServiceByInput.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

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
     * Finds a mock configuration based on URL pattern, request body and request headers.
     *
     * Header matching is a SUBSET match: every header declared in
     * {@code MockConfig.requestHeaders} must be present on the incoming request
     * with the exact same value. Extra headers on the request are ignored.
     *
     * @return the best matching MockConfig, or {@code null} if none match.
     */
    public MockConfig findMockConfig(String httpMethod,
                                     String urlPattern,
                                     String requestPayload,
                                     MultivaluedMap<String, String> incomingHeaders) {

        String payloadToSearch = JsonUtil.normalize(requestPayload);

        TypedQuery<MockConfig> query;
        String jpql;

        if (payloadToSearch == null) {
            jpql = "SELECT m FROM MockConfig m "
                    + "WHERE m.httpMethod = :httpMethod "
                    + "  AND m.urlPattern = :urlPattern "
                    + "  AND m.requestPayload IS NULL";
            query = em.createQuery(jpql, MockConfig.class);
        } else {
            jpql = "SELECT m FROM MockConfig m "
                    + "WHERE m.httpMethod = :httpMethod "
                    + "  AND m.urlPattern = :urlPattern "
                    + "  AND CAST(m.requestPayload AS VARCHAR2(4000)) = :payload";
            query = em.createQuery(jpql, MockConfig.class);
            query.setParameter("payload", payloadToSearch);
        }

        query.setParameter("httpMethod", httpMethod);
        query.setParameter("urlPattern", urlPattern);

        List<MockConfig> candidates;
        try {
            candidates = query.getResultList();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,
                    "Error finding mock config for method: " + httpMethod + ", URL: " + urlPattern, ex);
            return null;
        }

        if (candidates.isEmpty()) {
            LOGGER.log(Level.FINE,
                    "No mock configuration found for method: {0}, URL: {1}",
                    new Object[]{httpMethod, urlPattern});
            return null;
        }

        // Pick the first candidate whose header requirements are satisfied.
        for (MockConfig candidate : candidates) {
            if (headersMatch(candidate.getRequestHeaders(), incomingHeaders)) {
                return candidate;
            }
        }

        LOGGER.log(Level.FINE,
                "Found {0} candidate(s) for {1} {2} but none matched the request headers.",
                new Object[]{candidates.size(), httpMethod, urlPattern});
        return null;
    }

    /**
     * Backwards-compatible overload — no header matching (all configs match).
     * Useful if some callers cannot supply headers.
     */
    public MockConfig findMockConfig(String httpMethod, String urlPattern, String requestPayload) {
        return findMockConfig(httpMethod, urlPattern, requestPayload, null);
    }

    // ------------------------------------------------------------------
    // Header matching helpers
    // ------------------------------------------------------------------

    /**
     * Returns {@code true} when every header declared in {@code storedHeaders}
     * is present in {@code incomingHeaders} with the same value.
     *
     * <p>Format of {@code storedHeaders}: one entry per line,
     * {@code Name: value}. Blank lines and lines starting with {@code #} are
     * ignored. If {@code storedHeaders} is empty, everything matches.</p>
     */
    private boolean headersMatch(String storedHeaders,
                                 MultivaluedMap<String, String> incomingHeaders) {

        if (storedHeaders == null || storedHeaders.trim().isEmpty()) {
            return true;
        }
        if (incomingHeaders == null) {
            return false;
        }

        for (String line : storedHeaders.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int idx = trimmed.indexOf(':');
            if (idx <= 0) {
                continue; // malformed line, skip
            }

            String name     = trimmed.substring(0, idx).trim();
            String expected = trimmed.substring(idx + 1).trim();

            String actual = lookupHeaderIgnoreCase(incomingHeaders, name);
            if (actual == null || !actual.equals(expected)) {
                LOGGER.log(Level.FINE,
                        "Header mismatch: expected {0}=''{1}'' but request had ''{2}''",
                        new Object[]{name, expected, actual});
                return false;
            }
        }
        return true;
    }

    /** Case-insensitive lookup of a single header value from a JAX-RS map. */
    private String lookupHeaderIgnoreCase(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                List<String> values = entry.getValue();
                return (values == null || values.isEmpty()) ? null : values.get(0);
            }
        }
        return null;
    }
}