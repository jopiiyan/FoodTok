package com.example.foodtok.services;

import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IRecipeService} implementation backed by Supabase PostgREST. */
public class SupabaseRecipeService implements IRecipeService {

  private static final String RECIPE_SELECT =
      "*,recipe_ingredients(quantity,is_optional,"
      + "ingredients(id,name,calories_per_100g)),"
      + "users!author_id(username,display_name,avatar_url)";

  private final SupabaseApi api;

  public SupabaseRecipeService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
  }

  @Override
  public void getFeedRecipes(int page, int pageSize,
      RecipeListCallback callback) {
    int from = page * pageSize;
    int to = from + pageSize - 1;
    String range = from + "-" + to;

    api.getRecipes(RECIPE_SELECT, "created_at.desc", range)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Recipe> recipes = new ArrayList<>();
              for (RecipeDto dto : response.body()) {
                recipes.add(dto.toDomain());
              }
              callback.onSuccess(recipes);
            } else {
              callback.onError("Failed to load recipes: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void getRecipeById(String recipeId, RecipeCallback callback) {
    api.getRecipeById("eq." + recipeId, RECIPE_SELECT)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(response.body().get(0).toDomain());
            } else {
              callback.onError("Recipe not found");
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }
}
