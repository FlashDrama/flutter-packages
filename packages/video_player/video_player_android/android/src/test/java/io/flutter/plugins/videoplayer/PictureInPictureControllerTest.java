// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Rational;
import androidx.media3.common.Format;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Unit tests for {@link PictureInPictureController}. */
@RunWith(RobolectricTestRunner.class)
public final class PictureInPictureControllerTest {

  @Mock private ExoPlayer mockExoPlayer;
  @Mock private Activity mockActivity;
  @Mock private PackageManager mockPackageManager;

  @Rule public MockitoRule initRule = MockitoJUnit.rule();

  private PictureInPictureController controller;

  @Before
  public void setUp() {
    controller = new PictureInPictureController(mockExoPlayer);
    when(mockActivity.getPackageManager()).thenReturn(mockPackageManager);
    when(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
        .thenReturn(true);
  }

  @Test
  public void isPictureInPictureActive_returnsFalseWhenActivityIsNull() {
    // Activity not set
    assertFalse(controller.isPictureInPictureActive());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void isPictureInPictureActive_returnsFalseWhenNotInPipMode() {
    controller.setActivity(mockActivity);
    when(mockActivity.isInPictureInPictureMode()).thenReturn(false);

    assertFalse(controller.isPictureInPictureActive());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void isPictureInPictureActive_returnsTrueWhenInPipMode() {
    controller.setActivity(mockActivity);
    when(mockActivity.isInPictureInPictureMode()).thenReturn(true);

    assertTrue(controller.isPictureInPictureActive());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.N)
  public void isPictureInPictureActive_returnsFalseOnApiBelow26() {
    controller.setActivity(mockActivity);
    // API 24 doesn't support PiP
    assertFalse(controller.isPictureInPictureActive());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void shouldEnterPip_returnsFalseWhenNotPlaying() {
    controller.setActivity(mockActivity);
    when(mockExoPlayer.isPlaying()).thenReturn(false);

    assertFalse(controller.shouldEnterPip());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void shouldEnterPip_returnsFalseWhenActivityIsNull() {
    // Activity not set
    when(mockExoPlayer.isPlaying()).thenReturn(true);

    assertFalse(controller.shouldEnterPip());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void shouldEnterPip_returnsFalseWhenAlreadyInPipMode() {
    controller.setActivity(mockActivity);
    when(mockExoPlayer.isPlaying()).thenReturn(true);
    when(mockActivity.isInPictureInPictureMode()).thenReturn(true);

    assertFalse(controller.shouldEnterPip());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void shouldEnterPip_returnsFalseWhenPipNotSupported() {
    controller.setActivity(mockActivity);
    when(mockExoPlayer.isPlaying()).thenReturn(true);
    when(mockActivity.isInPictureInPictureMode()).thenReturn(false);
    when(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
        .thenReturn(false);

    assertFalse(controller.shouldEnterPip());
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void shouldEnterPip_returnsTrueWhenAllConditionsMet() {
    controller.setActivity(mockActivity);
    when(mockExoPlayer.isPlaying()).thenReturn(true);
    when(mockActivity.isInPictureInPictureMode()).thenReturn(false);

    assertTrue(controller.shouldEnterPip());
  }

  @Test
  public void calculateAspectRatio_returnsLandscapeForWideVideo() {
    Format format = new Format.Builder().setWidth(1920).setHeight(1080).build();
    when(mockExoPlayer.getVideoFormat()).thenReturn(format);

    Rational aspectRatio = controller.calculateAspectRatio();

    assertEquals(new Rational(16, 9), aspectRatio);
  }

  @Test
  public void calculateAspectRatio_returnsPortraitForTallVideo() {
    Format format = new Format.Builder().setWidth(1080).setHeight(1920).build();
    when(mockExoPlayer.getVideoFormat()).thenReturn(format);

    Rational aspectRatio = controller.calculateAspectRatio();

    assertEquals(new Rational(9, 16), aspectRatio);
  }

  @Test
  public void calculateAspectRatio_appliesRotationCorrection() {
    // Video is 1080x1920 but rotated 90 degrees, so effective dimensions are 1920x1080
    Format format =
        new Format.Builder().setWidth(1080).setHeight(1920).setRotationDegrees(90).build();
    when(mockExoPlayer.getVideoFormat()).thenReturn(format);

    Rational aspectRatio = controller.calculateAspectRatio();

    // After 90 degree rotation, video should be landscape
    assertEquals(new Rational(16, 9), aspectRatio);
  }

  @Test
  public void calculateAspectRatio_returnsDefaultWhenFormatIsNull() {
    when(mockExoPlayer.getVideoFormat()).thenReturn(null);

    Rational aspectRatio = controller.calculateAspectRatio();

    // Default is landscape 16:9
    assertEquals(new Rational(16, 9), aspectRatio);
  }

  @Test
  public void calculateAspectRatio_returnsLandscapeForSquareVideo() {
    Format format = new Format.Builder().setWidth(1080).setHeight(1080).build();
    when(mockExoPlayer.getVideoFormat()).thenReturn(format);

    Rational aspectRatio = controller.calculateAspectRatio();

    // Square videos are treated as landscape
    assertEquals(new Rational(16, 9), aspectRatio);
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.S)
  public void onDispose_disablesAutoEnterPipOnApi31() {
    controller.setActivity(mockActivity);

    controller.onDispose();

    // Verify setPictureInPictureParams was called with autoEnterEnabled=false
    verify(mockActivity, atLeastOnce()).setPictureInPictureParams(any(PictureInPictureParams.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.O)
  public void onDispose_doesNothingOnApi26To30() {
    controller.setActivity(mockActivity);
    reset(mockActivity);
    when(mockActivity.getPackageManager()).thenReturn(mockPackageManager);

    controller.onDispose();

    // setPictureInPictureParams should not be called on API 26-30 during dispose
    verify(mockActivity, never()).setPictureInPictureParams(any(PictureInPictureParams.class));
  }

  @Test
  @Config(sdk = Build.VERSION_CODES.S)
  public void onDispose_doesNothingWhenActivityIsNull() {
    // Activity not set
    controller.onDispose();

    // No exception should be thrown
  }
}
