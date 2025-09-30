package org.example.mock.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "GATEWAY_CONFIG")
@NamedQuery(name = "GatewayConfig.findAll", query = "SELECT g FROM GatewayConfig g ORDER BY g.id")
public class GatewayConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gateway_config_seq")
    @SequenceGenerator(name = "gateway_config_seq", sequenceName = "GATEWAY_CONFIG_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SOURCE_URL_PATTERN", nullable = false, unique = true)
    private String sourceUrlPattern;

    @Column(name = "TARGET_BASE_URL", nullable = false)
    private String targetBaseUrl;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Column(name = "DESCRIPTION")
    private String description;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceUrlPattern() { return sourceUrlPattern; }
    public void setSourceUrlPattern(String sourceUrlPattern) { this.sourceUrlPattern = sourceUrlPattern; }
    public String getTargetBaseUrl() { return targetBaseUrl; }
    public void setTargetBaseUrl(String targetBaseUrl) { this.targetBaseUrl = targetBaseUrl; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GatewayConfig that = (GatewayConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}