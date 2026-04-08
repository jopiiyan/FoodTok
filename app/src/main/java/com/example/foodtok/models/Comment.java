package com.example.foodtok.models;

/**
 * Represents a single comment on a recipe.
 *
 * OOP: Encapsulation — all fields are private; id is final (immutable identity).
 * Tell, Don't Ask — getFormattedTime() encapsulates display logic inside the model.
 */
public class Comment {

  private final String id;       // immutable — identity never changes after creation
  private String authorName;
  private String authorAvatarUrl;
  private String text;
  private long timestampMillis;  // Unix epoch ms; convert to relative string via getFormattedTime()
  private int likeCount;

  public Comment(String id, String authorName, String authorAvatarUrl,
                   String text, long timestampMillis) {
    this.id = id;
    this.authorName = authorName;
    this.authorAvatarUrl = authorAvatarUrl;
    this.text = text;
    this.timestampMillis = timestampMillis;
    this.likeCount = 0;
  }

  // --- Getters ---

  public String getId() { return id; }

  public String getAuthorName() { return authorName; }

  public String getAuthorAvatarUrl() { return authorAvatarUrl; }

  public String getText() { return text; }

  public long getTimestampMillis() { return timestampMillis; }

  public int getLikeCount() { return likeCount; }

  // --- Setters (only for mutable fields) ---

  public void setAuthorName(String authorName) { this.authorName = authorName; }

  public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

  public void setText(String text) { this.text = text; }

  public void incrementLike() { this.likeCount++; }

  public void decrementLike() { if (this.likeCount > 0) this.likeCount--; }

  /**
     * Tell, Don't Ask: the model owns time-formatting logic.
     * Returns a human-readable relative timestamp like "2h ago", "just now", etc.
     */
  public String getFormattedTime() {
    long diffMs = System.currentTimeMillis() - timestampMillis;
    long diffSec = diffMs / 1000;
    long diffMin = diffSec / 60;
    long diffHr  = diffMin / 60;
    long diffDay = diffHr  / 24;

    if (diffSec < 60)  return "just now";
    if (diffMin < 60)  return diffMin + "m ago";
    if (diffHr  < 24)  return diffHr  + "h ago";
    return diffDay + "d ago";
  }
}
