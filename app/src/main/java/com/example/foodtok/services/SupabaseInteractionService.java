package com.example.foodtok.services;

import com.example.foodtok.models.dto.CreateInteractionRequest;
import com.example.foodtok.models.dto.CreateSavedRecipeRequest;
import com.example.foodtok.models.dto.InteractionDto;
import com.example.foodtok.models.dto.SavedRecipeDto;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IInteractionService} implementation backed by Supabase PostgREST. */
public class SupabaseInteractionService implements IInteractionService {

  private final SupabaseApi api;

  public SupabaseInteractionService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
  }

  @Override
  public void likeRecipe(String recipeId, InteractionCallback callback) {
    toggleInteraction(recipeId, "like", callback);
  }

  @Override
  public void saveRecipe(String recipeId, InteractionCallback callback) {
    toggleInteraction(recipeId, "save", callback);
  }

  @Override
  public void addComment(String recipeId, String text,
      InteractionCallback callback) {
    // Comment creation is handled by ICommentService.
    // This method exists for interface compatibility.
    callback.onSuccess();
  }

  @Override
  public void isRecipeLiked(String recipeId, BooleanCallback callback) {
    checkInteractionExists(recipeId, "like", callback);
  }

  @Override
  public void isRecipeSaved(String recipeId, BooleanCallback callback) {
    checkInteractionExists(recipeId, "save", callback);
  }

  @Override
  public void markNotInterested(String recipeId,
      InteractionCallback callback) {
    toggleInteraction(recipeId, "not_interested", callback);
  }

  @Override
  public void isRecipeNotInterested(String recipeId,
      BooleanCallback callback) {
    checkInteractionExists(recipeId, "not_interested", callback);
  }

  /**
   * Toggles an interaction: deletes if it exists, creates if it doesn't.
   * Mirrors the toggle behavior of {@link MockInteractionService}.
   */
  private void toggleInteraction(String recipeId, String type,
      InteractionCallback callback) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      callback.onError("Please log in first");
      return;
    }

    String userFilter = "eq." + userId;
    String recipeFilter = "eq." + recipeId;
    String typeFilter = "eq." + type;

    // First check if the interaction already exists
    api.getInteractions(userFilter, recipeFilter, typeFilter)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              // Exists — delete it (un-like / un-save)
              deleteInteraction(userFilter, recipeFilter, typeFilter,
                  callback);
            } else {
              // Doesn't exist — create it
              createInteraction(userId, recipeId, type, callback);
            }
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  private void createInteraction(String userId, String recipeId,
      String type, InteractionCallback callback) {
    CreateInteractionRequest request =
        new CreateInteractionRequest(userId, recipeId, type);

    api.createInteraction(request)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            if (response.isSuccessful()) {
              if ("save".equals(type)) {
                syncSavedRecipeInsert(userId, recipeId);
              }
              callback.onSuccess();
            } else {
              callback.onError("Failed to save interaction: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  private void deleteInteraction(String userFilter, String recipeFilter,
      String typeFilter, InteractionCallback callback) {
    api.deleteInteraction(userFilter, recipeFilter, typeFilter)
        .enqueue(new Callback<Void>() {
          @Override
          public void onResponse(Call<Void> call,
              Response<Void> response) {
            if (response.isSuccessful()) {
              if ("eq.save".equals(typeFilter)) {
                syncSavedRecipeDelete(userFilter, recipeFilter);
              }
              callback.onSuccess();
            } else {
              callback.onError("Failed to remove interaction: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<Void> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  /**
   * Inserts a matching row into {@code saved_recipes} so the profile
   * Saved tab stays in sync with the {@code interactions} table.
   */
  private void syncSavedRecipeInsert(String userId, String recipeId) {
    api.createSavedRecipe(new CreateSavedRecipeRequest(userId, recipeId))
        .enqueue(new Callback<List<SavedRecipeDto>>() {
          @Override
          public void onResponse(
              Call<List<SavedRecipeDto>> call,
              Response<List<SavedRecipeDto>> resp) {
            // Best-effort sync — no action on failure.
          }

          @Override
          public void onFailure(
              Call<List<SavedRecipeDto>> call,
              Throwable t) {
            // Best-effort sync.
          }
        });
  }

  /**
   * Deletes the matching row from {@code saved_recipes} so the profile
   * Saved tab stays in sync with the {@code interactions} table.
   */
  private void syncSavedRecipeDelete(String userFilter,
      String recipeFilter) {
    api.deleteSavedRecipe(userFilter, recipeFilter)
        .enqueue(new Callback<Void>() {
          @Override
          public void onResponse(Call<Void> call,
              Response<Void> resp) {
            // Best-effort sync.
          }

          @Override
          public void onFailure(Call<Void> call, Throwable t) {
            // Best-effort sync.
          }
        });
  }

  private void checkInteractionExists(String recipeId, String type,
      BooleanCallback callback) {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      callback.onResult(false);
      return;
    }

    api.getInteractions("eq." + userId, "eq." + recipeId, "eq." + type)
        .enqueue(new Callback<List<InteractionDto>>() {
          @Override
          public void onResponse(Call<List<InteractionDto>> call,
              Response<List<InteractionDto>> response) {
            boolean exists = response.isSuccessful()
                && response.body() != null
                && !response.body().isEmpty();
            callback.onResult(exists);
          }

          @Override
          public void onFailure(Call<List<InteractionDto>> call,
              Throwable t) {
            callback.onResult(false);
          }
        });
  }
}
