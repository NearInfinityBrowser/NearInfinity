// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

/**
 * Provides specialized playback functionality for prebuffered audio data.
 */
public interface BufferedAudioPlayback extends AudioPlayback {
  /**
   * Returns the total length of the audio clip.
   *
   * @return Total sound length, in milliseconds. Returns {@code 0} if unavailable.
   */
  long getTotalLength();

  /**
   * Sets an explicit playback position.
   *
   * @param position New playback position in milliseconds. Position is clamped to the available audio clip duration.
   */
  void setSoundPosition(long position);

  /**
   * Returns whether loop mode is enabled.
   *
   * @return {@code true} if sound playback is in loop mode, {@code false} otherwise.
   */
  boolean isLooped();

  /**
   * Enables or disables looped playback.
   *
   * @param loop Specify {@code true} to start playback from the beginning if the end is reached. Specify {@code false}
   *               to stop playback when the end of the audio data is reached.
   */
  void setLooped(boolean loop);
}
