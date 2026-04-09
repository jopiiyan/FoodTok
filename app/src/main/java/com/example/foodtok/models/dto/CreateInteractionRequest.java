package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** POST body for creating an interaction (like/save/not_interested) via PostgREST. */
public class CreateInteractionRequest {

  @SerializedName("user_id")
  public String userId;

  @SerializedName("recipe_id")
  public String recipeId;

  @SerializedName("type")
  public String type;

  public CreateInteractionRequest(String userId, String recipeId, String type) {
    this.userId = userId;
    this.recipeId = recipeId;
    this.type = type;
  }
}
