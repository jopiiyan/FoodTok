package com.example.foodtok.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.UploadRecipeRequest;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Real {@link IRecipeService} implementation backed by Supabase PostgREST. */
public class SupabaseRecipeService implements IRecipeService {

  private static final String RECIPE_SELECT =
      "*,recipe_ingredients(quantity,is_optional,"
      + "ingredients(id,name,calories_per_100g)),"
      + "profiles!author_id(username)";

  private final SupabaseApi api;
  private final SupabaseStorageApi storageApi;

  public SupabaseRecipeService() {
    this.api = ApiClient.getRestClient().create(SupabaseApi.class);
    this.storageApi = ApiClient.getStorageClient()
        .create(SupabaseStorageApi.class);
  }

  @Override
  public void getFeedRecipes(int page, int pageSize,
      RecipeListCallback callback) {
    int from = page * pageSize;
    int to = from + pageSize - 1;
    String range = from + "-" + to;

    api.getRecipes(RECIPE_SELECT, "created_at.desc", range)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Recipe> recipes = new ArrayList<>();
              for (RecipeDto dto : response.body()) {
                recipes.add(dto.toDomain());
              }
              callback.onSuccess(recipes);
            } else {
              callback.onError("Failed to load recipes: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void getRecipeById(String recipeId, RecipeCallback callback) {
    api.getRecipeById("eq." + recipeId, RECIPE_SELECT)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(response.body().get(0).toDomain());
            } else {
              callback.onError("Recipe not found");
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void uploadRecipe(Context context, Uri videoUri, String title,
      String description, String[] tags, int prepTimeMinutes,
      int cookTimeMinutes, double estimatedCalories,
      RecipeCallback callback) {
    String userId = AuthManager.getInstance().getCurrentUser().getId();
    String fileId = UUID.randomUUID().toString();
    String videoStoragePath = userId + "/" + fileId + ".mp4";
    String thumbnailStoragePath = userId + "/" + fileId + ".jpg";

    // Step 1: Read the video bytes from the content Uri
    byte[] videoBytes;
    try {
      videoBytes = readBytes(context, videoUri);
    } catch (IOException e) {
      callback.onError("Failed to read video file: " + e.getMessage());
      return;
    }

    // Step 2: Extract a thumbnail from the video. If extraction fails,
    // we still proceed with the upload — the grid has a client-side
    // fallback that decodes the first frame on demand.
    final byte[] thumbnailBytes = extractThumbnailJpeg(context, videoUri);

    RequestBody videoBody = RequestBody.create(
        MediaType.parse("video/mp4"), videoBytes);

    // Step 3: Upload video to Supabase Storage
    storageApi.uploadFile("videos", videoStoragePath, "video/mp4", videoBody)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            if (!response.isSuccessful()) {
              callback.onError("Video upload failed: "
                  + response.code());
              return;
            }
            String videoUrl = Constants.STORAGE_BASE_URL
                + "object/public/videos/" + videoStoragePath;

            // Step 4: Upload thumbnail (best-effort, non-blocking for
            // the recipe row creation on failure).
            if (thumbnailBytes == null) {
              createRecipeRow(userId, title, description, videoUrl, null,
                  tags, prepTimeMinutes, cookTimeMinutes,
                  estimatedCalories, callback);
              return;
            }
            uploadThumbnail(thumbnailBytes, thumbnailStoragePath, thumbUrl ->
                createRecipeRow(userId, title, description, videoUrl, thumbUrl,
                    tags, prepTimeMinutes, cookTimeMinutes,
                    estimatedCalories, callback));
          }

          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onError("Video upload error: " + t.getMessage());
          }
        });
  }

  /** Simple single-arg callback for the thumbnail upload step. */
  private interface ThumbnailUploadCallback {
    void onDone(String thumbnailUrl);
  }

  /**
   * Uploads a JPEG thumbnail to the "thumbnails" bucket. On failure the
   * callback is still invoked with {@code null} so recipe creation can
   * proceed — a missing thumbnail is non-fatal.
   */
  private void uploadThumbnail(byte[] jpegBytes, String storagePath,
      ThumbnailUploadCallback callback) {
    RequestBody body = RequestBody.create(
        MediaType.parse("image/jpeg"), jpegBytes);
    storageApi.uploadFile("thumbnails", storagePath, "image/jpeg", body)
        .enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call,
              Response<ResponseBody> response) {
            if (response.isSuccessful()) {
              callback.onDone(Constants.STORAGE_BASE_URL
                  + "object/public/thumbnails/" + storagePath);
            } else {
              callback.onDone(null);
            }
          }

          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
            callback.onDone(null);
          }
        });
  }

  /**
   * Extracts a frame from the video at the 1-second mark, scales it to
   * a reasonable thumbnail size, and encodes as JPEG. Returns null if
   * extraction fails for any reason.
   */
  private byte[] extractThumbnailJpeg(Context context, Uri videoUri) {
    final int targetWidth = 720;
    final int targetHeight = 1080;
    final long frameTimeUs = 1_000_000L;
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    Bitmap frame = null;
    try {
      retriever.setDataSource(context, videoUri);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        frame = retriever.getScaledFrameAtTime(
            frameTimeUs,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            targetWidth,
            targetHeight);
      }
      if (frame == null) {
        frame = retriever.getFrameAtTime(
            frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
      }
      if (frame == null) {
        return null;
      }
      if (frame.getWidth() > targetWidth * 2) {
        Bitmap scaled = Bitmap.createScaledBitmap(
            frame, targetWidth, targetHeight, true);
        if (scaled != frame) {
          frame.recycle();
          frame = scaled;
        }
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      frame.compress(Bitmap.CompressFormat.JPEG, 80, out);
      return out.toByteArray();
    } catch (RuntimeException e) {
      return null;
    } finally {
      if (frame != null) {
        frame.recycle();
      }
      try {
        retriever.release();
      } catch (IOException | RuntimeException ignored) {
      }
    }
  }

  /**
   * Creates the recipe row in PostgREST after the video has been uploaded.
   */
  private void createRecipeRow(String authorId, String title,
      String description, String videoUrl, String thumbnailUrl,
      String[] tags, int prepTimeMinutes, int cookTimeMinutes,
      double estimatedCalories, RecipeCallback callback) {
    UploadRecipeRequest request = new UploadRecipeRequest();
    request.authorId = authorId;
    request.title = title;
    request.description = description;
    request.videoUrl = videoUrl;
    request.thumbnailUrl = thumbnailUrl;
    request.tags = tags;
    request.prepTimeMinutes = prepTimeMinutes;
    request.cookTimeMinutes = cookTimeMinutes;
    request.estimatedCalories = estimatedCalories;

    api.createRecipe(request)
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null
                && !response.body().isEmpty()) {
              callback.onSuccess(
                  response.body().get(0).toDomain());
            } else {
              callback.onError("Failed to create recipe: "
                  + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  @Override
  public void searchByIngredients(Set<String> searchTokens,
      RecipeListCallback callback) {
    // Tokens may be tag names OR ingredient names — we rank by how many
    // distinct tokens each recipe matches (logical OR). Fetch all recipes
    // then rank client-side.
    api.getRecipes(RECIPE_SELECT, "created_at.desc", "0-99")
        .enqueue(new Callback<List<RecipeDto>>() {
          @Override
          public void onResponse(Call<List<RecipeDto>> call,
              Response<List<RecipeDto>> response) {
            if (response.isSuccessful() && response.body() != null) {
              List<Recipe> matched = new ArrayList<>();
              for (RecipeDto dto : response.body()) {
                Recipe recipe = dto.toDomain();
                if (recipe.countMatchingTokens(searchTokens) > 0) {
                  matched.add(recipe);
                }
              }
              // Sort by distinct token match count descending
              Collections.sort(matched, (a, b) ->
                  Integer.compare(
                      b.countMatchingTokens(searchTokens),
                      a.countMatchingTokens(searchTokens)));
              callback.onSuccess(matched);
            } else {
              callback.onError("Search failed: " + response.code());
            }
          }

          @Override
          public void onFailure(Call<List<RecipeDto>> call,
              Throwable t) {
            callback.onError("Network error: " + t.getMessage());
          }
        });
  }

  /** Reads all bytes from a content:// Uri. */
  private byte[] readBytes(Context context, Uri uri)
      throws IOException {
    try (InputStream is = context.getContentResolver()
        .openInputStream(uri)) {
      if (is == null) {
        throw new IOException("Cannot open input stream for " + uri);
      }
      java.io.ByteArrayOutputStream buffer =
          new java.io.ByteArrayOutputStream();
      byte[] chunk = new byte[8192];
      int bytesRead;
      while ((bytesRead = is.read(chunk)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      return buffer.toByteArray();
    }
  }
}
