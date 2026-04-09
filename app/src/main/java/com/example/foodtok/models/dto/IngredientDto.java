package com.example.foodtok.models.dto;

import com.example.foodtok.models.Ingredient;
import com.google.gson.annotations.SerializedName;

/** DTO for the Supabase {@code ingredients} table. */
public class IngredientDto {

  @SerializedName("id")
  public String id;

  @SerializedName("name")
  public String name;

  @SerializedName("calories_per_100g")
  public double caloriesPer100g;

  @SerializedName("is_common_allergen")
  public boolean isCommonAllergen;

  @SerializedName("category")
  public String category;

  /** Converts this DTO to the domain {@link Ingredient} model. */
  public Ingredient toDomain() {
    return new Ingredient(name, caloriesPer100g);
  }
}
