package org.example.mock.service;

import org.example.mock.entity.AppUser;
import org.mindrot.jbcrypt.BCrypt;

import javax.ejb.Stateless;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;


@Stateless
public class AuthService implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    @PersistenceContext(unitName = "mock-pu")
    private EntityManager em;

    private AppUser currentUser;

    public AppUser authenticate(String username, String password) {
        try {
            AppUser user = em.createNamedQuery("AppUser.findByUsername", AppUser.class)
                    .setParameter("username", username)
                    .getSingleResult();

            if (user != null && user.isEnabled() && BCrypt.checkpw(password, user.getPasswordHash())) {
                this.currentUser = user;
                return user;
            }
            return null;
        } catch (NoResultException e) {
            LOGGER.log(Level.WARNING, "User not found: {0}", username);
            return null;
        }
    }

    public AppUser register(String username, String password, String fullName, String email) {
        try {
            em.createNamedQuery("AppUser.findByUsername", AppUser.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return null;
        } catch (NoResultException e) {
            AppUser user = new AppUser();
            user.setUsername(username);
            user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            user.setFullName(fullName);
            user.setEmail(email);
            user.setEnabled(true);
            user.setCreatedAt(Timestamp.from(Instant.now()));

            em.persist(user);
            return user;
        }
    }

    public void setCurrentUser(AppUser user) {
        this.currentUser = user;
    }

    public AppUser getCurrentUser() {
        return currentUser;
    }

    public void clearCurrentUser() {
        this.currentUser = null;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public AppUser findById(Long id) {
        return em.find(AppUser.class, id);
    }
}