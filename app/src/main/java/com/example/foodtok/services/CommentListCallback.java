package com.example.foodtok.services;

import com.example.foodtok.models.Comment;

import java.util.List;

/** Async callback for operations that return a list of comments. */
public interface CommentListCallback {
  void onSuccess(List<Comment> comments);
  void onError(String message);
}
