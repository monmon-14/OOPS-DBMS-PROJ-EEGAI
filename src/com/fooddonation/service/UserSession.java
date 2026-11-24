package com.fooddonation.service;

import com.fooddonation.model.User;

/**
 * Thread-safe Singleton Session Manager.
 * Retains the authenticated User state across screens and workflows.
 */
public class UserSession {
    private static volatile UserSession instance;
    private User currentUser;
    private long loginTimestamp;

    private UserSession() {
    }

    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    /**
     * Stores the logged-in User into the active session.
     */
    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
        this.loginTimestamp = System.currentTimeMillis();
    }

    /**
     * Retrieves the current logged-in User.
     */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks if a user is currently authenticated.
     */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Terminates the current session and clears user credentials.
     */
    public synchronized void logout() {
        this.currentUser = null;
        this.loginTimestamp = 0;
    }

    public synchronized long getLoginTimestamp() {
        return loginTimestamp;
    }
}
