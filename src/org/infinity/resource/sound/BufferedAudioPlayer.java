// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

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
   * None of the methods block execution. Playback state changes are propagated through {@link AudioStateEvent}s.
   * </p>
   */
  public class BufferedAudioPlayer implements BufferedAudioPlayback, LineListener {
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

    public BufferedAudioPlayer(AudioBuffer audioBuffer) throws Exception {
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
    public BufferedAudioPlayer(AudioBuffer audioBuffer, AudioStateListener listener) throws Exception {
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

    @Override
    public long getTotalLength() {
      if (isClosed()) {
        return 0L;
      }
      return audioClip.getMicrosecondLength() / 1_000L;
    }

    @Override
    public long getElapsedTime() {
      if (isClosed()) {
        return 0L;
      }
      long position = audioClip.getMicrosecondPosition() % audioClip.getMicrosecondLength();
      return position / 1_000L;
    }

    @Override
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
          setLooped();
        }
      } finally {
        setLineListenersEnabled(true);
      }
    }

    @Override
    public boolean isLooped() {
      return looped;
    }

    @Override
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

    @Override
    public boolean isPlaying() {
      return !isClosed() && playing;
    }

    /**
     * Starts or stops playback of audio data. Triggers an {@link AudioStateEvent} if playback is started or stopped.
     *
     * @param play Specify {@code true} to start playback or {@code false} to stop playback.
     */
    @Override
    public void setPlaying(boolean play) {
      if (play) {
        play();
      } else {
        stop();
      }
    }

    /**
     * Returns {@code true} if playback is paused. Enabling or disabled the paused state does not affect playback
     * activity.
     */
    @Override
    public boolean isPaused() {
      return isPlaying() && paused;
    }

    /**
     * Enters or leaves paused state when playback is active. Does nothing is playback is stopped.
     * Triggers an {@link AudioStateEvent} if playback is paused or resumed.
     *
     * @param pause Specify {@code true} to pause current playback or {@code false} to resume playback.
     */
    @Override
    public void setPaused(boolean pause) {
      if (pause) {
        pause();
      } else {
        resume();
      }
    }

    @Override
    public boolean isClosed() {
      return closed;
    }

    /** Closes the player and releases all resources. A closed audio player does not accept new playback commands. */
    @Override
    public void close() throws Exception {
      if (isClosed()) {
        return;
      }

      closed = true;
      playing = false;
      paused = false;

      // removing listeners
      final AudioStateListener[] items = getAudioStateListeners();
      for (int i = items.length - 1; i >= 0; i--) {
        removeAudioStateListener(items[i]);
      }

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

    /** Use internally after each call {@link Clip#start()} to set up looping mode. */
    private void setLooped() {
      if (isClosed()) {
        return;
      }
      if (isPlaying() && !isPaused()) {
        audioClip.loop(isLooped() ? Clip.LOOP_CONTINUOUSLY : 0);
      }
    }

    /** Starts playback of the associated audio data. Does nothing if the player is closed or already playing. */
    private void play() {
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
     * stopped playback.
     */
    private void stop() {
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
     * Pauses active playback. Does nothing if the player is closed, playback is not active, or already in the paused
     * state.
     */
    private void pause() {
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
     * paused state.
     */
    private void resume() {
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