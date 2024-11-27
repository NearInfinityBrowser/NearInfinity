// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import org.infinity.resource.Closeable;

/**
 * Provides basic playback functionality for audio data.
 */
public interface AudioPlayback extends Closeable {
  /**
   * Returns the elapsed playback time of audio data.
   *
   * @return Elapsed playback time, in milliseconds.
   */
  long getElapsedTime();

  /**
   * Returns whether playback is active. Pausing and resuming playback does not affect the result.
   *
   * @return {@code true} if playback is active, {@code false} otherwise.
   */
  boolean isPlaying();

  /**
   * Starts or stops playback of audio data.
   *
   * @param play Specify {@code true} to start playback or {@code false} to stop playback.
   */
  void setPlaying(boolean play);

  /**
   * Returns whether if playback is paused. Enabling or disabled the paused state does not affect the result of
   * {@link #isPlaying()}..
   *
   * @return {@code true} if current playback is paused, {@code false} otherwise.
   */
  boolean isPaused();

  /**
   * Enters or leaves paused state when playback is active. Does nothing is playback is stopped.
   *
   * @param pause Specify {@code true} to pause current playback or {@code false} to resume playback.
   */
  void setPaused(boolean pause);

  /**
   * Returns whether the player has been closed. A closed audio player does not accept new playback commands.
   *
   * @return {@code true} if {@link #close()} was called on this audio player instance, {@code false} otherwise.
   */
  boolean isClosed();
}
