// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.infinity.util.Logger;

/**
 * Common base for audio player classes.
 */
public abstract class AbstractAudioPlayer implements AudioPlayback, LineListener {
  private final EventListenerList listenerList = new EventListenerList();

  private boolean listenersEnabled;

  /** Adds a {@link AudioStateListener} to the audio player instance. */
  public void addAudioStateListener(AudioStateListener l) {
    if (l != null) {
      listenerList.add(AudioStateListener.class, l);
    }
  }

  /** Returns all registered {@link AudioStateListener}s for this audio player instance. */
  public AudioStateListener[] getAudioStateListeners() {
    return listenerList.getListeners(AudioStateListener.class);
  }

  /** Removes a {@link AudioStateListener} from the audio player instance. */
  public void removeAudioStateListener(AudioStateListener l) {
    if (l != null) {
      listenerList.remove(AudioStateListener.class, l);
    }
  }

  /** Fires an {@link AudioStateListener} of the specified name to all registered listeners. */
  protected void fireAudioStateEvent(AudioStateEvent.State state, Object value) {
    // collect and execute
    synchronized (listenerList) {
      final AudioStateListener[] listeners = listenerList.getListeners(AudioStateListener.class);
      if (listeners.length > 0) {
        final AudioStateEvent event = new AudioStateEvent(this, state, value);
        SwingUtilities.invokeLater(() -> {
          for (AudioStateListener listener : listeners) {
            listener.audioStateChanged(event);
          }
        });
      }
    }
  }

  /** Fires if an unrecoverable error occurs during audio playback. */
  protected void fireError(Exception e) {
    fireAudioStateEvent(AudioStateEvent.State.ERROR, e);
  }

  /** Fires when the the audio device is opened. */
  protected void firePlayerOpened() {
    fireAudioStateEvent(AudioStateEvent.State.OPEN, null);
  }

  /** Fires when the audio device is closed. */
  protected void firePlayerClosed() {
    fireAudioStateEvent(AudioStateEvent.State.CLOSE, null);
  }

  /** Fires when playback has started. */
  protected void firePlaybackStarted() {
    fireAudioStateEvent(AudioStateEvent.State.START, null);
  }

  /** Fires when playback has stopped. */
  protected void firePlaybackStopped() {
    fireAudioStateEvent(AudioStateEvent.State.STOP, null);
  }

  /** Fires when playback is set to paused mode. */
  protected void firePlaybackPaused() {
    fireAudioStateEvent(AudioStateEvent.State.PAUSE, getSoundPosition());
  }

  /** Fires when paused playback is resumed. */
  protected void firePlaybackResumed() {
    fireAudioStateEvent(AudioStateEvent.State.RESUME, getSoundPosition());
  }

  /**
   * Returns the audio line responsible for playing back audio data.
   *
   * @return {@link Line} instance of the player.
   */
  protected abstract Line getLine();

  /** Returns whether {@link Line}'s status changes are tracked by this class instance. */
  protected boolean isLineListenersEnabled() {
    return listenersEnabled;
  }

  /** Specifies whether {@link Line}'s status changes should be tracked by this class instance. */
  protected void setLineListenersEnabled(boolean enable) {
    if (getLine() == null) {
      listenersEnabled = false;
      return;
    }

    if (enable != listenersEnabled) {
      if (enable) {
        getLine().addLineListener(this);
      } else {
        getLine().removeLineListener(this);
      }
      listenersEnabled = enable;
    }
  }

  /**
   * Diagnostic method that prints all available sound mixers and their supported audio formats to {@code stdout}.
   */
  public static void printMixerInfo() {
    final StringBuilder sb = new StringBuilder("Available sound mixers:\n");
    try {
      final Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();
      for (final Mixer.Info mi : mixersInfo) {
        final Mixer mixer = AudioSystem.getMixer(mi);
        sb.append("  Mixer: ").append(mixer).append('\n');
        Line.Info[] sourceLinesInfo = mixer.getSourceLineInfo();
        for (final Line.Info sli : sourceLinesInfo) {
          sb.append("    Line: ").append(sli).append('\n');
          if (sli instanceof SourceDataLine.Info) {
            final SourceDataLine.Info info = (SourceDataLine.Info)sli;
            final AudioFormat[] formats = info.getFormats();
            for (final AudioFormat af : formats) {
              sb.append("      Format: ").append(af).append('\n');
            }
          }
        }
      }
      System.out.println(sb);
    } catch (Throwable t) {
      System.out.println(sb);
      Logger.warn(t);
    }
  }
}
