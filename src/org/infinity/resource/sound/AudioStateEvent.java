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
    /** Playback of the current sound clip has started from the beginning. Associated value: {@code null} */
    START,
    /** Playback of the current sound clip has stopped. Associated value: {@code null} */
    STOP,
    /** Playback of the current sound clip is paused. Associated value: Elapsed time in milliseconds ({@link Long}) */
    PAUSE,
    /** Paused playback is resumed. Associated value: Elapsed time in milliseconds ({@link Long}) */
    RESUME,
  }

  private AudioStateEvent.State audioState;
  private Object value;

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
    StringBuilder sb = new StringBuilder(getClass().getName());
    sb.append("[audioState=").append(getAudioState());
    sb.append("; value=").append(getValue());
    sb.append("; source=").append(getSource());
    return sb.append("]").toString();
  }
}