package org.example.mock.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "GATEWAY_LOG")
@NamedQuery(name = "GatewayLog.findByConfigId", query = "SELECT l FROM GatewayLog l WHERE l.gatewayConfig.id = :configId ORDER BY l.timestamp DESC")
public class GatewayLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gateway_log_seq")
    @SequenceGenerator(name = "gateway_log_seq", sequenceName = "GATEWAY_LOG_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GATEWAY_CONFIG_ID", nullable = false)
    private GatewayConfig gatewayConfig;

    @Column(name = "TIMESTAMP", nullable = false)
    private Timestamp timestamp;

    @Column(name = "REQUEST_METHOD", length = 10)
    private String requestMethod;

    // --- SOLUTION: Remove @Lob and specify a length ---
    // A length of 2048 is generous for a URL.
    @Column(name = "REQUEST_URL", length = 2048)
    private String requestUrl;

    // A length of 4000 is the max for VARCHAR2 and is plenty for headers.
    @Lob
    @Column(name = "REQUEST_HEADERS", length = 4000)
    private String requestHeaders;

    // For bodies, we can keep @Lob or use a large VARCHAR. Let's try a large VARCHAR first.
    @Lob
    @Column(name = "REQUEST_BODY", length = 4000)
    private String requestBody;

    @Column(name = "RESPONSE_STATUS_CODE")
    private int responseStatusCode;
    @Lob
    @Column(name = "RESPONSE_HEADERS", length = 4000)
    private String responseHeaders;
    @Lob
    @Column(name = "RESPONSE_BODY", length = 4000)
    private String responseBody;

    @Column(name = "DURATION_MS")
    private long durationMs;

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GatewayConfig getGatewayConfig() { return gatewayConfig; }
    public void setGatewayConfig(GatewayConfig gatewayConfig) { this.gatewayConfig = gatewayConfig; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public int getResponseStatusCode() { return responseStatusCode; }
    public void setResponseStatusCode(int responseStatusCode) { this.responseStatusCode = responseStatusCode; }
    public String getResponseHeaders() { return responseHeaders; }
    public void setResponseHeaders(String responseHeaders) { this.responseHeaders = responseHeaders; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public GatewayLog() {
        // Default constructor
    }

    // --- SOLUTION: Add the constructor used by our new query ---
    public GatewayLog(Long id, Timestamp timestamp, String requestMethod, int responseStatusCode, long durationMs) {
        this.id = id;
        this.timestamp = timestamp;
        this.requestMethod = requestMethod;
        this.responseStatusCode = responseStatusCode;
        this.durationMs = durationMs;
    }
}