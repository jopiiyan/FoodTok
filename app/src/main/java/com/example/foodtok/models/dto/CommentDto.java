package com.example.foodtok.models.dto;

import com.example.foodtok.models.Comment;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DTO for the Supabase {@code comments} table with nested author join.
 * PostgREST select: {@code *,users(username,display_name,avatar_url)}
 */
public class CommentDto {

  @SerializedName("id")
  public String id;

  @SerializedName("user_id")
  public String userId;

  @SerializedName("recipe_id")
  public String recipeId;

  @SerializedName("content")
  public String content;

  @SerializedName("created_at")
  public String createdAt;

  /** Nested join: comment author. */
  @SerializedName("users")
  public UserDto author;

  /** Converts this DTO to the domain {@link Comment} model. */
  public Comment toDomain() {
    String authorName = "Unknown";
    String avatarUrl = null;
    if (author != null) {
      if (author.displayName != null && !author.displayName.isEmpty()) {
        authorName = author.displayName;
      } else if (author.username != null) {
        authorName = author.username;
      }
      avatarUrl = author.avatarUrl;
    }

    long timestampMillis = parseTimestamp(createdAt);
    return new Comment(id, authorName, avatarUrl, content, timestampMillis);
  }

  private long parseTimestamp(String iso) {
    if (iso == null) {
      return System.currentTimeMillis();
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(
          "yyyy-MM-dd'T'HH:mm:ss", Locale.US);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = sdf.parse(iso);
      return date != null ? date.getTime() : System.currentTimeMillis();
    } catch (ParseException e) {
      return System.currentTimeMillis();
    }
  }
}
