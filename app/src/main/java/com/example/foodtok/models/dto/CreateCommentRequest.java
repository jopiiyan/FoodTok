package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** POST body for creating a comment via PostgREST. */
public class CreateCommentRequest {

  @SerializedName("user_id")
  public String userId;

  @SerializedName("recipe_id")
  public String recipeId;

  @SerializedName("content")
  public String content;

  public CreateCommentRequest(String userId, String recipeId, String content) {
    this.userId = userId;
    this.recipeId = recipeId;
    this.content = content;
  }
}
