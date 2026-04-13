package com.example.foodtok.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.User;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link RecommendationService} — verifies scoring,
 * filtering, and priority-queue ranking against various interest
 * profile shapes.
 */
public class RecommendationServiceTest {

  private RecommendationService service;

  @Before
  public void setUp() {
    service = new RecommendationService();
  }

  // ── scoreRecipe ────────────────────────────────────────────────

  @Test
  public void scoreRecipe_noTags_returnsZero() {
    Recipe recipe = new Recipe("r1", "Recipe", "url",
        new ArrayList<>(), new ArrayList<>());
    double score = service.scoreRecipe(recipe, new HashMap<>());
    assertEquals(0.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagsNotInProfile_returnsZero() {
    Recipe recipe = recipeWithTags("italian", "pasta");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("mexican", 20);
    double score = service.scoreRecipe(recipe, profile);
    // Both tags default to 0 → total 0
    assertEquals(0.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_sumsAllTagScores() {
    Recipe recipe = recipeWithTags("italian", "pasta");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 20);
    profile.put("pasta", 15);
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(35.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagAboveShowOftenThreshold_getsBoost() {
    Recipe recipe = recipeWithTags("italian");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 60);
    // 60 > 50 → boosted by 1.5x = 90
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(90.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagAtShowOftenThreshold_noBoost() {
    Recipe recipe = recipeWithTags("italian");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 50);
    // 50 is NOT > 50 → no boost
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(50.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagBelowNeverShowThreshold_returnsNegInf() {
    Recipe recipe = recipeWithTags("italian", "pork");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 30);
    profile.put("pork", -30);
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(Double.NEGATIVE_INFINITY, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagAtNeverShowThreshold_notExcluded() {
    Recipe recipe = recipeWithTags("italian");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", -10);
    // -10 is NOT < -10 → included
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(-10.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_mixedBoostAndNormal_sumsCorrectly() {
    Recipe recipe = recipeWithTags("italian", "quick");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 80);   // boosted: 120
    profile.put("quick", 20);     // normal: 20
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(140.0, score, 0.001);
  }

  @Test
  public void scoreRecipe_tagLookupIsCaseInsensitive() {
    Recipe recipe = recipeWithTags("Italian");
    Map<String, Integer> profile = new HashMap<>();
    profile.put("italian", 40);
    double score = service.scoreRecipe(recipe, profile);
    assertEquals(40.0, score, 0.001);
  }

  // ── generateFeed ───────────────────────────────────────────────

  @Test
  public void generateFeed_nullUser_returnsOriginalOrder() {
    List<Recipe> recipes = Arrays.asList(
        recipeWithTags("a"),
        recipeWithTags("b"),
        recipeWithTags("c"));
    List<Recipe> feed = service.generateFeed(recipes, null);
    assertEquals(3, feed.size());
    assertEquals(recipes.get(0).getTags(), feed.get(0).getTags());
  }

  @Test
  public void generateFeed_nullCandidates_returnsEmptyList() {
    User user = new User("u1", "alice", "a@test.com");
    List<Recipe> feed = service.generateFeed(null, user);
    assertTrue(feed.isEmpty());
  }

  @Test
  public void generateFeed_emptyCandidates_returnsEmptyList() {
    User user = new User("u1", "alice", "a@test.com");
    List<Recipe> feed =
        service.generateFeed(new ArrayList<>(), user);
    assertTrue(feed.isEmpty());
  }

  @Test
  public void generateFeed_emptyProfile_returnsAllRecipes() {
    User user = new User("u1", "alice", "a@test.com");
    List<Recipe> recipes = Arrays.asList(
        recipeWithTags("a"),
        recipeWithTags("b"),
        recipeWithTags("c"));
    List<Recipe> feed = service.generateFeed(recipes, user);
    // Empty profile → shuffled, but all recipes still present
    assertEquals(3, feed.size());
  }

  @Test
  public void generateFeed_rankedByScore_highestFirst() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("italian", 60);  // boost: 90
    user.updateInterestScore("quick", 20);
    user.updateInterestScore("vegan", 5);

    Recipe italian = recipeWithId("it", "italian");
    Recipe quick = recipeWithId("qk", "quick");
    Recipe vegan = recipeWithId("vg", "vegan");

    List<Recipe> feed = service.generateFeed(
        Arrays.asList(vegan, italian, quick), user);

    assertEquals(3, feed.size());
    assertEquals("it", feed.get(0).getId());
    assertEquals("qk", feed.get(1).getId());
    assertEquals("vg", feed.get(2).getId());
  }

  @Test
  public void generateFeed_excludesRecipesWithNeverShowTags() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("italian", 40);
    user.updateInterestScore("pork", -30);  // never show

    Recipe safe = recipeWithId("safe", "italian");
    Recipe blocked = recipeWithId("blk", "italian", "pork");

    List<Recipe> feed = service.generateFeed(
        Arrays.asList(safe, blocked), user);

    assertEquals(1, feed.size());
    assertEquals("safe", feed.get(0).getId());
  }

  @Test
  public void generateFeed_allBlocked_returnsEmptyList() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("pork", -30);

    Recipe r1 = recipeWithId("r1", "pork");
    Recipe r2 = recipeWithId("r2", "pork");

    List<Recipe> feed = service.generateFeed(
        Arrays.asList(r1, r2), user);
    assertTrue(feed.isEmpty());
  }

  @Test
  public void generateFeed_preservesAllNonBlockedRecipes() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("italian", 20);
    user.updateInterestScore("mexican", 15);

    List<Recipe> candidates = Arrays.asList(
        recipeWithId("a", "italian"),
        recipeWithId("b", "mexican"),
        recipeWithId("c", "italian"),
        recipeWithId("d", "mexican"),
        recipeWithId("e", "italian"));

    List<Recipe> feed = service.generateFeed(candidates, user);
    assertEquals(5, feed.size());
  }

  @Test
  public void generateFeed_allItalianBeforeAllMexican() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("italian", 30);
    user.updateInterestScore("mexican", 10);

    List<Recipe> candidates = Arrays.asList(
        recipeWithId("m1", "mexican"),
        recipeWithId("i1", "italian"),
        recipeWithId("m2", "mexican"),
        recipeWithId("i2", "italian"));

    List<Recipe> feed = service.generateFeed(candidates, user);
    // Italians (score 30) should precede Mexicans (score 10)
    assertTrue(feed.get(0).getId().startsWith("i"));
    assertTrue(feed.get(1).getId().startsWith("i"));
    assertTrue(feed.get(2).getId().startsWith("m"));
    assertTrue(feed.get(3).getId().startsWith("m"));
  }

  @Test
  public void likePoints_increaseTagScores_viaUser() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("italian", 0);
    // Simulate a like: +LIKE_POINTS
    user.updateInterestScore("italian",
        RecommendationService.LIKE_POINTS);
    assertEquals(5, user.getScoreForTag("italian"));
  }

  @Test
  public void notInterestedPoints_decreaseTagScores_viaUser() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("pork", 0);
    user.updateInterestScore("pork",
        RecommendationService.NOT_INTERESTED_POINTS);
    assertEquals(-10, user.getScoreForTag("pork"));
  }

  @Test
  public void onboardingPoints_setsInitialPreference() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("vegan",
        RecommendationService.ONBOARDING_POINTS);
    assertEquals(30, user.getScoreForTag("vegan"));
  }

  @Test
  public void allergenPenalty_excludesRecipe() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("peanut",
        RecommendationService.ALLERGEN_PENALTY);

    Recipe r = recipeWithId("r1", "peanut");
    List<Recipe> feed = service.generateFeed(
        Arrays.asList(r), user);
    // -30 < -10 → excluded
    assertTrue(feed.isEmpty());
  }

  @Test
  public void repeatedLikes_accumulatePoints() {
    User user = new User("u1", "alice", "a@test.com");
    for (int i = 0; i < 10; i++) {
      user.updateInterestScore("italian",
          RecommendationService.LIKE_POINTS);
    }
    // 10 likes * 5 = 50 points (boundary: NOT > 50)
    assertEquals(50, user.getScoreForTag("italian"));

    // One more like crosses the threshold
    user.updateInterestScore("italian",
        RecommendationService.LIKE_POINTS);
    assertEquals(55, user.getScoreForTag("italian"));

    // Now the recipe should be boosted
    Recipe r = recipeWithId("r1", "italian");
    double score = service.scoreRecipe(r, user.getInterestProfile());
    assertEquals(82.5, score, 0.001);  // 55 * 1.5
  }

  @Test
  public void feedRankingAfterLikeAndNotInterested() {
    User user = new User("u1", "alice", "a@test.com");
    // Preference: liked italian, not interested in vegan
    user.updateInterestScore("italian", 40);
    user.updateInterestScore("vegan", -5);

    Recipe ita = recipeWithId("ita", "italian");
    Recipe veg = recipeWithId("veg", "vegan");
    Recipe neu = recipeWithId("neu", "mexican");

    List<Recipe> feed = service.generateFeed(
        Arrays.asList(veg, neu, ita), user);

    assertEquals("ita", feed.get(0).getId());
    // "neu" has score 0, "veg" has score -5
    assertEquals("neu", feed.get(1).getId());
    assertEquals("veg", feed.get(2).getId());
  }

  @Test
  public void notBlockedButLowScore_stillIncluded() {
    User user = new User("u1", "alice", "a@test.com");
    user.updateInterestScore("bland", -9);

    Recipe r = recipeWithId("r1", "bland");
    List<Recipe> feed = service.generateFeed(
        Arrays.asList(r), user);
    // -9 is NOT < -10 → still shown
    assertEquals(1, feed.size());
  }

  // ── Helpers ────────────────────────────────────────────────────

  private Recipe recipeWithTags(String... tags) {
    return recipeWithId("r_" + tags[0], tags);
  }

  private Recipe recipeWithId(String id, String... tags) {
    List<String> tagList = new ArrayList<>(Arrays.asList(tags));
    return new Recipe(id, "Recipe " + id,
        "http://example.com/" + id + ".mp4",
        tagList, new ArrayList<>());
  }
}
