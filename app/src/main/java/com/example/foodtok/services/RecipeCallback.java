package com.example.foodtok.services;

import com.example.foodtok.models.Recipe;

/** Async callback for operations that return a single recipe. */
public interface RecipeCallback {
  void onSuccess(Recipe recipe);
  void onError(String message);
}
