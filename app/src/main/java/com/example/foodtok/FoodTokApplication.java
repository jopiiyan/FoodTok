package com.example.foodtok;

import android.app.Application;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.auth.AuthServiceProvider;
import com.example.foodtok.auth.SupabaseAuthService;
import com.example.foodtok.models.User;
import com.example.foodtok.services.CommentServiceProvider;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.RecipeServiceProvider;
import com.example.foodtok.services.SupabaseCommentService;
import com.example.foodtok.services.SupabaseInteractionService;
import com.example.foodtok.services.SupabaseRecipeService;
import com.example.foodtok.util.SessionManager;

/** Custom Application entry point. Initializes session management and service providers. */
public class FoodTokApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    // 1. Initialize SessionManager (needs Context, so must be first)
    SessionManager.init(this);

    // 2. Set real auth service (replaces MockAuthService)
    AuthServiceProvider.setAuthService(new SupabaseAuthService());

    // 3. Set real Supabase services (replaces mock implementations)
    RecipeServiceProvider.setRecipeService(new SupabaseRecipeService());
    CommentServiceProvider.setCommentService(new SupabaseCommentService());
    InteractionServiceProvider.setInteractionService(
        new SupabaseInteractionService());

    // 4. Restore login state if user was previously logged in
    restoreSession();
  }

  private void restoreSession() {
    SessionManager session = SessionManager.getInstance();

    if (session.isLoggedIn()) {
      // User was logged in before app closed
      // Create a basic User object from saved ID
      // Full profile can be fetched later from Supabase
      String userId = session.getUserId();
      String username = session.getUsername();
      User user = new User(userId, username, "");
      AuthManager.getInstance().login(user);
    }
  }
}