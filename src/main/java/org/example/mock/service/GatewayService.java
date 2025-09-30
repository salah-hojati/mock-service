package org.example.mock.service;

import org.example.mock.entity.GatewayConfig;
import org.example.mock.entity.GatewayLog;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Collections;
import java.util.List;

@Stateless
public class GatewayService {

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    // --- GatewayConfig Methods ---

    public List<GatewayConfig> findAllConfigs() {
        return em.createNamedQuery("GatewayConfig.findAll", GatewayConfig.class).getResultList();
    }

    public void saveConfig(GatewayConfig config) {
        if (config.getId() == null) {
            em.persist(config);
        } else {
            em.merge(config);
        }
    }

    public void deleteConfig(GatewayConfig config) {
        if (em.contains(config)) {
            em.remove(config);
        } else {
            em.remove(em.merge(config));
        }
    }

    public GatewayConfig findActiveConfigBySourcePattern(String sourceUrlPattern) {
        try {
            return em.createQuery("SELECT g FROM GatewayConfig g WHERE g.sourceUrlPattern = :pattern AND g.enabled = true", GatewayConfig.class)
                    .setParameter("pattern", sourceUrlPattern)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // --- GatewayLog Methods ---

    public void saveLog(GatewayLog log) {
        em.persist(log);
    }

    public List<GatewayLog> findLogsForConfig(Long configId, int maxResults) {
        // This is a JPQL Constructor Expression.
        // It creates new GatewayLog objects using a custom constructor,
        // selecting only the non-LOB fields we need for the summary table.
        String jpql = "SELECT NEW org.example.mock.entity.GatewayLog(" +
                "l.id, l.timestamp, l.requestMethod, l.responseStatusCode, l.durationMs" +
                ") " +
                "FROM GatewayLog l " +
                "WHERE l.gatewayConfig.id = :configId " +
                "ORDER BY l.timestamp DESC";

        return em.createQuery(jpql, GatewayLog.class)
                .setParameter("configId", configId)
                .setMaxResults(maxResults)
                .getResultList();
    }
    public GatewayLog findLogDetails(Long logId) {
        return em.find(GatewayLog.class, logId);
    }
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void clearLogsForConfig(Long configId) {
        if (configId != null) {
            em.createQuery("DELETE FROM GatewayLog l WHERE l.gatewayConfig.id = :configId")
              .setParameter("configId", configId)
              .executeUpdate();
        }
    }
}