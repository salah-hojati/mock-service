package org.example.mock.ui;

import org.example.mock.entity.MockConfig2;
import org.example.mock.service.MockConfigService2;
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

    @Inject
    private MockConfigService2 mockConfigService;

    private List<MockConfig2> configs = new ArrayList<>();
    private MockConfig2 selectedConfig;
    private String generatedCurlCommand;


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
            mockConfigService.save(this.selectedConfig);
            this.configs = mockConfigService.findAll(); // Refresh list
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Configuration Saved"));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", false);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save Error", e.getMessage()));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
        }
    }

    public void deleteConfig() {
        mockConfigService.delete(this.selectedConfig);
        this.configs.remove(this.selectedConfig);
        this.selectedConfig = null;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Configuration Deleted"));
    }

    public void generateCurl(MockConfig2 config) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // Dynamically construct the base URL (e.g., http://localhost:7001/mock-service)
        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        // Use the '/api/mock2/' path for this version
        String fullUrl = String.format("%s/api/mock2/%s", baseUrl, config.getUrlPattern());
        String httpMethod = config.getHttpMethod().toUpperCase();

        // Since this version doesn't handle request bodies, the command is simpler.
        // The -i flag is added to include response headers, which is useful for debugging.
        String command = String.format("curl -i -X %s '%s'", httpMethod, fullUrl);

        this.generatedCurlCommand = command;
    }

    public List<String> getHttpMethods() {
        return Arrays.asList("GET", "POST", "PUT", "DELETE");
    }

    // Getters and Setters
    public List<MockConfig2> getConfigs() {
        return configs;
    }

    public MockConfig2 getSelectedConfig() {
        return selectedConfig;
    }

    public void setSelectedConfig(MockConfig2 selectedConfig) {
        this.selectedConfig = selectedConfig;
    }

    public String getGeneratedCurlCommand() {
        return generatedCurlCommand;
    }
}