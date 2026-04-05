package com.example.foodtok.models;

public class Ingredient {

    private final String name;
    private final double calories;
    private final boolean allergen;

    public Ingredient(String name, double calories, boolean allergen) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingredient name cannot be empty");
        }
        this.name = name.trim().toLowerCase();
        this.calories = calories;
        this.allergen = allergen;
    }

    // --- Getters (all justified) ---

    // Needed: display in recipe detail, search matching
    public String getName() {
        return name;
    }

    // Needed: Recipe.calculateCalories() sums these
    public double getCalories() {
        return calories;
    }

    // Needed: allergen warning display
    public boolean isAllergen() {
        return allergen;
    }

    // No setters — ingredients don't change after creation
    // "Tomato" doesn't become "Potato"
    // Calories are fixed per ingredient
}