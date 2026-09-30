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
     * Backwards-compatible overload — no header matching (all configs match).
     * Useful if some callers cannot supply headers.
     */
    public MockConfig findMockConfig(String httpMethod, String urlPattern, String requestPayload) {
        return findMockConfig(httpMethod, urlPattern, requestPayload, null);
    }

    public MockConfig findMockConfig(String httpMethod,
                                     String urlPattern,
                                     String requestPayload,
                                     MultivaluedMap<String, String> incomingHeaders) {

        String payloadToSearch = JsonUtil.normalize(requestPayload);

        // Load every config for this method + URL — filtering happens in Java
        // because the "match body / match headers" rules are per-row.
        TypedQuery<MockConfig> query = em.createQuery(
                "SELECT m FROM MockConfig m "
                        + "WHERE m.httpMethod = :httpMethod "
                        + "  AND m.urlPattern = :urlPattern "
                        + "ORDER BY m.id",                 // stable order
                MockConfig.class);
        query.setParameter("httpMethod", httpMethod);
        query.setParameter("urlPattern", urlPattern);

        List<MockConfig> candidates;
        try {
            candidates = query.getResultList();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE,
                    "Error finding mock config for " + httpMethod + " " + urlPattern, ex);
            return null;
        }

        if (candidates.isEmpty()) {
            LOGGER.log(Level.FINE, "No mock configs registered for {0} {1}",
                    new Object[]{httpMethod, urlPattern});
            return null;
        }

        // Pick the FIRST candidate whose flags allow a match.
        // (Sort by id, so the oldest rule wins — predictable.)
        MockConfig best = null;
        for (MockConfig c : candidates) {
            if (!matches(c, payloadToSearch, incomingHeaders)) continue;

            // Prefer a more specific rule over a wildcard when both match.
            if (best == null || specificity(c) > specificity(best)) {
                best = c;
            }
        }

        if (best == null) {
            LOGGER.log(Level.FINE, "Configs exist for {0} {1} but none matched the request.",
                    new Object[]{httpMethod, urlPattern});
        }
        return best;
    }

    /** Does this config accept the given request? */
    private boolean matches(MockConfig c, String payload, MultivaluedMap<String, String> headers) {

        // 1) Body check — only when the rule asks for it
        if (Boolean.TRUE.equals(c.getMatchBody())) {
            String stored = JsonUtil.normalize(c.getRequestPayload());
            if (stored == null) {
                // rule says "match body" but rule body is empty → require empty request body
                if (payload != null) return false;
            } else if (!stored.equals(payload)) {
                return false;
            }
        }

        // 2) Header check — only when the rule asks for it
        if (Boolean.TRUE.equals(c.getMatchHeaders())) {
            if (!headersMatch(c.getRequestHeaders(), headers)) return false;
        }

        return true;
    }

    /** Bigger = more specific = preferred when several rules match. */
    private int specificity(MockConfig c) {
        int score = 0;
        if (Boolean.TRUE.equals(c.getMatchBody())
                && c.getRequestPayload() != null && !c.getRequestPayload().isEmpty()) score += 2;
        if (Boolean.TRUE.equals(c.getMatchHeaders())
                && c.getRequestHeaders() != null && !c.getRequestHeaders().isEmpty()) score += 1;
        return score;
    }

    // headersMatch(...) — unchanged from the previous version.
    private boolean headersMatch(String storedHeaders, MultivaluedMap<String, String> incomingHeaders) {
        if (storedHeaders == null || storedHeaders.trim().isEmpty()) return true;
        if (incomingHeaders == null) return false;

        for (String line : storedHeaders.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            int i = t.indexOf(':');
            if (i <= 0) continue;
            String name     = t.substring(0, i).trim();
            String expected = t.substring(i + 1).trim();

            String actual = null;
            for (Map.Entry<String, List<String>> e : incomingHeaders.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                    List<String> v = e.getValue();
                    actual = (v == null || v.isEmpty()) ? null : v.get(0);
                    break;
                }
            }
            if (actual == null || !actual.equals(expected)) return false;
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