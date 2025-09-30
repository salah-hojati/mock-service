package org.example.mock.ui;

import org.example.mock.entity.MockConfig;
import org.example.mock.service.MockConfigService;
import org.example.mock.util.JsonUtil;
import org.primefaces.PrimeFaces;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@ViewScoped
public class MockConfigBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(MockConfigBean.class.getName());

    @Inject
    private MockConfigService mockConfigService;

    private List<MockConfig> configs= new ArrayList<>();;
    private MockConfig selectedConfig;
    private String generatedCurlCommand;

    // List of HTTP methods for the dropdown
    private final List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

    @PostConstruct
    public void init() {
        try {
            this.configs = mockConfigService.findAll();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations", e);
            // Add an error message for the user
            addErrorMessage("Error Loading Data", "Could not retrieve configurations. See server logs for details.");
            // The 'configs' list is already an empty list, so the page won't crash.
        }
    }
    public void openNew() {
        this.selectedConfig = new MockConfig();
        // This command ensures the dialog form is truly empty
        PrimeFaces.current().resetInputs("form:manage-config-content");
    }
    public boolean isRequestBodyApplicable() {
        if (selectedConfig == null || selectedConfig.getHttpMethod() == null) {
            return false;
        }
        String method = selectedConfig.getHttpMethod().toUpperCase();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
    public void saveConfig() {
        try {

            if (!isRequestBodyApplicable()) {
                this.selectedConfig.setRequestPayload(null);
            }
            // Normalize JSON payloads before saving
            this.selectedConfig.setRequestPayload(JsonUtil.normalize(this.selectedConfig.getRequestPayload()));
            this.selectedConfig.setResponsePayload(JsonUtil.normalize(this.selectedConfig.getResponsePayload()));

            mockConfigService.save(this.selectedConfig);

            // A simple way to refresh the list is to just reload it from the DB
            this.configs = mockConfigService.findAll();
            addMessage("Configuration Saved", "The mock configuration was successfully saved.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving configuration", e);
            addErrorMessage("Save Error", "An unexpected error occurred. Check server logs for details.");
        }
    }

    public void deleteConfig() {
        try {
            mockConfigService.delete(this.selectedConfig);
            this.configs.remove(this.selectedConfig);
            this.selectedConfig = null;
            addMessage("Configuration Deleted", "The selected configuration has been removed.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting configuration", e);
            addErrorMessage("Delete Error", "An unexpected error occurred. Check server logs for details.");
        }
    }

    // ===================== SOLUTION IS HERE =====================
    /**
     * Generates a dynamic curl command based on the mock configuration's HTTP method.
     * @param config The MockConfig to generate the command for.
     */
    public void generateCurl(MockConfig config) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // Construct the base URL (e.g., http://localhost:7001/mock-service)
        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        String fullUrl = String.format("%s/api/mock/%s", baseUrl, config.getUrlPattern());
        String httpMethod = config.getHttpMethod().toUpperCase();

        StringBuilder curl = new StringBuilder("curl -i -X ");
        curl.append(httpMethod).append(" "); // Use the dynamic method
        curl.append("'").append(fullUrl).append("'");

        // Only add Content-Type and data for methods that typically have a body
        boolean hasBody = "POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod);

        if (hasBody) {
            curl.append(" \\\n"); // Add line break for readability
            curl.append("-H 'Content-Type: application/json'");

            if (config.getRequestPayload() != null && !config.getRequestPayload().isEmpty()) {
                // Escape single quotes in the JSON payload for shell safety
                String escapedPayload = config.getRequestPayload().replace("'", "'\\''");
                curl.append(" \\\n");
                curl.append("-d '").append(escapedPayload).append("'");
            }
        }

        this.generatedCurlCommand = curl.toString();
    }
    // ============================================================

    private void addMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    // Getters and Setters
    public List<MockConfig> getConfigs() { return configs; }
    public void setConfigs(List<MockConfig> configs) { this.configs = configs; }
    public MockConfig getSelectedConfig() { return selectedConfig; }
    public void setSelectedConfig(MockConfig selectedConfig) { this.selectedConfig = selectedConfig; }
    public String getGeneratedCurlCommand() { return generatedCurlCommand; }
    public List<String> getHttpMethods() { return httpMethods; }
}