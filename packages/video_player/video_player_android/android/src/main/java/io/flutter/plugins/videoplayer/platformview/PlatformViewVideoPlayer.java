// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer.platformview;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import io.flutter.plugins.videoplayer.ExoPlayerEventListener;
import io.flutter.plugins.videoplayer.PictureInPictureController;
import io.flutter.plugins.videoplayer.VideoAsset;
import io.flutter.plugins.videoplayer.VideoPlayer;
import io.flutter.plugins.videoplayer.VideoPlayerCallbacks;
import io.flutter.plugins.videoplayer.VideoPlayerOptions;
import io.flutter.view.TextureRegistry.SurfaceProducer;

/**
 * A subclass of {@link VideoPlayer} that adds functionality related to platform view as a way of
 * displaying the video in the app.
 */
public class PlatformViewVideoPlayer extends VideoPlayer {
  @Nullable private PictureInPictureController pipController;

  @VisibleForTesting
  public PlatformViewVideoPlayer(
      @NonNull Context context,
      @NonNull VideoPlayerCallbacks events,
      @NonNull MediaItem mediaItem,
      @NonNull VideoPlayerOptions options,
      @NonNull ExoPlayerProvider exoPlayerProvider) {
    super(context, events, mediaItem, options, /* surfaceProducer */ null, exoPlayerProvider);
  }

  /**
   * Creates a platform view video player.
   *
   * @param context application context.
   * @param events event callbacks.
   * @param asset asset to play.
   * @param options options for playback.
   * @return a video player instance.
   */
  @NonNull
  public static PlatformViewVideoPlayer create(
      @NonNull Context context,
      @NonNull VideoPlayerCallbacks events,
      @NonNull VideoAsset asset,
      @NonNull VideoPlayerOptions options) {
    return new PlatformViewVideoPlayer(
        context,
        events,
        asset.getMediaItem(),
        options,
        () -> {
          ExoPlayer.Builder builder =
              new ExoPlayer.Builder(context)
                  .setMediaSourceFactory(asset.getMediaSourceFactory(context));
          return builder.build();
        });
  }

  @NonNull
  @Override
  protected ExoPlayerEventListener createExoPlayerEventListener(
      @NonNull ExoPlayer exoPlayer, @Nullable SurfaceProducer surfaceProducer) {
    // Create PiP controller with the ExoPlayer instance
    pipController = new PictureInPictureController(exoPlayer);

    return new PlatformViewExoPlayerEventListener(
        exoPlayer, videoPlayerEvents, this::onPlaybackStateChanged);
  }

  /**
   * Sets the Activity reference for PiP operations.
   *
   * @param activity The current Activity, or null when detached.
   */
  public void setActivity(@Nullable Activity activity) {
    if (pipController != null) {
      pipController.setActivity(activity);
    }
  }

  /**
   * Returns whether Picture-in-Picture mode is currently active.
   *
   * @return true if PiP is active, false otherwise.
   */
  @Override
  public boolean isPictureInPictureActive() {
    if (pipController == null) {
      return false;
    }
    return pipController.isPictureInPictureActive();
  }

  /**
   * Attempts to enter Picture-in-Picture mode.
   *
   * <p>PiP will only be entered if the video is currently playing and the device supports PiP.
   *
   * @return true if PiP mode was entered successfully, false otherwise.
   */
  public boolean enterPictureInPicture() {
    if (pipController == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return false;
    }
    return pipController.enterPictureInPicture();
  }

  /** Called when playback state changes to update PiP settings. */
  private void onPlaybackStateChanged() {
    if (pipController != null) {
      pipController.onPlaybackStateChanged();
    }
  }

  @Override
  public void dispose() {
    // Disable auto-enter PiP before releasing the player
    if (pipController != null) {
      pipController.onDispose();
    }
    super.dispose();
  }
}
