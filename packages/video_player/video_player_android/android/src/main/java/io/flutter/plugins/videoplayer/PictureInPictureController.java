// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Rational;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.Format;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * Controller for Picture-in-Picture functionality.
 *
 * <p>Handles PiP operations including:
 *
 * <ul>
 *   <li>Building PiP parameters with appropriate aspect ratio
 *   <li>Entering PiP mode when conditions are met
 *   <li>Managing PiP state
 *   <li>Dynamic control of auto-enter PiP on API 31+
 * </ul>
 */
public class PictureInPictureController {
  @NonNull private final ExoPlayer exoPlayer;
  @Nullable private Activity activity;

  /** Aspect ratio for landscape videos (16:9). */
  private static final Rational ASPECT_RATIO_LANDSCAPE = new Rational(16, 9);

  /** Aspect ratio for portrait videos (9:16). */
  private static final Rational ASPECT_RATIO_PORTRAIT = new Rational(9, 16);

  /**
   * Creates a new PictureInPictureController.
   *
   * @param exoPlayer The ExoPlayer instance to monitor for playback state.
   */
  public PictureInPictureController(@NonNull ExoPlayer exoPlayer) {
    this.exoPlayer = exoPlayer;
  }

  /**
   * Sets the Activity reference for PiP operations.
   *
   * @param activity The current Activity, or null when detached.
   */
  public void setActivity(@Nullable Activity activity) {
    this.activity = activity;
    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      updateAutoEnterPip();
    }
  }

  /**
   * Returns whether Picture-in-Picture mode is currently active.
   *
   * @return true if PiP is active, false otherwise.
   */
  public boolean isPictureInPictureActive() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) {
      return false;
    }
    return activity.isInPictureInPictureMode();
  }

  /**
   * Attempts to enter Picture-in-Picture mode.
   *
   * <p>PiP will only be entered if:
   *
   * <ul>
   *   <li>API level is 26 or higher
   *   <li>Activity is available
   *   <li>Video is currently playing
   *   <li>Device supports PiP
   *   <li>Not already in PiP mode
   * </ul>
   *
   * @return true if PiP mode was entered successfully, false otherwise.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  public boolean enterPictureInPicture() {
    if (!shouldEnterPip()) {
      return false;
    }

    PictureInPictureParams params = buildPictureInPictureParams();
    return activity.enterPictureInPictureMode(params);
  }

  /**
   * Called when playback state changes to update auto-enter PiP settings.
   *
   * <p>On API 31+, this updates the auto-enter PiP setting based on whether the video is playing.
   */
  public void onPlaybackStateChanged() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      updateAutoEnterPip();
    }
  }

  /**
   * Checks if PiP mode should be entered.
   *
   * @return true if all conditions for entering PiP are met.
   */
  @VisibleForTesting
  boolean shouldEnterPip() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return false;
    }
    if (activity == null) {
      return false;
    }
    if (!exoPlayer.isPlaying()) {
      return false;
    }
    if (activity.isInPictureInPictureMode()) {
      return false;
    }
    return isPipSupported();
  }

  /**
   * Checks if the device supports Picture-in-Picture.
   *
   * @return true if PiP is supported on this device.
   */
  private boolean isPipSupported() {
    if (activity == null) {
      return false;
    }
    return activity
        .getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
  }

  /**
   * Builds PictureInPictureParams with the appropriate aspect ratio.
   *
   * @return PictureInPictureParams configured with the video's aspect ratio.
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  @NonNull
  private PictureInPictureParams buildPictureInPictureParams() {
    PictureInPictureParams.Builder builder =
        new PictureInPictureParams.Builder().setAspectRatio(calculateAspectRatio());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      builder.setAutoEnterEnabled(exoPlayer.isPlaying());
    }

    return builder.build();
  }

  /**
   * Calculates the appropriate aspect ratio based on video dimensions.
   *
   * <p>Returns 16:9 for landscape videos and 9:16 for portrait videos.
   *
   * @return The calculated aspect ratio.
   */
  @VisibleForTesting
  @NonNull
  Rational calculateAspectRatio() {
    Format videoFormat = exoPlayer.getVideoFormat();
    if (videoFormat == null) {
      return ASPECT_RATIO_LANDSCAPE;
    }

    int width = videoFormat.width;
    int height = videoFormat.height;

    // Apply rotation correction
    int rotation = videoFormat.rotationDegrees;
    if (rotation == 90 || rotation == 270) {
      int temp = width;
      width = height;
      height = temp;
    }

    // Return 16:9 for landscape, 9:16 for portrait
    return (width >= height) ? ASPECT_RATIO_LANDSCAPE : ASPECT_RATIO_PORTRAIT;
  }

  /**
   * Updates the auto-enter PiP setting on API 31+.
   *
   * <p>Auto-enter is enabled only when the video is playing.
   */
  @RequiresApi(api = Build.VERSION_CODES.S)
  private void updateAutoEnterPip() {
    if (activity == null || !isPipSupported()) {
      return;
    }

    PictureInPictureParams params = buildPictureInPictureParams();
    activity.setPictureInPictureParams(params);
  }

  /**
   * Disables auto-enter PiP when the player is being disposed.
   *
   * <p>This ensures PiP won't be triggered after the player is released.
   */
  public void onDispose() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null && isPipSupported()) {
      disableAutoEnterPip();
    }
  }

  /**
   * Disables auto-enter PiP on API 31+.
   */
  @RequiresApi(api = Build.VERSION_CODES.S)
  private void disableAutoEnterPip() {
    PictureInPictureParams params =
        new PictureInPictureParams.Builder()
            .setAutoEnterEnabled(false)
            .build();
    activity.setPictureInPictureParams(params);
  }
}
