package com.example.foodtok.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Extracts the first frame of a remote video URL as a Bitmap on a
 * background thread, caches the result in memory, and posts it back
 * to the target ImageView. Used as a fallback for recipes that do
 * not have a dedicated thumbnail uploaded.
 */
public final class VideoThumbnailLoader {

  private static final int CACHE_SIZE_BYTES = 16 * 1024 * 1024; // 16 MB
  private static final long FRAME_TIME_US = 1_000_000L; // 1 second in
  /** Target thumbnail width in px. Grid cells are ~half screen wide. */
  private static final int THUMB_WIDTH_PX = 480;
  private static final int THUMB_HEIGHT_PX = 720; // 2:3 aspect to match item_grid_recipe

  private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
  private static final Handler MAIN = new Handler(Looper.getMainLooper());

  private static final LruCache<String, Bitmap> CACHE =
      new LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
          return value.getByteCount();
        }
      };

  /** Tracks the latest requested url per ImageView to avoid stale binds on recycle. */
  private static final Map<ImageView, String> PENDING = new HashMap<>();

  private VideoThumbnailLoader() {}

  public static void load(String videoUrl, ImageView target) {
    if (target == null) {
      return;
    }
    if (TextUtils.isEmpty(videoUrl)) {
      PENDING.remove(target);
      target.setImageDrawable(null);
      return;
    }

    PENDING.put(target, videoUrl);

    Bitmap cached = CACHE.get(videoUrl);
    if (cached != null) {
      target.setImageBitmap(cached);
      return;
    }

    target.setImageDrawable(null);

    EXECUTOR.execute(() -> {
      Bitmap frame = extractFrame(videoUrl);
      if (frame == null) {
        return;
      }
      CACHE.put(videoUrl, frame);
      MAIN.post(() -> {
        if (videoUrl.equals(PENDING.get(target))) {
          target.setImageBitmap(frame);
        }
      });
    });
  }

  private static Bitmap extractFrame(String videoUrl) {
    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    try {
      retriever.setDataSource(videoUrl, new HashMap<>());
      Bitmap frame = null;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        frame = retriever.getScaledFrameAtTime(
            FRAME_TIME_US,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            THUMB_WIDTH_PX,
            THUMB_HEIGHT_PX);
      }
      if (frame == null) {
        frame = retriever.getFrameAtTime(
            FRAME_TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
      }
      if (frame == null) {
        frame = retriever.getFrameAtTime();
      }
      if (frame != null && frame.getWidth() > THUMB_WIDTH_PX * 2) {
        // API < 27 path: downscale manually so the LRU cache holds many entries.
        Bitmap scaled = Bitmap.createScaledBitmap(
            frame, THUMB_WIDTH_PX, THUMB_HEIGHT_PX, true);
        if (scaled != frame) {
          frame.recycle();
        }
        frame = scaled;
      }
      return frame;
    } catch (RuntimeException e) {
      return null;
    } finally {
      try {
        retriever.release();
      } catch (IOException | RuntimeException ignored) {
      }
    }
  }
}
