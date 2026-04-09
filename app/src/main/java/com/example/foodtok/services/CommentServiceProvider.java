package com.example.foodtok.services;

/** Factory singleton that provides the active {@link ICommentService} implementation. */
public class CommentServiceProvider {

  private static ICommentService commentService;

  private CommentServiceProvider() {
  }

  /** Returns the current comment service (defaults to mock if none set). */
  public static ICommentService getCommentService() {
    if (commentService == null) {
      commentService = new MockCommentService();
    }
    return commentService;
  }

  /** Swaps in a different implementation (e.g. real Supabase service). */
  public static void setCommentService(ICommentService service) {
    commentService = service;
  }
}
