package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** POST body for creating a recipe via PostgREST. */
public class UploadRecipeRequest {

  @SerializedName("author_id")
  public String authorId;

  @SerializedName("title")
  public String title;

  @SerializedName("description")
  public String description;

  @SerializedName("video_url")
  public String videoUrl;

  @SerializedName("thumbnail_url")
  public String thumbnailUrl;

  @SerializedName("tags")
  public String[] tags;

  @SerializedName("prep_time_minutes")
  public int prepTimeMinutes;

  @SerializedName("cook_time_minutes")
  public int cookTimeMinutes;

  @SerializedName("estimated_calories")
  public double estimatedCalories;
}
