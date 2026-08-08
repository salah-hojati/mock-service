package org.example.mock.ui2;

import org.example.mock.entity.Category;
import org.example.mock.entity.MockConfig;
import org.example.mock.entity.MockConfig2;
import org.example.mock.service.CategoryService;
import org.example.mock.service.MockConfigServiceByUrl;
import org.example.mock.service.MockConfigServiceByUrlNew;
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

@Named
@ViewScoped
public class MockConfigBeanByUrlNew implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(MockConfigBeanByUrlNew.class.getName());

    // This could be a static final constant for a minor performance gain.
    private static final List<String> HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE");

    @Inject
    private MockConfigServiceByUrlNew mockConfigService;

    @Inject
    private CategoryService categoryService;

    private List<MockConfig2> configs = new ArrayList<>();
    private MockConfig2 selectedConfig;
    private String generatedCurlCommand;
    private String curlCommandToCopy;
    private List<Category> categories = new ArrayList<>();
    private Long selectedCategoryId;



    @PostConstruct
    public void init() {
        try {
            loadCategories();
            loadConfigs();
            selectedConfig = new MockConfig2();
            sampleConfig(selectedConfig);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations for Bean2", e);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error Loading Data", "Could not retrieve configurations."));
        }
    }
    private void sampleConfig(MockConfig2 selectedConfig) {
        selectedConfig = new MockConfig2();
        selectedConfig.setHttpMethod("POST");
        selectedConfig.setHttpStatusCode(200);
        selectedConfig.setDelayMs(0);
    }
    private void loadCategories() {
        try {
            categories = categoryService.findAllForCurrentUser();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load categories for Bean2", e);
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
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock configurations for Bean2", e);
            configs = new ArrayList<>();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error Loading Data", "Could not retrieve configurations."));
        }
    }

    public void filterByCategory() {
        loadConfigs();
    }
    public boolean isRequestBodyApplicable() {
        if (selectedConfig == null) {
            selectedConfig=new MockConfig2();
            sampleConfig(selectedConfig);
        }
        if (selectedConfig == null || selectedConfig.getHttpMethod() == null) {
            return false;
        }
        String method = selectedConfig.getHttpMethod().toUpperCase();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
    public void openNew() {
        this.selectedConfig = new MockConfig2();
        sampleConfig(selectedConfig);
        this.selectedCategoryId = null;

        PrimeFaces.current().resetInputs("form2:manage-config-content");
    }

    public void saveConfig() {
        try {
            this.selectedConfig.setCapturedRequestPayload(JsonUtil.normalize(this.selectedConfig.getCapturedRequestPayload()));
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

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Configuration Saved",
                            "The mock configuration was saved successfully."));

            PrimeFaces.current().ajax().addCallbackParam("validationFailed", false);
            PrimeFaces.current().executeScript("PF('manageMock2Dialog').hide()");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving configuration", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save Error", e.getMessage()));
            PrimeFaces.current().ajax().addCallbackParam("validationFailed", true);
        }
    }

    public void deleteConfig() {
        try {
            mockConfigService.delete(this.selectedConfig);
            loadConfigs();
            this.selectedConfig = null;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Configuration Deleted",
                            "The configuration was removed successfully."));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete configuration", e);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Delete Error",
                            "Could not delete the selected configuration."));
        }
    }

    public void generateCurl(MockConfig2 selectedConfig) {
        this.selectedConfig = mockConfigService.findById(selectedConfig.getId());
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        String baseUrl = String.format("%s://%s:%d%s",
                request.getScheme(),
                request.getServerName(),
                request.getServerPort(),
                request.getContextPath());

        String fullUrl = String.format("%s/api/mock2/%s", baseUrl, this.selectedConfig.getUrlPattern());
        String httpMethod = this.selectedConfig.getHttpMethod().toUpperCase();

        StringBuilder curl = new StringBuilder("curl -i -X ");
        curl.append(httpMethod).append(" ");
        curl.append("'").append(fullUrl).append("'");

        boolean hasBody = "POST".equals(httpMethod) || "PUT".equals(httpMethod);

        if (hasBody) {
            curl.append(" \\\n");
            curl.append("-H 'Content-Type: application/json'");

            if (this.selectedConfig.getResponsePayload() != null && !this.selectedConfig.getResponsePayload().isEmpty()) {
                String escapedPayload = this.selectedConfig.getResponsePayload().replace("'", "'\\''");
                curl.append(" \\\n");
                curl.append("-d '").append(escapedPayload).append("'");
            }
        }

        this.curlCommandToCopy = curl.toString();
        this.generatedCurlCommand = curl.toString();
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

    public void filterByCategoryWithParam() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> params = context.getExternalContext().getRequestParameterMap();
        String categoryIdStr = params.get("indexForm:mainTabView:categorySelectUrl_input");

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

    // Getters and Setters
    public List<MockConfig2> getConfigs() { return configs; }
    public MockConfig2 getSelectedConfig() {
        return selectedConfig; }
    public void setSelectedConfig(MockConfig2 selectedConfig) {
        this.selectedConfig = selectedConfig;
        if (selectedConfig != null && selectedConfig.getCategory() != null) {
            this.selectedCategoryId = selectedConfig.getCategory().getId();
        }
    }
    public String getGeneratedCurlCommand() { return generatedCurlCommand; }
    public String getCurlCommandToCopy() { return curlCommandToCopy; }
    public void setCurlCommandToCopy(String curlCommandToCopy) { this.curlCommandToCopy = curlCommandToCopy; }
    public List<String> getHttpMethods() { return HTTP_METHODS; }

    public List<Category> getCategories() { return categories; }
    public Long getSelectedCategoryId() { return selectedCategoryId; }
    public void setSelectedCategoryId(Long selectedCategoryId) { this.selectedCategoryId = selectedCategoryId; }



}