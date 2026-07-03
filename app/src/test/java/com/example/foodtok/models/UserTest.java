package com.example.foodtok.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link User} — the HashMap-backed interest profile and the
 * allergen/ingredient blacklist. Both are pure Java (no Android dependencies).
 */
public class UserTest {

  private User user;

  @Before
  public void setUp() {
    user = new User("u1", "alice", "alice@test.com");
  }

  // ── Interest profile ───────────────────────────────────────────

  @Test
  public void unseenTag_defaultsToZero() {
    assertEquals(0, user.getScoreForTag("italian"));
  }

  @Test
  public void updateInterestScore_incrementsFromZero() {
    user.updateInterestScore("italian", 5);
    assertEquals(5, user.getScoreForTag("italian"));
  }

  @Test
  public void updateInterestScore_accumulatesAcrossCalls() {
    user.updateInterestScore("italian", 5);
    user.updateInterestScore("italian", 10);
    user.updateInterestScore("italian", 15);
    assertEquals(30, user.getScoreForTag("italian"));
  }

  @Test
  public void updateInterestScore_negativePointsDecrement() {
    user.updateInterestScore("pork", 5);
    user.updateInterestScore("pork", -10);   // "not interested"
    assertEquals(-5, user.getScoreForTag("pork"));
  }

  @Test
  public void getTopPreferences_returnsHighestScoredInOrder() {
    user.updateInterestScore("italian", 50);
    user.updateInterestScore("mexican", 30);
    user.updateInterestScore("vegan", 10);
    user.updateInterestScore("dessert", 90);

    List<String> top = user.getTopPreferences(3);
    assertEquals(3, top.size());
    assertEquals("dessert", top.get(0));
    assertEquals("italian", top.get(1));
    assertEquals("mexican", top.get(2));
  }

  @Test
  public void getTopPreferences_limitLargerThanProfile_returnsAll() {
    user.updateInterestScore("italian", 20);
    user.updateInterestScore("mexican", 10);

    List<String> top = user.getTopPreferences(10);
    assertEquals(2, top.size());
  }

  @Test
  public void getTopPreferences_emptyProfile_returnsEmpty() {
    assertTrue(user.getTopPreferences(5).isEmpty());
  }

  @Test
  public void getInterestProfile_isDefensiveCopy() {
    user.updateInterestScore("italian", 40);
    Map<String, Integer> snapshot = user.getInterestProfile();

    // Mutating the returned map must not affect the user's real profile.
    snapshot.put("italian", 9999);
    snapshot.put("injected", 1);

    assertEquals(40, user.getScoreForTag("italian"));
    assertEquals(0, user.getScoreForTag("injected"));
  }

  // ── Allergen / ingredient blacklist ────────────────────────────

  @Test
  public void addBlacklistedIngredient_flagsIt() {
    user.addBlacklistedIngredient("Peanuts");
    assertTrue(user.isIngredientBlacklisted("Peanuts"));
    assertEquals(1, user.getBlacklistedCount());
  }

  @Test
  public void isIngredientBlacklisted_isCaseInsensitive() {
    user.addBlacklistedIngredient("Shellfish");
    assertTrue(user.isIngredientBlacklisted("shellfish"));
    assertTrue(user.isIngredientBlacklisted("SHELLFISH"));
    assertTrue(user.isIngredientBlacklisted("ShElLfIsH"));
  }

  @Test
  public void nonBlacklistedIngredient_returnsFalse() {
    user.addBlacklistedIngredient("Gluten");
    assertFalse(user.isIngredientBlacklisted("Dairy"));
  }

  @Test
  public void duplicateAndCaseVariantAdds_doNotGrowCount() {
    user.addBlacklistedIngredient("Gluten");
    user.addBlacklistedIngredient("gluten");   // same after lower-casing
    user.addBlacklistedIngredient("GLUTEN");
    assertEquals(1, user.getBlacklistedCount());
  }

  @Test
  public void multipleDistinctAllergens_coexist() {
    String[] allergens =
        {"Peanuts", "Gluten", "Shellfish", "Eggs", "Soy"};
    for (String a : allergens) {
      user.addBlacklistedIngredient(a);
    }
    assertEquals(5, user.getBlacklistedCount());
    for (String a : allergens) {
      assertTrue(user.isIngredientBlacklisted(a));
    }
  }

  @Test
  public void removeBlacklistedIngredient_isCaseInsensitive() {
    user.addBlacklistedIngredient("Peanuts");
    user.removeBlacklistedIngredient("PEANUTS");   // different case
    assertFalse(user.isIngredientBlacklisted("Peanuts"));
    assertEquals(0, user.getBlacklistedCount());
  }

  @Test
  public void removeUnknownIngredient_isNoOp() {
    user.addBlacklistedIngredient("Peanuts");
    user.removeBlacklistedIngredient("Dairy");     // not present
    assertEquals(1, user.getBlacklistedCount());
    assertTrue(user.isIngredientBlacklisted("Peanuts"));
  }

  @Test
  public void getBlacklistedIngredients_isDefensiveCopy() {
    user.addBlacklistedIngredient("Peanuts");
    List<String> snapshot = user.getBlacklistedIngredients();

    // Mutating the returned list must not affect the real blacklist.
    snapshot.add("gluten");
    snapshot.clear();

    assertEquals(1, user.getBlacklistedCount());
    assertTrue(user.isIngredientBlacklisted("Peanuts"));
  }

  @Test
  public void getBlacklistedIngredients_storedLowerCased() {
    user.addBlacklistedIngredient("Peanuts");
    assertTrue(user.getBlacklistedIngredients().contains("peanuts"));
  }
}
