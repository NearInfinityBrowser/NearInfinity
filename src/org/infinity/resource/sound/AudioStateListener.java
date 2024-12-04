// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.util.EventListener;

/**
 * Instances of classes that implement the {@code AudioStateListener} interface can receive events when the state
 * of the audio player has changed.
 */
public interface AudioStateListener extends EventListener {
  /**
   * Informs the listener that the audio state has changed.
   *
   * @param event a {@link AudioStateEvent} that describes the changed state.
   */
  void audioStateChanged(AudioStateEvent event);
}
