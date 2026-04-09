package com.example.foodtok.services;

import com.example.foodtok.models.Recipe;

import java.util.List;

/** Async callback for operations that return a list of recipes. */
public interface RecipeListCallback {
  void onSuccess(List<Recipe> recipes);
  void onError(String message);
}
