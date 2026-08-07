package org.example.mock.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "CATEGORY")
@NamedQuery(name = "Category.findAll", query = "SELECT c FROM Category c ORDER BY c.name")
public class Category implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_seq")
    @SequenceGenerator(name = "category_seq", sequenceName = "CATEGORY_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "COLOR", length = 20)
    private String color = "#3B82F6"; // Default blue

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "USER_ID", nullable = false)
//    private AppUser user;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<MockConfig> mockConfigs = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<MockConfig2> mockConfigs2 = new ArrayList<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<GatewayConfig> gatewayConfigs = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
//    public AppUser getUser() { return user; }
//    public void setUser(AppUser user) { this.user = user; }
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
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}