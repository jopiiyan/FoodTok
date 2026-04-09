package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** DTO for the Supabase {@code interactions} table. */
public class InteractionDto {

  @SerializedName("id")
  public String id;

  @SerializedName("user_id")
  public String userId;

  @SerializedName("recipe_id")
  public String recipeId;

  @SerializedName("type")
  public String type;

  @SerializedName("created_at")
  public String createdAt;
}
