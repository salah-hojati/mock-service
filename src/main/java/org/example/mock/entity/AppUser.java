package org.example.mock.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "APP_USER")
@NamedQuery(name = "AppUser.findByUsername", query = "SELECT u FROM AppUser u WHERE u.username = :username")
@NamedQuery(name = "AppUser.findAll", query = "SELECT u FROM AppUser u ORDER BY u.id")
public class AppUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_seq")
    @SequenceGenerator(name = "app_user_seq", sequenceName = "APP_USER_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "FULL_NAME", length = 100)
    private String fullName;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Column(name = "CREATED_AT")
    private java.sql.Timestamp createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<MockConfig> mockConfigs = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<MockConfig2> mockConfigs2 = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<GatewayConfig> gatewayConfigs = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public List<MockConfig> getMockConfigs() { return mockConfigs; }
    public void setMockConfigs(List<MockConfig> mockConfigs) { this.mockConfigs = mockConfigs; }
    public List<MockConfig2> getMockConfigs2() { return mockConfigs2; }
    public void setMockConfigs2(List<MockConfig2> mockConfigs2) { this.mockConfigs2 = mockConfigs2; }
    public List<GatewayConfig> getGatewayConfigs() { return gatewayConfigs; }
    public void setGatewayConfigs(List<GatewayConfig> gatewayConfigs) { this.gatewayConfigs = gatewayConfigs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUser appUser = (AppUser) o;
        return Objects.equals(id, appUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}