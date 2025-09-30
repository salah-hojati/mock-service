package org.example.mock.ui;

import org.example.mock.entity.GatewayConfig;
import org.example.mock.entity.GatewayLog;
import org.example.mock.service.GatewayService;
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

@Named
@ViewScoped
public class GatewayBean implements Serializable {

    @Inject
    private GatewayService gatewayService;

    private List<GatewayConfig> configs = new ArrayList<>();
    private GatewayConfig selectedConfig;
    private List<GatewayLog> selectedConfigLogs = new ArrayList<>();

    // Add these two fields to your GatewayBean.java
    private String generatedCurlCommand;
    private final List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE");
// In GatewayBean.java

    // Add a new field to hold the single log entry you want to view in detail.
    private GatewayLog detailedLog;

    // Add a new method to set the selected log and show the dialog.
    public void viewLogDetails(GatewayLog log) {
       // this.detailedLog = log;
        this.detailedLog = gatewayService.findLogDetails(log.getId());
        // Use an absolute path to update the content of the new dialog
      //  PrimeFaces.current().ajax().update(":log-detail-dialog-form:log-detail-content");
      // PrimeFaces.current().executeScript("PF('logDetailDialog').show()");
    }

    // Add the getter for the new field.
    public GatewayLog getDetailedLog() {
        return detailedLog;
    }

    @PostConstruct
    public void init() {
        try {
            this.configs = gatewayService.findAllConfigs();
        } catch (Exception e) {
            // Log the error and show a message to the user
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error Loading Data", "Could not retrieve gateway configurations from the database."));
            // The 'configs' list will remain an empty list, preventing the page from crashing.
        }
    }

    public void openNew() {
        this.selectedConfig = new GatewayConfig();
    }

    public void saveConfig() {
        try {
            gatewayService.saveConfig(this.selectedConfig);
            this.configs = gatewayService.findAllConfigs(); // Refresh list
            addInfoMessage("Gateway Rule Saved", "The configuration was saved successfully.");

            // Let the XHTML handle UI updates
            PrimeFaces.current().ajax().update("form:messages", "form:dt-configs");
            PrimeFaces.current().executeScript("PF('manageGatewayDialog').hide()");
        } catch (Exception e) {
            addErrorMessage("Save Error", e.getMessage());
        }
    }


    public void deleteConfig() {
        try {
            gatewayService.deleteConfig(this.selectedConfig);
            this.configs.remove(this.selectedConfig);
            this.selectedConfig = null;
            addInfoMessage("Gateway Rule Deleted", "The configuration was removed.");
        } catch (Exception e) {
            addErrorMessage("Delete Error", "Could not delete the configuration.");
        }
    }

    public void viewLogs(GatewayConfig config) {
        this.selectedConfig = config;
        try {
            this.selectedConfigLogs = gatewayService.findLogsForConfig(config.getId(), 100);
        } catch (Exception e) {
            this.selectedConfigLogs = new ArrayList<>();
            addErrorMessage("Error Loading Logs", "Could not retrieve logs for the selected rule.");
        }
    }
    
    public void clearLogs() {
        if (selectedConfig != null) {
            gatewayService.clearLogsForConfig(selectedConfig.getId());
            this.selectedConfigLogs.clear(); // Clear the list in the UI
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Logs Cleared", "All logs for this rule have been deleted."));
            PrimeFaces.current().ajax().update("form:messages", "log-dialog-content");
        }
    }
// Add this method to your GatewayBean.java

    /**
     * Generates example curl commands for the selected gateway rule.
     * Since a gateway can handle any HTTP method, this provides templates for common use cases.
     *
     * @param config The GatewayConfig to generate the command for.
     */
    public void generateCurl(GatewayConfig config) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        // 1. Dynamically construct the base URL of this application
        // e.g., http://localhost:8080/mock-service
        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        // 2. Construct the full URL for the gateway endpoint
        String fullUrl = String.format("%s/api/gateway/%s", baseUrl, config.getSourceUrlPattern());

        // 3. Create example commands for different methods
        String getExample = String.format(
                "# Example for a GET request:\n" +
                        "curl -i -X GET '%s'",
                fullUrl
        );

        String postExample = String.format(
                "# Example for a POST request with a JSON body:\n" +
                        "curl -i -X POST '%s' \\\n" +
                        "-H 'Content-Type: application/json' \\\n" +
                        "-d '{\"key\": \"value\", \"some_data\": 123}'",
                fullUrl
        );

        // 4. Combine the examples into a single string for display
        this.generatedCurlCommand = getExample + "\n\n" + postExample;
    }


    // --- Helper Methods for Messages ---
    private void addInfoMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }
    // Getters and Setters
    public List<GatewayConfig> getConfigs() { return configs; }
    public GatewayConfig getSelectedConfig() { return selectedConfig; }
    public void setSelectedConfig(GatewayConfig selectedConfig) { this.selectedConfig = selectedConfig; }
    public List<GatewayLog> getSelectedConfigLogs() { return selectedConfigLogs; }

// Add these getters to your GatewayBean.java

    public String getGeneratedCurlCommand() {
        return generatedCurlCommand;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }


}