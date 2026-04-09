package com.example.foodtok.services;

import com.example.foodtok.models.Comment;

/** Async callback for operations that return a single comment. */
public interface CommentCallback {
  void onSuccess(Comment comment);
  void onError(String message);
}
