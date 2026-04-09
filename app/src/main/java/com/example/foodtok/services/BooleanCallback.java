package com.example.foodtok.services;

/** Async callback for operations that return a boolean result. */
public interface BooleanCallback {
  void onResult(boolean value);
  void onError(String message);
}
