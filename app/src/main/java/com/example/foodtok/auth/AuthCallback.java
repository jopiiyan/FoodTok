package com.example.foodtok.auth;

import com.example.foodtok.models.User;

public interface AuthCallback {

    void onSuccess(User user);

    void onError(String message);
}