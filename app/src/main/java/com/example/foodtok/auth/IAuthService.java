package com.example.foodtok.auth;

import com.example.foodtok.models.User;

/** Interface contract for authentication services (signup, login, logout). */
public interface IAuthService {

  void signUp(String username, String email, String password,
        AuthCallback callback);

  void login(String email, String password, AuthCallback callback);

  void logout();
}