// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import org.tinylog.Logger;

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer {
  private final byte[] buffer = new byte[8196];

  private AudioFormat audioFormat;
  private SourceDataLine dataLine;
  private boolean playing = true;
  private boolean paused = false;
  private boolean stopped = true;

  /**
   * Starts playback of audio data associated with the specified audio buffer and closes the audio line when
   * playback ends.
   *
   * @param audioBuffer AudioBuffer object containing audio data.
   * @throws Exception On error
   */
  public void play(AudioBuffer audioBuffer) throws Exception {
    play(audioBuffer, false);
  }

  /**
   * Starts playback of audio data associated with the specified audio buffer and keeps the audio line open for
   * additional sound data.
   *
   * @param audioBuffer AudioBuffer object containing audio data.
   * @throws Exception On error
   */
  public void playContinuous(AudioBuffer audioBuffer) throws Exception {
    play(audioBuffer, true);
  }

  /**
   * Starts playback of audio data associated with the specified audio buffer.
   *
   * @param audioBuffer AudioBuffer object containing audio data.
   * @param continuous Whether the audio line should be kept open for additional sound data.
   * @throws Exception On error
   */
  private void play(AudioBuffer audioBuffer, boolean continuous) throws Exception {
    if (audioBuffer == null || audioBuffer.getAudioData() == null) {
      return;
    }

    setPlaying(true);
    setStopped(false);
    try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBuffer.getAudioData()))) {
      if (dataLine == null || !ais.getFormat().matches(audioFormat)) {
        audioFormat = ais.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
          throw new UnsupportedAudioFileException("Unsupported audio format");
        }
        dataLine = (SourceDataLine) AudioSystem.getLine(info);
        dataLine.open(ais.getFormat(), 16384);
        dataLine.start();
      }

      while (isPlaying()) {
        if (!isPaused()) {
          int numBytesRead = ais.read(buffer, 0, buffer.length);
          if (numBytesRead < 0) {
            if (!continuous) {
              dataLine.drain();
            }
            break;
          }
          dataLine.write(buffer, 0, numBytesRead);
        } else {
          Thread.sleep(25L);
        }
      }
    } catch (Exception e) {
      setStopped(true);
      throw e;
    }

    if (!isPlaying()) {
      dataLine.drain();
    }

    setStopped(true);
  }

  /**
   * Returns whether the player is initialized to play back sound.
   */
  public boolean isRunning() {
    return dataLine != null && dataLine.isOpen();
  }

  /**
   * Sets the current playback mode to "pause" or "playing", depending on the specified parameter.
   */
  public void setPaused(boolean pause) {
    if (paused != pause) {
      paused = pause;
      if (pause) {
        dataLine.stop();
      } else {
        dataLine.start();
      }
    }
  }

  /**
   * Returns whether the player is in pause mode.
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Stops audio playback.
   */
  public void stopPlay() {
    setPlaying(false);
    while (!isStopped()) {
      try {
        Thread.sleep(50L);
      } catch (InterruptedException e) {
        Logger.trace(e);
      }
    }
    try {
      Thread.sleep(150L);
    } catch (InterruptedException e) {
      Logger.trace(e);
    }
    setStopped(true);
    if (dataLine != null && dataLine.isOpen()) {
      dataLine.close();
    }
    dataLine = null;
  }

  /**
   * Returns the {@link DataLine} instances of the player, or {@code null} if the player has not been initialized.
   */
  public DataLine getDataLine() {
    return dataLine;
  }

  private synchronized void setPlaying(boolean b) {
    if (b != playing) {
      playing = b;
      if (!b) {
        paused = false;
      }
    }
  }

  private boolean isPlaying() {
    return playing;
  }

  private synchronized void setStopped(boolean b) {
    if (b != stopped) {
      stopped = b;
    }
  }

  private boolean isStopped() {
    return stopped;
  }
}
