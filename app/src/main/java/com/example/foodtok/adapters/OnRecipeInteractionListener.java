package com.example.foodtok.adapters;

import com.example.foodtok.models.Recipe;

/** Callback interface for recipe interaction events (like, comment, save). */
public interface OnRecipeInteractionListener {
  void onLikeClicked(Recipe recipe);
  void onCommentClicked(Recipe recipe);
  void onSaveClicked(Recipe recipe);
}