package org.example.mock.entity;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "MOCK_CONFIG")
@NamedQuery(name = "MockConfig.findAll", query = "SELECT m FROM MockConfig m ORDER BY m.id")
public class MockConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mock_config_seq")
    @SequenceGenerator(name = "mock_config_seq", sequenceName = "MOCK_CONFIG_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "URL_PATTERN", nullable = false)
    private String urlPattern;

    @Lob // Large Object for CLOB
    @Column(name = "REQUEST_PAYLOAD")
    private String requestPayload;

    @Lob // Large Object for CLOB
    @Column(name = "RESPONSE_PAYLOAD", nullable = false)
    private String responsePayload;


    @Min(0) // Ensures the value is not negative
    @Column(name = "DELAY_MS", nullable = false)
    private Integer delayMs = 0; // Default to 0


    @Min(100)
    @Max(599)
    @Column(name = "HTTP_STATUS_CODE", nullable = false)
    private Integer httpStatusCode = 200; // Default to 200 OK


    @Column(name = "HTTP_METHOD", nullable = false, length = 10)
    private String httpMethod = "POST"; // Default to POST

    // ... existing getters and setters ...

    // Add getter and setter for the new httpMethod field
    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public Integer getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(Integer delayMs) {
        this.delayMs = delayMs;
    }



    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUrlPattern() { return urlPattern; }
    public void setUrlPattern(String urlPattern) { this.urlPattern = urlPattern; }
    public String getRequestPayload() { return requestPayload; }
    public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MockConfig that = (MockConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}