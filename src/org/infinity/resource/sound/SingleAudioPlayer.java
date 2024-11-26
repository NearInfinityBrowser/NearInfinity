// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.beans.PropertyChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.tinylog.Logger;

/**
   * A class for providing smooth and responsive playback of a single sound clip.
   * <p>
   * None of the methods block execution. Playback state changes are propagated through {@link PropertyChangeEvent}s.
   * </p>
   */
  public class SingleAudioPlayer implements AutoCloseable, LineListener {
    private final EventListenerList listenerList = new EventListenerList();

    private final AudioBuffer audioBuffer;
    private final Clip audioClip;

    private AudioInputStream audioStream;

    /** Indicates whether the audio player has been closed. */
    private boolean closed;
    /** Indicates whether playback of the audio player is active. Pausing playback doesn't affect this flag. */
    private boolean playing;
    /** Indicates whether playback is currently paused. Changing the paused state doesn't affect playback activity. */
    private boolean paused;
    /** Indicates whether playback is looped. */
    private boolean looped;
    private boolean listenersEnabled;

    public SingleAudioPlayer(AudioBuffer audioBuffer) throws Exception {
      this(audioBuffer, null);
    }

    /**
     * Creates a new audio player and initializes it with the specified audio buffer.
     *
     * @param audioBuffer {@link AudioBuffer} to load into the audio player.
     * @throws NullPointerException if {@code audioBuffer} is {@code null}.
     * @throws IOException if an I/O error occurs.
     * @throws UnsupportedAudioFileException if the audio data is incompatible with the player.
     * @throws LineUnavailableException  if the audio line is not available due to resource restrictions.
     * @throws IllegalArgumentException if the audio data is invalid.
     */
    public SingleAudioPlayer(AudioBuffer audioBuffer, AudioStateListener listener) throws Exception {
      this.audioBuffer = Objects.requireNonNull(audioBuffer);
      addAudioStateListener(listener);
      audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(this.audioBuffer.getAudioData()));
      final AudioFormat audioFormat = audioStream.getFormat();
      final DataLine.Info audioInfo = new DataLine.Info(Clip.class, audioFormat);
      audioClip = (Clip) AudioSystem.getLine(audioInfo);
      setLineListenersEnabled(true);
      audioClip.open(audioStream);
      audioClip.setLoopPoints(0, -1);
    }

    /** Returns the total length of the audio clip, in milliseconds. */
    public long getTotalLength() {
      if (isClosed()) {
        return 0L;
      }
      return audioClip.getMicrosecondLength() / 1_000L;
    }

    /** Returns the elapsed playback time of the current clip, in milliseconds. */
    public long getElapsedTime() {
      if (isClosed()) {
        return 0L;
      }
      long position = audioClip.getMicrosecondPosition() % audioClip.getMicrosecondLength();
      return position / 1_000L;
    }

    /**
     * Sets an explicit playback position.
     *
     * @param position New playback position in milliseconds. Position is clamped to the available audio clip duration.
     */
    public void setSoundPosition(long position) {
      if (isClosed()) {
        return;
      }

      position = Math.max(0L, Math.min(audioClip.getMicrosecondLength(), position * 1_000L));

      try {
        setLineListenersEnabled(false);
        final boolean isPlaying = isPlaying() && !isPaused();
        if (isPlaying) {
          audioClip.stop();
          audioClip.flush();
        }
        audioClip.setMicrosecondPosition(position);
        if (isPlaying) {
          audioClip.start();
        }
      } finally {
        setLineListenersEnabled(true);
      }
    }

    /** Returns whether loop mode is enabled. */
    public boolean isLooped() {
      return looped;
    }

    /**
     * Enables or disable looped playback.
     *
     * @param loop Indicates whether to loop playback.
     */
    public void setLooped(boolean loop) {
      if (isClosed()) {
        return;
      }

      if (isLooped() == loop) {
        return;
      }

      looped = loop;
      setLooped();
    }

    /** Use internally after each call {@link Clip#start()} to set up looping mode. */
    private void setLooped() {
      if (isClosed()) {
        return;
      }
      if (isPlaying()) {
        audioClip.loop(isLooped() ? Clip.LOOP_CONTINUOUSLY : 0);
      }
    }

    /** Returns {@code true} if playback is active. Pausing and resuming playback does not affect the result. */
    public boolean isPlaying() {
      return !isClosed() && playing;
    }

    /**
     * Starts playback of the associated audio data. Does nothing if the player is closed or already playing.
     * Triggers a {@link PropertyChangeEvent} with the name {@link #PROPERTY_NAME_START}.
     */
    public void play() {
      if (isClosed() || isPlaying()) {
        return;
      }

      synchronized (audioClip) {
        if (audioClip.isRunning()) {
          audioClip.stop();
          audioClip.flush();
        }
        playing = true;
        paused = false;
        audioClip.setFramePosition(0);
        audioClip.start();
        setLooped();
      }
    }

    /**
     * Stops active playback and sets position to the start of the clip. Does nothing if the player is closed or has
     * stopped playback. Triggers a {@link PropertyChangeEvent} with the name {@link #PROPERTY_NAME_STOP}.
     */
    public void stop() {
      if (isClosed() || !isPlaying()) {
        return;
      }

      synchronized (audioClip) {
        final boolean isPaused = isPaused();
        playing = false;
        paused = false;
        if (isPaused) {
          // Pause mode is technically "stop" mode, so we need to trigger a "STOP" event manually
          update(new LineEvent(audioClip, LineEvent.Type.STOP, audioClip.getLongFramePosition()));
        } else {
          audioClip.stop();
        }
        audioClip.flush();
        audioClip.setFramePosition(0);
      }
    }

    /**
     * Returns {@code true} if playback is paused. Enabling or disabled the paused state does not affect playback
     * activity.
     */
    public boolean isPaused() {
      return isPlaying() && paused;
    }

    /**
     * Pauses active playback. Does nothing if the player is closed, playback is not active, or already in the paused
     * state. Triggers a {@link PropertyChangeEvent} with the name {@link #PROPERTY_NAME_PAUSE}.
     */
    public void pause() {
      if (isClosed()) {
        return;
      }

      synchronized (audioClip) {
        if (isPlaying() && !isPaused()) {
          paused = true;
          audioClip.stop();
          firePlaybackPaused();
        }
      }
    }

    /**
     * Resumes previously paused playback. Does nothing if the player is closed, playback is not active, or not in the
     * paused state. Triggers a {@link PropertyChangeEvent} with the name {@link #PROPERTY_NAME_RESUME}.
     */
    public void resume() {
      if (isClosed()) {
        return;
      }

      synchronized (audioClip) {
        if (isPlaying() && isPaused()) {
          paused = false;
          audioClip.start();
          setLooped();
          firePlaybackResumed();
        }
      }
    }

    /** Returns whether the player has been closed. A closed player does not accept any playback commands. */
    public boolean isClosed() {
      return closed;
    }

    /**
     * Closes the player and releases any resources. Playback cannot be used anymore after calling this method.
     */
    @Override
    public void close() {
      if (isClosed()) {
        return;
      }

      closed = true;
      playing = false;
      paused = false;
      synchronized (audioClip) {
        if (audioClip.isRunning()) {
          audioClip.stop();
        }
        audioClip.flush();
        audioClip.close();
      }
      try {
        audioStream.close();
      } catch (IOException e) {
        Logger.warn(e);
      }
      audioStream = null;

      // removing listeners
      final AudioStateListener[] items = getAudioStateListeners();
      for (int i = items.length - 1; i >= 0; i--) {
        removeAudioStateListener(items[i]);
      }
    }

    @Override
    public void update(LineEvent event) {
      if (event.getType() == LineEvent.Type.START) {
        firePlaybackStarted();
      } else if (event.getType() == LineEvent.Type.STOP) {
        playing = paused;   // override if paused to keep state consistent
        firePlaybackStopped();
      } else if (event.getType() == LineEvent.Type.OPEN) {
        firePlayerOpened();
      } else if (event.getType() == LineEvent.Type.CLOSE) {
        firePlayerClosed();
      }
    }

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

    /** Returns whether {@link Line}'s status changes are tracked by this class instance. */
    @SuppressWarnings("unused")
    private boolean isLineListenersEnabled() {
      return listenersEnabled;
    }

    /** Specifies whether {@link Line}'s status changes should be tracked by this class instance. */
    private void setLineListenersEnabled(boolean enable) {
      if (enable != listenersEnabled) {
        if (enable) {
          audioClip.addLineListener(this);
        } else {
          audioClip.removeLineListener(this);
        }
        listenersEnabled = enable;
      }
    }

    /** Fires a {@link AudioStateListener} of the specified name to all registered listeners. */
    private void fireAudioStateEvent(AudioStateEvent.State state, Object value) {
      // collect and execute
      synchronized (listenerList) {
        final AudioStateListener[] listeners = listenerList.getListeners(AudioStateListener.class);
        if (listeners.length > 0) {
          final AudioStateEvent event = new AudioStateEvent(this, state, value);
          SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < listeners.length; i++)
              listeners[i].audioStateChanged(event);
          });
        }
      }
//      synchronized (listeners) {
//        listeners.clear();
//        final Object[] entries = listenerList.getListenerList();
//        AudioStateEvent evt = null;
//        for (int i = entries.length - 2; i >= 0; i -= 2) {
//          if (entries[i] == AudioStateListener.class) {
//            // event object is lazily created
//            if (evt == null) {
//              evt = new AudioStateEvent(this, state, value);
//            }
//            listeners.add((AudioStateListener) entries[i + 1]);
//          }
//        }
//
//        if (!listeners.isEmpty()) {
//          final AudioStateEvent event = evt;
//          SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.audioStateChanged(event)));
////          Threading.invokeInEventThread(e -> listeners.forEach(l -> l.audioStateChanged(e)), evt);
//        }
//      }
    }

    /** Fires when the current sound clip is released. */
    private void firePlayerOpened() {
      fireAudioStateEvent(AudioStateEvent.State.OPEN, null);
    }

    /** Fires when the current sound clip is released. */
    private void firePlayerClosed() {
      fireAudioStateEvent(AudioStateEvent.State.CLOSE, null);
    }

    /** Fires when playback is started. */
    private void firePlaybackStarted() {
      fireAudioStateEvent(AudioStateEvent.State.START, null);
    }

    /** Fires when playback is stopped. */
    private void firePlaybackStopped() {
      fireAudioStateEvent(AudioStateEvent.State.STOP, null);
    }

    /** Fires when playback is paused. */
    private void firePlaybackPaused() {
      fireAudioStateEvent(AudioStateEvent.State.PAUSE, Long.valueOf(getElapsedTime()));
    }

    /** Fires when paused playback is resumed. */
    private void firePlaybackResumed() {
      fireAudioStateEvent(AudioStateEvent.State.RESUME, Long.valueOf(getElapsedTime()));
    }
  }