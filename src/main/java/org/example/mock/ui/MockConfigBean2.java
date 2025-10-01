package org.example.mock.ui;

import org.example.mock.entity.MockConfig2;
import org.example.mock.service.MockConfigService2;
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
public class MockConfigBean2 implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(MockConfigBean2.class.getName());

    // This could be a static final constant for a minor performance gain.
    private static final List<String> HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE");

    @Inject
    private MockConfigService2 mockConfigService;

    private List<MockConfig2> configs = new ArrayList<>();
    private MockConfig2 selectedConfig;
    private String generatedCurlCommand;
    private String curlCommandToCopy;


    @PostConstruct
    public void init() {
        try {
            this.configs = mockConfigService.findAll();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations for Bean2", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error Loading Data", "Could not retrieve configurations."));
        }
    }

    public void openNew() {
        this.selectedConfig = new MockConfig2();
    }

    public void saveConfig() {
        try {
            // Normalize JSON payloads before saving
            this.selectedConfig.setCapturedRequestPayload(JsonUtil.normalize(this.selectedConfig.getCapturedRequestPayload()));
            this.selectedConfig.setResponsePayload(JsonUtil.normalize(this.selectedConfig.getResponsePayload()));

            mockConfigService.save(this.selectedConfig);
            this.configs = mockConfigService.findAll(); // Refresh list from DB
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Configuration Saved"));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", false);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save Error", e.getMessage()));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
        }
    }

    public void deleteConfig() {
        try {
            mockConfigService.delete(this.selectedConfig);
            this.configs = mockConfigService.findAll(); // IMPROVEMENT: Refresh list from DB for consistency.
            this.selectedConfig = null;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Configuration Deleted"));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete configuration", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Delete Error", "Could not delete the selected configuration."));
        }
    }

    public void generateCurl() {
        if (this.selectedConfig == null) {
            LOGGER.warning("generateCurl() was called but selectedConfig is null. This indicates a mismatch between the dataTable 'var' and the f:setPropertyActionListener 'value'.");
            this.generatedCurlCommand = "# Error: No configuration was selected to generate the command.";
            this.curlCommandToCopy = this.generatedCurlCommand;
            return;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        String fullUrl = String.format("%s/api/mock2/%s", baseUrl, this.selectedConfig.getUrlPattern());
        String httpMethod = this.selectedConfig.getHttpMethod().toUpperCase();

        String command = String.format("curl -i -X %s '%s'", httpMethod, fullUrl);
        this.curlCommandToCopy = command;
        this.generatedCurlCommand = command;
    }

    public List<String> getHttpMethods() {
        return HTTP_METHODS;
    }

    // Getters and Setters
    public List<MockConfig2> getConfigs() {
        return configs;
    }

    public MockConfig2 getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(MockConfig2 selectedConfig) {
        // You can add a log here for debugging if you want:
        // LOGGER.info("setSelectedConfig called with: " + (selectedConfig != null ? selectedConfig.getId() : "null"));
        this.selectedConfig = selectedConfig;
    }

    public String getGeneratedCurlCommand() {
        return generatedCurlCommand;
    }

    public String getCurlCommandToCopy() {
        return curlCommandToCopy;
    }

    // IMPROVEMENT: Added the missing setter for curlCommandToCopy.
    public void setCurlCommandToCopy(String curlCommandToCopy) {
        this.curlCommandToCopy = curlCommandToCopy;
    }
}