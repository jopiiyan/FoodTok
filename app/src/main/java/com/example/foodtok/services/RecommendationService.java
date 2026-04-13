package com.example.foodtok.services;

import android.util.Log;

import com.example.foodtok.data.RecipePriorityQueue;
import com.example.foodtok.data.RecipePriorityQueue.ScoredRecipe;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Scores and ranks recipes based on the user's interest profile
 * using a custom {@link RecipePriorityQueue} (max-heap).
 *
 * <h3>Point-based scoring</h3>
 * <ul>
 *   <li>Each recipe tag is looked up in the user's interest profile
 *       HashMap.</li>
 *   <li>Tag score &lt; -10 → recipe is excluded ("never show").</li>
 *   <li>Tag score -10 to 50 → contributes raw score ("show
 *       sometimes").</li>
 *   <li>Tag score &gt; 50 → contributes 1.5x score ("show
 *       often").</li>
 * </ul>
 *
 * <h3>Point assignment (handled externally)</h3>
 * <ul>
 *   <li>Onboarding preference: +30</li>
 *   <li>Like: +5 to all tags</li>
 *   <li>Not Interested: -10 to all tags</li>
 *   <li>Allergen/blacklisted tags: initially -30</li>
 * </ul>
 */
public class RecommendationService {

  private static final String TAG = "RecommendationService";

  /** Flip to false to silence verbose recommendation logs. */
  private static final boolean DEBUG = true;

  /** Tags below this threshold cause the recipe to be excluded. */
  private static final int NEVER_SHOW_THRESHOLD = -10;

  /** Tags above this threshold get a score boost. */
  private static final int SHOW_OFTEN_THRESHOLD = 50;

  /** Multiplier for tags in the "show often" tier. */
  private static final double BOOST_MULTIPLIER = 1.5;

  /** Points added per tag when the user likes a recipe. */
  public static final int LIKE_POINTS = 5;

  /** Points added per tag when the user saves a recipe (stronger signal than a like). */
  public static final int SAVE_POINTS = 10;

  /** Points deducted per tag when the user marks not interested. */
  public static final int NOT_INTERESTED_POINTS = -10;

  /** Points added per tag during onboarding preference selection. */
  public static final int ONBOARDING_POINTS = 30;

  /** Initial penalty for allergen/blacklisted tags. */
  public static final int ALLERGEN_PENALTY = -30;

  /**
   * Generates a ranked feed by scoring each recipe against the
   * user's interest profile and extracting the top entries from a
   * max-heap.
   *
   * <p>Guest users (null user) receive the recipes in their original
   * order (no personalization).
   *
   * @param candidates all available recipes
   * @param user       the current user, or {@code null} for guests
   * @return recipes ordered by recommendation score (highest first)
   */
  public List<Recipe> generateFeed(List<Recipe> candidates,
      User user) {
    if (user == null || candidates == null
        || candidates.isEmpty()) {
      if (DEBUG) {
        Log.d(TAG, "generateFeed: guest/empty — skipping personalization "
            + "(user=" + (user == null ? "null" : user.getUsername())
            + ", candidates="
            + (candidates == null ? "null" : candidates.size()) + ")");
      }
      return candidates != null
          ? new ArrayList<>(candidates)
          : new ArrayList<>();
    }

    Map<String, Integer> profile = user.getInterestProfile();

    if (DEBUG) {
      Log.d(TAG, "═══ generateFeed for " + user.getUsername()
          + " (" + candidates.size() + " candidates) ═══");
      Log.d(TAG, "Interest profile: " + profile);
    }

    // If the user has no interest profile yet, shuffle for variety.
    if (profile.isEmpty()) {
      if (DEBUG) {
        Log.d(TAG, "Empty profile → shuffling for variety");
      }
      List<Recipe> shuffled = new ArrayList<>(candidates);
      Collections.shuffle(shuffled);
      return shuffled;
    }

    RecipePriorityQueue pq = new RecipePriorityQueue();
    int excluded = 0;

    for (Recipe recipe : candidates) {
      double score = scoreRecipe(recipe, profile);
      if (score > Double.NEGATIVE_INFINITY) {
        pq.insert(new ScoredRecipe(recipe, score));
      } else {
        excluded++;
        if (DEBUG) {
          Log.d(TAG, "  ✗ EXCLUDED \"" + recipe.getTitle()
              + "\" tags=" + recipe.getTags());
        }
      }
    }

    List<Recipe> ranked = new ArrayList<>();
    List<ScoredRecipe> scored = pq.pollTop(pq.size());
    for (ScoredRecipe sr : scored) {
      ranked.add(sr.getRecipe());
    }

    if (DEBUG) {
      Log.d(TAG, "Result: " + ranked.size() + " ranked, "
          + excluded + " excluded");
      for (int i = 0; i < scored.size(); i++) {
        ScoredRecipe sr = scored.get(i);
        Log.d(TAG, String.format("  #%d  score=%.1f  \"%s\"  tags=%s",
            i + 1, sr.getScore(), sr.getRecipe().getTitle(),
            sr.getRecipe().getTags()));
      }
      Log.d(TAG, "═══════════════════════════════════════");
    }
    return ranked;
  }

  /**
   * Computes a recommendation score for a single recipe.
   *
   * @param recipe  the recipe to score
   * @param profile the user's tag → score map
   * @return the score, or {@link Double#NEGATIVE_INFINITY} if the
   *         recipe should be excluded
   */
  public double scoreRecipe(Recipe recipe,
      Map<String, Integer> profile) {
    List<String> tags = recipe.getTags();

    // Recipes with no tags get a neutral score so they still appear.
    if (tags == null || tags.isEmpty()) {
      return 0;
    }

    double total = 0;
    for (String tag : tags) {
      if (tag == null) {
        continue;
      }
      String key = tag.toLowerCase();
      int tagScore = profile.getOrDefault(key, 0);

      // If ANY tag is below the "never show" threshold, exclude
      // the entire recipe.
      if (tagScore < NEVER_SHOW_THRESHOLD) {
        return Double.NEGATIVE_INFINITY;
      }

      // Boost tags in the "show often" tier.
      if (tagScore > SHOW_OFTEN_THRESHOLD) {
        total += tagScore * BOOST_MULTIPLIER;
      } else {
        total += tagScore;
      }
    }

    return total;
  }
}
