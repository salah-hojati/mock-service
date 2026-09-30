package org.example.mock.ui;

import org.example.mock.entity.MockConfig;
import org.example.mock.service.CategoryService;
import org.example.mock.service.MockConfigServiceByInput;
import org.example.mock.ui2.LoginBean;
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
public class MockConfigBeanByInput implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(MockConfigBeanByInput.class.getName());

    @Inject
    private MockConfigServiceByInput mockConfigService;

   @Inject
   private LoginBean loginBean;
    @Inject
    private CategoryService categoryService;
    private List<MockConfig> configs= new ArrayList<>();;
    private MockConfig selectedConfig;
    private String generatedCurlCommand;
    private String curlCommandToCopy;
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
            this.selectedConfig.setUser(loginBean.getUser());

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

    /* ============================================================
     * Curl generation
     * ============================================================ */

    /** Called from the row button — reloads the row first, then builds the curl. */
    public void generateCurl(MockConfig selectedConfig) {
        this.selectedConfig = mockConfigService.findById(selectedConfig.getId());
        this.generatedCurlCommand = buildCurl(this.selectedConfig);
        this.curlCommandToCopy     = this.generatedCurlCommand;
    }

    /** Called with no args (e.g. from the dialog) — uses the current selectedConfig. */
    public void generateCurl() {
        if (this.selectedConfig == null) {
            LOGGER.warning("generateCurl() called with null selectedConfig.");
            this.generatedCurlCommand = "# Error: No configuration was selected.";
            this.curlCommandToCopy    = this.generatedCurlCommand;
            return;
        }
        this.generatedCurlCommand = buildCurl(this.selectedConfig);
        this.curlCommandToCopy    = this.generatedCurlCommand;
    }

    /**
     * Builds a curl command from a MockConfig.
     *
     * Header handling:
     *   - The stored `requestHeaders` field is a multi-line "Name: value" list.
     *   - Each valid line becomes its own `-H 'Name: value'` argument.
     *   - If POST/PUT/PATCH and the user did NOT declare a Content-Type header,
     *     `-H 'Content-Type: application/json'` is added automatically.
     *   - If the user DID declare Content-Type, their value is used instead.
     */
    private String buildCurl(MockConfig config) {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request =
                (HttpServletRequest) context.getExternalContext().getRequest();

        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        String fullUrl    = String.format("%s/api/mock/%s", baseUrl, config.getUrlPattern());
        String httpMethod = config.getHttpMethod().toUpperCase();

        StringBuilder curl = new StringBuilder("curl -i -X ");
        curl.append(httpMethod).append(" '").append(fullUrl).append("'");

        boolean hasBody = "POST".equals(httpMethod)
                || "PUT".equals(httpMethod)
                || "PATCH".equals(httpMethod);

        // ---- Parse stored header lines into [name, value] pairs ----
        List<String[]> headers = parseHeaderLines(config.getRequestHeaders());

        // Does the user already specify Content-Type?
        boolean userSetContentType = headers.stream()
                .anyMatch(h -> h[0].equalsIgnoreCase("Content-Type"));

        // Auto-add Content-Type for body-bearing methods only if user didn't.
        if (hasBody && !userSetContentType) {
            curl.append(" \\\n").append("-H 'Content-Type: application/json'");
        }

        // Emit every user-declared header.
        for (String[] header : headers) {
            curl.append(" \\\n")
                    .append("-H '")
                    .append(escapeSingleQuotes(header[0])).append(": ")
                    .append(escapeSingleQuotes(header[1]))
                    .append("'");
        }

        // ---- Request body ----
        if (hasBody
                && config.getRequestPayload() != null
                && !config.getRequestPayload().isEmpty()) {
            curl.append(" \\\n");
            curl.append("-d '")
                    .append(escapeSingleQuotes(config.getRequestPayload()))
                    .append("'");
        }

        return curl.toString();
    }

    /** Parses "Name: value" lines. Blank lines and #-comments are ignored. */
    private List<String[]> parseHeaderLines(String raw) {
        List<String[]> headers = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return headers;
        }
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int idx = trimmed.indexOf(':');
            if (idx <= 0) {
                continue;                         // malformed, skip
            }
            String name  = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (!name.isEmpty()) {
                headers.add(new String[]{name, value});
            }
        }
        return headers;
    }

    /** Escapes ' for use inside single-quoted shell strings. */
    private String escapeSingleQuotes(String s) {
        return s == null ? "" : s.replace("'", "'\\''");
    }




    /**
     * Clones an existing config into a new (unsaved) one and opens the dialog.
     * The new row keeps every field except the ID and creation metadata,
     * so it becomes a fresh INSERT when the user clicks Save.
     */
    public void duplicateConfig(MockConfig source) {
        if (source == null) {
            return;
        }
        try {
            // Reload the full entity (in case the row is a lazy proxy)
            MockConfig original = mockConfigService.findById(source.getId());
            if (original == null) {
                addErrorMessage("Clone Error", "Source configuration no longer exists.");
                return;
            }

            MockConfig copy = new MockConfig();

            // --- Copy every field EXCEPT the identity / audit columns ---
            copy.setHttpMethod(original.getHttpMethod());
            copy.setUrlPattern(original.getUrlPattern());
            copy.setRequestPayload(original.getRequestPayload());
            copy.setRequestHeaders(original.getRequestHeaders());
            copy.setResponsePayload(original.getResponsePayload());
            copy.setDelayMs(original.getDelayMs());
            copy.setHttpStatusCode(original.getHttpStatusCode());

            // Description gets a "(copy)" suffix so you can tell them apart.
            String desc = original.getDescription();
            copy.setDescription(
                    (desc == null || desc.isEmpty())
                            ? "(copy)"
                            : desc + " (copy)");

            // IMPORTANT: leave id == null so JPA treats this as a new row.
            // copy.setId(null) is not needed because it's already null on a fresh object.

            this.selectedConfig = copy;

            addMessage("Cloned",
                    "A copy of the configuration was created. Review it and click Save.");

            // Reset any stale form state, then let the dialog render the new values.
            PrimeFaces.current().resetInputs("form:manage-config-content");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error duplicating configuration", e);
            addErrorMessage("Clone Error", "Could not duplicate the configuration. Check server logs.");
        }
    }

    // ============================================================

    private void addMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    public String getCurlCommandToCopy() {
        return curlCommandToCopy;
    }

    public void setCurlCommandToCopy(String curlCommandToCopy) {
        this.curlCommandToCopy = curlCommandToCopy;
    }

    // Getters and Setters
    public List<MockConfig> getConfigs() { return configs; }
    public void setConfigs(List<MockConfig> configs) { this.configs = configs; }
    public MockConfig getSelectedConfig() { return selectedConfig; }
    public void setSelectedConfig(MockConfig selectedConfig) { this.selectedConfig = selectedConfig; }
    public String getGeneratedCurlCommand() { return generatedCurlCommand; }
    public List<String> getHttpMethods() { return httpMethods; }
}