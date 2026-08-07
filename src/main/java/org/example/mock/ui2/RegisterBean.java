package org.example.mock.ui2;

import org.example.mock.entity.AppUser;
import org.example.mock.service.AuthService;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;

@Named
@ViewScoped
public class RegisterBean implements Serializable {

    @Inject
    private AuthService authService;

    private String username;
    private String password;
    private String fullName;
    private String email;

    public void register() {
        try {
            AppUser user = authService.register(username, password, fullName, email);

            if (user != null) {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                
                context.addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Registration Successful", 
                                "Your account has been created. Please log in."));
                
                // Redirect to login page
                context.getExternalContext().redirect(request.getContextPath() + "/login.xhtml");
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration Failed", 
                                "Username already exists. Please choose another one."));
            }
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unable to redirect."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Registration Failed", e.getMessage()));
        }
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}