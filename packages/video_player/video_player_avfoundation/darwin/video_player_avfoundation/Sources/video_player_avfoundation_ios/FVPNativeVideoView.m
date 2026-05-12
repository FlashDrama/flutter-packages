// Copyright 2013 The Flutter Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import "../video_player_avfoundation/include/video_player_avfoundation/FVPNativeVideoView.h"

#import <AVFoundation/AVFoundation.h>
#import <AVKit/AVKit.h>
#import <UIKit/UIKit.h>

@interface FVPPlayerView : UIView
@end

@implementation FVPPlayerView
+ (Class)layerClass {
  return [AVPlayerLayer class];
}

- (void)setPlayer:(AVPlayer *)player {
  [(AVPlayerLayer *)[self layer] setPlayer:player];
}
@end

@interface FVPNativeVideoView ()
@property(nonatomic) FVPPlayerView *playerView;
@property(nonatomic, strong) AVPictureInPictureController *pipController;
@end

@implementation FVPNativeVideoView
- (instancetype)initWithPlayer:(AVPlayer *)player {
  if (self = [super init]) {
    _playerView = [[FVPPlayerView alloc] init];
    [_playerView setPlayer:player];

    // Setup Picture-in-Picture for automatic background transition
    if (@available(iOS 14.2, *)) {
      if ([AVPictureInPictureController isPictureInPictureSupported]) {
        AVPlayerLayer *playerLayer = (AVPlayerLayer *)[_playerView layer];
        _pipController = [[AVPictureInPictureController alloc] initWithPlayerLayer:playerLayer];
        _pipController.canStartPictureInPictureAutomaticallyFromInline = YES;

        [[NSNotificationCenter defaultCenter]
            addObserver:self
               selector:@selector(applicationDidBecomeActive:)
                   name:UIApplicationDidBecomeActiveNotification
                 object:nil];
      }
    }
  }
  return self;
}

- (void)dealloc {
  [[NSNotificationCenter defaultCenter] removeObserver:self
                                                  name:UIApplicationDidBecomeActiveNotification
                                                object:nil];
}

// Stop any active PiP whenever the app foregrounds so that returning to the
// app via any path (icon, app switcher, deeplink, …) tears down PiP through
// the standard delegate sequence (`willStop` → `didStop`) instead of leaving
// the floating window on top of the app. The PiP restore button path is
// observed to start its stop before this notification fires, so the
// `pictureInPictureActive` guard skips this branch and avoids a double-stop;
// if that ordering ever changes the guard still prevents redundant teardown.
- (void)applicationDidBecomeActive:(NSNotification *)notification {
  if (self.pipController.pictureInPictureActive) {
    [self.pipController stopPictureInPicture];
  }
}

- (FVPPlayerView *)view {
  return self.playerView;
}

- (BOOL)isPictureInPictureActive {
  if (@available(iOS 14.2, *)) {
    return self.pipController.pictureInPictureActive;
  }
  return NO;
}
@end
