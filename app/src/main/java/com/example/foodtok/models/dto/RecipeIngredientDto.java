package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for the Supabase {@code recipe_ingredients} join table.
 * PostgREST returns the nested {@code ingredients} row as a single object
 * (many-to-one from recipe_ingredients to ingredients).
 */
public class RecipeIngredientDto {

  @SerializedName("quantity")
  public String quantity;

  @SerializedName("is_optional")
  public boolean isOptional;

  @SerializedName("ingredients")
  public IngredientDto ingredient;
}
