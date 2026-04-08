package com.example.foodtok.models.dto;

import java.util.HashMap;
import java.util.Map;

/** DTO for signup request body (email, password, and user metadata). */
public class SignUpRequest {

  private final String email;
  private final String password;
  private final Map<String, String> data;

  public SignUpRequest(String email, String password, String username) {
    this.email = email;
    this.password = password;
    this.data = new HashMap<>();
    this.data.put("username", username);
  }
}