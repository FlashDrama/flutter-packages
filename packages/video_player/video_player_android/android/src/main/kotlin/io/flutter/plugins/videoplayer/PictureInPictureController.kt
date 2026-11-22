// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package io.flutter.plugins.videoplayer

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Keeps the platform view player's PiP state in sync with its host activity. */
internal class PictureInPictureController(private val player: ExoPlayer) : Player.Listener {
  private var host: Activity? = null

  fun setActivity(activity: Activity?) {
    host = activity
    updateAutoEnter(player.isPlaying)
  }

  fun isPictureInPictureActive() =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && host?.isInPictureInPictureMode == true

  private fun isSupported() =
      host?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true

  fun shouldEnterPip() =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
          isSupported() &&
          player.isPlaying &&
          !isPictureInPictureActive()

  fun calculateAspectRatio(): Rational {
    val format = player.videoFormat ?: return Rational(16, 9)
    val rotated = format.rotationDegrees == 90 || format.rotationDegrees == 270
    val landscape = if (rotated) format.height >= format.width else format.width >= format.height
    return if (landscape) Rational(16, 9) else Rational(9, 16)
  }

  @RequiresApi(Build.VERSION_CODES.O)
  fun enterIfPlaying() {
    if (shouldEnterPip()) {
      host?.enterPictureInPictureMode(
          PictureInPictureParams.Builder().setAspectRatio(calculateAspectRatio()).build())
    }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) = updateAutoEnter(isPlaying)

  private fun updateAutoEnter(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isSupported()) {
      host?.setPictureInPictureParams(
          PictureInPictureParams.Builder()
              .setAspectRatio(calculateAspectRatio())
              .setAutoEnterEnabled(enabled)
              .build())
    }
  }

  fun onDispose() {
    updateAutoEnter(false)
    host = null
  }
}
