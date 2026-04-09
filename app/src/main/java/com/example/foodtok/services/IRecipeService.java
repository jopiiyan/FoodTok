package com.example.foodtok.services;

/** Interface contract for fetching and creating recipes. */
public interface IRecipeService {

  /**
   * Fetches a page of recipes for the feed.
   *
   * @param page     zero-based page index
   * @param pageSize number of recipes per page
   * @param callback async result callback
   */
  void getFeedRecipes(int page, int pageSize, RecipeListCallback callback);

  /**
   * Fetches a single recipe by its ID.
   *
   * @param recipeId the recipe UUID
   * @param callback async result callback
   */
  void getRecipeById(String recipeId, RecipeCallback callback);
}
