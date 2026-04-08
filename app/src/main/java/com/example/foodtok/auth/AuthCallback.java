package com.example.foodtok.auth;

import com.example.foodtok.models.User;

/** Async callback for authentication operations (login, signup, logout). */
public interface AuthCallback {

  void onSuccess(User user);

  void onError(String message);
}