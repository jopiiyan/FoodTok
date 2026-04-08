package com.example.foodtok.auth;

import com.example.foodtok.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory mock implementation of {@link IAuthService} for testing without a backend. */
public class MockAuthService implements IAuthService {

  // Fake database of users (email → User)
  private final Map<String, User> mockUsers = new HashMap<>();
  // Store passwords separately (email → password)
  private final Map<String, String> mockPasswords = new HashMap<>();

  @Override
  public void signUp(String username, String email, String password,
                       AuthCallback callback) {

    // Check if email already exists
    if (mockUsers.containsKey(email)) {
      callback.onError("Email already taken");
      return;
    }

    // Create new user with random ID
    String id = UUID.randomUUID().toString();
    User newUser = new User(id, username, email);

    // Store in our fake database
    mockUsers.put(email, newUser);
    mockPasswords.put(email, password);

    // Success — tell the callback
    callback.onSuccess(newUser);
  }

  @Override
  public void login(String email, String password,
           AuthCallback callback) {

    // Check if email exists
    if (!mockUsers.containsKey(email)) {
      callback.onError("Account not found");
      return;
    }

    // Check if password matches
    String storedPassword = mockPasswords.get(email);
    if (!storedPassword.equals(password)) {
      callback.onError("Incorrect password");
      return;
    }

    // Success — return the user
    User user = mockUsers.get(email);
    callback.onSuccess(user);
  }

  @Override
  public void logout() {
    AuthManager.getInstance().logout();
  }
}