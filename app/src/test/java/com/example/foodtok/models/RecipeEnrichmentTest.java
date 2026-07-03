package com.example.foodtok.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link RecipeEnrichment} — the AI-detected allergen layer.
 * Pure Java, no Android dependencies.
 */
public class RecipeEnrichmentTest {

  private RecipeEnrichment enrichmentWithAllergens(String... allergens) {
    return new RecipeEnrichment(
        "r1",
        new ArrayList<>(Arrays.asList(allergens)),
        new ArrayList<>(),
        250.0,
        new ArrayList<>());
  }

  @Test
  public void hasAllergenWarnings_trueWhenAllergensPresent() {
    RecipeEnrichment e = enrichmentWithAllergens("Peanuts");
    assertTrue(e.hasAllergenWarnings());
  }

  @Test
  public void hasAllergenWarnings_falseWhenEmpty() {
    RecipeEnrichment e = enrichmentWithAllergens();
    assertFalse(e.hasAllergenWarnings());
  }

  @Test
  public void isIngredientFlaggedByAI_matchesCaseInsensitively() {
    RecipeEnrichment e = enrichmentWithAllergens("Peanuts", "Shellfish");
    assertTrue(e.isIngredientFlaggedByAI("peanuts"));
    assertTrue(e.isIngredientFlaggedByAI("SHELLFISH"));
    assertTrue(e.isIngredientFlaggedByAI("Peanuts"));
  }

  @Test
  public void isIngredientFlaggedByAI_falseForUnflagged() {
    RecipeEnrichment e = enrichmentWithAllergens("Peanuts");
    assertFalse(e.isIngredientFlaggedByAI("Gluten"));
  }

  @Test
  public void isIngredientFlaggedByAI_nullSafe() {
    RecipeEnrichment e = enrichmentWithAllergens("Peanuts");
    assertFalse(e.isIngredientFlaggedByAI(null));
  }

  @Test
  public void multipleAllergensOnOneRecipe_allDetected() {
    RecipeEnrichment e =
        enrichmentWithAllergens("Peanuts", "Gluten", "Shellfish", "Eggs");
    List<String> detected = e.getDetectedAllergens();
    assertEquals(4, detected.size());
    assertTrue(e.isIngredientFlaggedByAI("gluten"));
    assertTrue(e.isIngredientFlaggedByAI("eggs"));
  }

  @Test
  public void nullConstructorLists_defaultToEmpty_noNpe() {
    RecipeEnrichment e =
        new RecipeEnrichment("r1", null, null, 0.0, null);
    assertFalse(e.hasAllergenWarnings());
    assertTrue(e.getDetectedAllergens().isEmpty());
    assertFalse(e.hasGeneratedInstructions());
    assertFalse(e.hasSuggestedTags());
    assertFalse(e.hasEstimatedCalories());
  }

  @Test
  public void getDetectedAllergens_isUnmodifiable() {
    RecipeEnrichment e = enrichmentWithAllergens("Peanuts");
    try {
      e.getDetectedAllergens().add("Gluten");
      fail("Expected UnsupportedOperationException — list should be unmodifiable");
    } catch (UnsupportedOperationException expected) {
      // pass
    }
  }

  @Test
  public void hasEstimatedCalories_trueOnlyWhenPositive() {
    assertTrue(enrichmentWithAllergens().hasEstimatedCalories()); // 250.0 in helper
    RecipeEnrichment zero =
        new RecipeEnrichment("r1", null, null, 0.0, null);
    assertFalse(zero.hasEstimatedCalories());
  }
}
