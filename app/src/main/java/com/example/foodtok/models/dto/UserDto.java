package com.example.foodtok.models.dto;

import com.google.gson.annotations.SerializedName;

/** DTO for the Supabase {@code users} table, used in PostgREST joins. */
public class UserDto {

  @SerializedName("id")
  public String id;

  @SerializedName("username")
  public String username;

  @SerializedName("display_name")
  public String displayName;

  @SerializedName("avatar_url")
  public String avatarUrl;
}
