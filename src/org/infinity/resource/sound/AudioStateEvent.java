// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.util.EventObject;

/**
 * An AudioStateEvent is triggered when the state of a sound clip has changed.
 * <p>
 * States may include opening or closing a sound resource, starting or stopping playback, and pausing or resuming
 * playback.
 * </p>
 */
public class AudioStateEvent extends EventObject {
  /** Provides available audio states. */
  public enum State {
    /** An unrecoverable error was triggered during audio playback. Associated value: the thrown {@link Exception}. */
    ERROR,
    /**
     * A sound resource has been successfully opened and is ready for playback. Associated value: Sound resource name
     * {@link String}) if available, {@code null} otherwise.
     */
    OPEN,
    /**
     * The current sound clip is released. Associated value: Sound resource name {@link String}) if available,
     * {@code null} otherwise.
     */
    CLOSE,
    /**
     * Playback of the current sound clip has started from the beginning. Associated value: {@code null}
     * <p><strong>Note:</strong> This state is only triggered if the audio line starts processing actual audio data.</p>
     */
    START,
    /** Playback of the current sound clip has stopped. Associated value: {@code null} */
    STOP,
    /** Playback of the current sound clip is paused. Associated value: Elapsed time in milliseconds ({@link Long}) */
    PAUSE,
    /** Paused playback is resumed. Associated value: Elapsed time in milliseconds ({@link Long}) */
    RESUME,
    /**
     * Streamed audio playback only: The current audio buffer contains no more data. Associated value: {@link Boolean}
     * that indicates whether more audio buffers are queued.
     */
    BUFFER_EMPTY,
  }

  private final AudioStateEvent.State audioState;
  private final Object value;

  public AudioStateEvent(Object source, AudioStateEvent.State audioState, Object value) {
    super(source);
    this.audioState = audioState;
    this.value = value;
  }

  /** Returns the state that triggered this event. */
  public AudioStateEvent.State getAudioState() {
    return audioState;
  }

  /** Returns the value associated with the state. Value may be {@code null} if the state doesn't provide a value. */
  public Object getValue() {
    return value;
  }

  public String toString() {
    return getClass().getName() + "[audioState=" + getAudioState() +
        "; value=" + getValue() +
        "; source=" + getSource() +
        "]";
  }
}
