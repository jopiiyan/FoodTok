package com.example.foodtok.auth;

import com.example.foodtok.models.User;

public class AuthManager {
    private static AuthManager instance;

    private User currentUser;

    private AuthManager(){
        this.currentUser = null;
    }

    //singleton
    public static AuthManager getInstance(){
        if (instance == null){
            instance = new AuthManager();
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }


}
