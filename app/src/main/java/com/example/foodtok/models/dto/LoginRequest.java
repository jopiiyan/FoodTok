package com.example.foodtok.models.dto;

/** DTO for login request body (email and password). */
public class LoginRequest {

  private final String email;
  private final String password;

  public LoginRequest(String email, String password) {
    this.email = email;
    this.password = password;
  }
}