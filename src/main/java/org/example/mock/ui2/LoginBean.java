package org.example.mock.ui2;

import org.example.mock.entity.AppUser;
import org.example.mock.service.AuthService;


import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;

import javax.faces.context.FacesContext;
;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;

@Named
@SessionScoped
public class LoginBean implements Serializable {

    @Inject
    private AuthService authService;

    private String username;
    private String password;
    private AppUser user;

    public void login() {
        try {
             user = authService.authenticate(username, password);

            if (user != null) {
                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", user.getId());

                context.getExternalContext().redirect(request.getContextPath() + "/ui2/index.xhtml");
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid credentials",
                                "Username or password is incorrect."));
            }
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error",
                            "Unable to redirect after login."));
        }
    }

    public void logout() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            authService.clearCurrentUser();
            context.getExternalContext().redirect(request.getContextPath() + "/ui2/login.xhtml");
        } catch (IOException e) {
            // Handle exception
        }
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public AppUser getUser() { return user; }
}