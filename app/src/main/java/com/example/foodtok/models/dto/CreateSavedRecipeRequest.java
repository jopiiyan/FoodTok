package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** POST body for inserting a row into the {@code saved_recipes} table. */
public class CreateSavedRecipeRequest {

  @SerializedName("user_id")
  public String userId;

  @SerializedName("recipe_id")
  public String recipeId;

  public CreateSavedRecipeRequest(String userId, String recipeId) {
    this.userId = userId;
    this.recipeId = recipeId;
  }
}
