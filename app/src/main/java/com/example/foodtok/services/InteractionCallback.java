package com.example.foodtok.services;

/** Async callback for recipe interaction operations (success or error). */
public interface InteractionCallback {
  void onSuccess();
  void onError(String message);
}
