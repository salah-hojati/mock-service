package org.example.mock.ui2;

import org.example.mock.entity.Category;
import org.example.mock.entity.MockConfig;
import org.example.mock.entity.MockConfig2;
import org.example.mock.service.CategoryService;

import org.example.mock.service.MockConfigServiceByInputNew;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("mockConfigBeanByInputNew")
@ViewScoped
public class MockConfigBeanByInputNew implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(MockConfigBeanByInputNew.class.getName());

    @Inject
    private MockConfigServiceByInputNew mockConfigService;

    @Inject
    private transient CategoryService categoryService;

    private List<MockConfig> configs = new ArrayList<>();

   // private Long selectedConfigId;

    private MockConfig selectedConfig;
    private String generatedCurlCommand;
    private String curlCommandToCopy;

    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;

    private final List<String> httpMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH");

    @PostConstruct
    public void init() {
        try {
            loadCategories();
            loadConfigs();
            // Initialize selectedConfig to prevent null
            this.selectedConfig = new MockConfig();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations", e);
            addErrorMessage("Error Loading Data", "Could not retrieve configurations.");
        }
    }

    private void loadCategories() {
        try {
            categories = categoryService.findAllForCurrentUser();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load categories", e);
            categories = new ArrayList<>();
        }
    }

    private void loadConfigs() {
        try {
            if (selectedCategoryId != null && selectedCategoryId > 0) {
                configs = mockConfigService.findByCategory(selectedCategoryId);
            } else {
                configs = mockConfigService.findAll();
            }
            if(selectedConfig == null) {
                selectedConfig = new MockConfig();
                sampleConfig(selectedConfig);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations", e);
            configs = new ArrayList<>();
            addErrorMessage("Error Loading Data", "Could not retrieve configurations.");
        }
    }

    public void filterByCategoryWithParam() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String categoryIdStr = params.get("indexForm:mainTabView:categorySelect_input");

        LOGGER.info("filterByCategoryWithParam() called with categoryId: " + categoryIdStr);

        if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
            try {
                this.selectedCategoryId = Long.parseLong(categoryIdStr);
            } catch (NumberFormatException e) {
                this.selectedCategoryId = null;
            }
        } else {
            this.selectedCategoryId = null;
        }

        loadConfigs();
      //  addMessage("Filter Applied", "Showing " + configs.size() + " configurations");
    }

    public void filterByCategory() {
        loadConfigs();
    }


    public void selectAndEdit(MockConfig mockConfig) {
        // Select the category
        this.selectedConfig = mockConfig;

        // Create a copy for editing
        this.selectedConfig = new MockConfig();
        this.selectedConfig.setId(mockConfig.getId());
        this.selectedConfig.setCategory(mockConfig.getCategory());
        this.selectedConfig.setUser(mockConfig.getUser());
        this.selectedConfig.setDescription(mockConfig.getDescription());
        this.selectedConfig.setResponsePayload(mockConfig.getResponsePayload());
        this.selectedConfig.setHttpStatusCode(mockConfig.getHttpStatusCode());
        this.selectedConfig.setDelayMs(mockConfig.getDelayMs());
        this.selectedConfig.setRequestPayload(mockConfig.getRequestPayload());
        this.selectedConfig.setUrlPattern(mockConfig.getUrlPattern());


        System.out.println("MockConfig selected and prepared for edit: " + mockConfig.getId());
    }
    public void openNew() {
        sampleConfig(this.selectedConfig);
        this.selectedCategoryId = null;
       // this.selectedConfigId = null;
        PrimeFaces.current().resetInputs("manage-config-content");
    }

    private void sampleConfig(MockConfig selectedConfig) {
        selectedConfig = new MockConfig();
        selectedConfig.setHttpMethod("POST");
        selectedConfig.setHttpStatusCode(200);
        selectedConfig.setDelayMs(0);
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
            if (selectedConfig == null) {
                addErrorMessage("Error", "No configuration selected.");
                return;
            }

            if (!isRequestBodyApplicable()) {
                this.selectedConfig.setRequestPayload(null);
            }

            this.selectedConfig.setRequestPayload(JsonUtil.normalize(this.selectedConfig.getRequestPayload()));
            this.selectedConfig.setResponsePayload(JsonUtil.normalize(this.selectedConfig.getResponsePayload()));

            if (selectedCategoryId != null && selectedCategoryId > 0) {
                Category category = categoryService.findById(selectedCategoryId);
                if (category != null) {
                    this.selectedConfig.setCategory(category);
                }
            } else {
                this.selectedConfig.setCategory(null);
            }

            mockConfigService.save(this.selectedConfig);
            loadConfigs();

            // Find and set the saved config in the list
            if (selectedConfig.getId() != null) {
                for (MockConfig c : configs) {
                    if (c.getId().equals(selectedConfig.getId())) {
                        this.selectedConfig = c;
                        break;
                    }
                }
            }

            addMessage("Configuration Saved", "The mock configuration was successfully saved.");
            PrimeFaces.current().executeScript("PF('manageConfigDialog').hide()");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving configuration", e);
            addErrorMessage("Save Error", "An unexpected error occurred.");
        }
    }

    public void deleteConfig() {
        try {
            if (selectedConfig != null && selectedConfig.getId() != null) {
                mockConfigService.delete(this.selectedConfig);
                loadConfigs();
                this.selectedConfig = null;
               // this.selectedConfigId = null;
                addMessage("Configuration Deleted", "The selected configuration has been removed.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting configuration", e);
            addErrorMessage("Delete Error", "Could not delete the configuration.");
        }
    }

    public void generateCurl(MockConfig config) {
        LOGGER.info("generateCurl() called");
        if (config == null) {
            return;
        }

        // Reload from database to get latest data
        this.selectedConfig = mockConfigService.findById(config.getId());
        if (this.selectedConfig == null) {
            return;
        }

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        String fullUrl = String.format("%s/api/mock/%s", baseUrl, this.selectedConfig.getUrlPattern());
        String httpMethod = this.selectedConfig.getHttpMethod().toUpperCase();

        StringBuilder curl = new StringBuilder("curl -i -X ");
        curl.append(httpMethod).append(" ");
        curl.append("'").append(fullUrl).append("'");

        boolean hasBody = "POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod);

        if (hasBody) {
            curl.append(" \\\n");
            curl.append("-H 'Content-Type: application/json'");

            if (this.selectedConfig.getRequestPayload() != null && !this.selectedConfig.getRequestPayload().isEmpty()) {
                String escapedPayload = this.selectedConfig.getRequestPayload().replace("'", "'\\''");
                curl.append(" \\\n");
                curl.append("-d '").append(escapedPayload).append("'");
            }
        }
        this.curlCommandToCopy = curl.toString();
        this.generatedCurlCommand = curl.toString();
    }

    public void generateCurl() {
        if (this.selectedConfig == null || this.selectedConfig.getId() == null) {
            LOGGER.warning("generateCurl() was called but selectedConfig is null or has no ID.");
            this.generatedCurlCommand = "# Error: No configuration was selected.";
            this.curlCommandToCopy = this.generatedCurlCommand;
            return;
        }
        generateCurl(this.selectedConfig);
    }

    public String getCategoryName(Long categoryId) {
        if (categoryId == null) return "";
        for (Category cat : categories) {
            if (cat.getId().equals(categoryId)) {
                return cat.getName();
            }
        }
        return "";
    }

    private void addMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail));
    }

    private void addErrorMessage(String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
    }

    // ===== Getters and Setters =====

    public List<MockConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<MockConfig> configs) {
        this.configs = configs;
    }

    public MockConfig getSelectedConfig() {
        if (selectedConfig == null) {
            sampleConfig(selectedConfig);
        }
        return selectedConfig;
    }

    public void setSelectedConfig(MockConfig selectedConfig) {
        this.selectedConfig = selectedConfig;
        if (selectedConfig != null && selectedConfig.getCategory() != null) {
            this.selectedCategoryId = selectedConfig.getCategory().getId();
            generateCurl(selectedConfig);
           // this.selectedConfigId = selectedConfig.getId();
        } else {
            this.selectedCategoryId = null;
        //    this.selectedConfigId = null;
        }
    }



    public void setSelectedConfigId(Long selectedConfigId) {
       // this.selectedConfigId = selectedConfigId;
        // Load the config from the list when ID is set
        if (selectedConfigId != null) {
            for (MockConfig config : configs) {
                if (config.getId().equals(selectedConfigId)) {
                    this.selectedConfig = config;
                    if (config.getCategory() != null) {
                        this.selectedCategoryId = config.getCategory().getId();
                    }
                    break;
                }
            }
        }
    }

    public String getGeneratedCurlCommand() {
        return generatedCurlCommand;
    }

    public String getCurlCommandToCopy() {
        return curlCommandToCopy;
    }

    public void setCurlCommandToCopy(String curlCommandToCopy) {
        this.curlCommandToCopy = curlCommandToCopy;
    }

    public List<String> getHttpMethods() {
        return httpMethods;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Long getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Long selectedCategoryId) {
        this.selectedCategoryId = selectedCategoryId;
    }
}