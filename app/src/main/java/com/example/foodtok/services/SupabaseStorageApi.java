package com.example.foodtok.services;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Retrofit interface for uploading files to Supabase Storage. */
public interface SupabaseStorageApi {

  /**
   * Uploads a file to a Supabase Storage bucket.
   *
   * @param bucket   the storage bucket name (e.g. "videos", "thumbnails")
   * @param filePath the path within the bucket (e.g. "userId/uuid.mp4")
   * @param contentType MIME type of the file (e.g. "video/mp4")
   * @param body     the raw file bytes
   * @return response containing the storage key
   */
  @POST("object/{bucket}/{filePath}")
  Call<ResponseBody> uploadFile(
      @Path("bucket") String bucket,
      @Path(value = "filePath", encoded = true) String filePath,
      @Header("Content-Type") String contentType,
      @Body RequestBody body
  );
}
