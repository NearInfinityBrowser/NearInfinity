// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

/**
 * Provides specialized playback functionality for streamed audio data.
 */
public interface StreamingAudioPlayback extends AudioPlayback {
  /**
   * Returns whether the audio queue is empty. Audio may still be available in the currently processed audio buffer.
   *
   * @return {@code true} if the audio queue is empty, {@code false} otherwise.
   */
  boolean isAudioQueueEmpty();

  /** Removes all remaining {@link AudioBuffer} instances in the audio queue. */
  void clearAudioQueue();

  /**
   * Adds more sound data to the audio queue.
   *
   * @param audioBuffer {@link AudioBuffer} to add.
   * @throws NullPointerException     if the {@code audioBuffer} argument or the associated audio data array is
   *                                    {@code null}.
   * @throws IllegalArgumentException if the audio buffer contains no data.
   */
  void addAudioBuffer(AudioBuffer audioBuffer);

  /**
   * Removes a single instance of the specified {@link AudioBuffer} object if present in the audio queue.
   *
   * @param audioBuffer {@link AudioBuffer} object to be removed from the audio queue, if present.
   * @return {@code true} if an element could be successfully removed, {@code false} otherwise.
   */
  boolean removeAudioBuffer(AudioBuffer audioBuffer);
}
