package com.example.foodtok.services;

/** Factory singleton that provides the active {@link IRecipeService} implementation. */
public class RecipeServiceProvider {

  private static IRecipeService recipeService;

  private RecipeServiceProvider() {
  }

  /** Returns the current recipe service (defaults to mock if none set). */
  public static IRecipeService getRecipeService() {
    if (recipeService == null) {
      recipeService = new MockRecipeService();
    }
    return recipeService;
  }

  /** Swaps in a different implementation (e.g. real Supabase service). */
  public static void setRecipeService(IRecipeService service) {
    recipeService = service;
  }
}
