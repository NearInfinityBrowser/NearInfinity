// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.tinylog.Logger;

/**
 * A class for providing smooth playback of streamed audio data.
 * <p>
 * None of the methods block execution. Playback state changes are propagated through {@link AudioStateEvent}s.
 * </p>
 */
public class StreamingAudioPlayer extends AbstractAudioPlayer implements StreamingAudioPlayback {
  private final ConcurrentLinkedQueue<AudioBuffer> audioBufferQueue = new ConcurrentLinkedQueue<>();
  private final Runner runner = new Runner();

  private AudioFormat audioFormat;
  private SourceDataLine dataLine;
  private byte[] bufferBytes;

  private boolean closed;
  private boolean playing;
  private boolean paused;

  /** Marks pause/resume state changes internally to fire correct state events. */
  private boolean pauseResume;

  /**
   * Creates a new audio player and initializes it with the specified audio buffers.
   *
   * @param audioBuffers Optional {@link AudioBuffer} instances to load into the playback queue.
   * @throws NullPointerException          if {@code audioBuffer} is {@code null}.
   * @throws IOException                   if an I/O error occurs.
   * @throws UnsupportedAudioFileException if the audio data is incompatible with the player.
   * @throws LineUnavailableException      if the audio line is not available due to resource restrictions.
   * @throws IllegalArgumentException      if the audio data is invalid.
   */
  public StreamingAudioPlayer(AudioBuffer... audioBuffers) throws Exception {
    this(null, audioBuffers);
  }

  /**
   * Creates a new audio player and initializes it with the specified audio buffers.
   *
   * @param listener     {@link AudioStateListener} that receives audio state changes.
   * @param audioBuffers Optional {@link AudioBuffer} instances to load into the playback queue.
   * @throws NullPointerException          if {@code audioBuffer} is {@code null}.
   * @throws IOException                   if an I/O error occurs.
   * @throws UnsupportedAudioFileException if the audio data is incompatible with the player.
   * @throws LineUnavailableException      if the audio line is not available due to resource restrictions.
   * @throws IllegalArgumentException      if the audio data is invalid.
   */
  public StreamingAudioPlayer(AudioStateListener listener, AudioBuffer... audioBuffers) throws Exception {
    super();
    addAudioStateListener(listener);
    init();
    for (final AudioBuffer ab : audioBuffers) {
      addAudioBuffer(ab);
    }
  }

  @Override
  public boolean isAudioQueueEmpty() {
    return audioBufferQueue.isEmpty();
  }

  @Override
  public void clearAudioQueue() {
    audioBufferQueue.clear();
  }

  @Override
  public void addAudioBuffer(AudioBuffer audioBuffer) {
    if (audioBuffer == null) {
      throw new NullPointerException("audioBuffer is null");
    }
    if (audioBuffer.getAudioData() == null) {
      throw new NullPointerException("audio data is null");
    }
    if (audioBuffer.getAudioData().length == 0) {
      throw new IllegalArgumentException("No audio data");
    }

    audioBufferQueue.offer(audioBuffer);
    runner.signalBufferAvailable();
  }

  @Override
  public boolean removeAudioBuffer(AudioBuffer audioBuffer) {
    return audioBufferQueue.remove(audioBuffer);
  }

  // --------------------- Begin Interface AudioPlayback ---------------------

  @Override
  public long getSoundPosition() {
    if (isClosed() || dataLine == null) {
      return 0L;
    }
    long position = dataLine.getMicrosecondPosition();
    return position / 1000L;
  }

  @Override
  public boolean isPlaying() {
    return !isClosed() && playing;
  }

  @Override
  public void setPlaying(boolean play) {
    if (isClosed()) {
      return;
    }

    if (play) {
      play();
    } else {
      stop();
    }
  }

  @Override
  public boolean isPaused() {
    return isPlaying() && paused;
  }

  @Override
  public void setPaused(boolean pause) {
    if (isClosed()) {
      return;
    }

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

  // --------------------- End Interface AudioPlayback ---------------------

  // --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception {
    if (isClosed()) {
      return;
    }

    closed = true;
    playing = false;
    paused = false;

    runner.close();
    dataLine.stop();
    dataLine.flush();
    dataLine.close();

    dataLine = null;
    audioFormat = null;
    bufferBytes = null;

    // removing listeners
    final AudioStateListener[] items = getAudioStateListeners();
    for (int i = items.length - 1; i >= 0; i--) {
      removeAudioStateListener(items[i]);
    }
  }

  // --------------------- End Interface Closeable ---------------------

  // --------------------- Begin Interface LineListener ---------------------

  @Override
  public void update(LineEvent event) {
    if (event.getType() == LineEvent.Type.START) {
      if (!pauseResume) {
        firePlaybackStarted();
      } else {
        pauseResume = false;
      }
    } else if (event.getType() == LineEvent.Type.STOP) {
      if (!pauseResume) {
        firePlaybackStopped();
      } else {
        pauseResume = false;
      }
    } else if (event.getType() == LineEvent.Type.OPEN) {
      firePlayerOpened();
    } else if (event.getType() == LineEvent.Type.CLOSE) {
      firePlayerClosed();
    }
  }

  // --------------------- End Interface LineListener ---------------------

  @Override
  protected SourceDataLine getLine() {
    return dataLine;
  }

  /** Fires when the current audio buffer contains no more data. */
  protected void fireBufferEmpty() {
    fireAudioStateEvent(AudioStateEvent.State.BUFFER_EMPTY, !isAudioQueueEmpty());
  }

  private void play() {
    if (isClosed() || isPlaying()) {
      return;
    }

    if (dataLine.isRunning()) {
      dataLine.stop();
      dataLine.flush();
    }

    playing = true;
    paused = false;
    dataLine.start();
    runner.signal();
  }

  private void stop() {
    if (isClosed() || !isPlaying()) {
      return;
    }

    final boolean isPaused = isPaused();
    playing = false;
    paused = false;
    if (isPaused) {
      // Pause mode is technically "stop" mode, so we need to trigger a "STOP" event manually
      SwingUtilities
          .invokeLater(() -> update(new LineEvent(dataLine, LineEvent.Type.STOP, dataLine.getLongFramePosition())));
    } else {
      dataLine.stop();
    }
    dataLine.flush();
    runner.signal();
  }

  private void pause() {
    if (isClosed() || !isPlaying() || isPaused()) {
      return;
    }

    pauseResume = true;
    paused = true;
    dataLine.stop();
    firePlaybackPaused();
  }

  private void resume() {
    if (isClosed() || !isPlaying() || !isPaused()) {
      return;
    }

    pauseResume = true;
    paused = false;
    dataLine.start();
    runner.signal();
    firePlaybackResumed();
  }

  /**
   * Creates and returns a new {@link AudioInputStream} instance from the next available {@link AudioBuffer} in the
   * audio queue.
   *
   * @return an {@link AudioInputStream} instance that can be fed directly to the source data line.
   * @throws UnsupportedAudioFileException if the stream does not point to valid audio file data recognized by the
   *                                         system.
   * @throws LineUnavailableException      if a matching source data line is not available due to resource restrictions.
   * @throws IllegalArgumentException      if the audio buffer data is not compatible with the source data line.
   * @throws EmptyQueueException           if no further audio buffer is queued.
   * @throws IOException                   if an I/O exception occurs.
   */
  private AudioInputStream pollAudioBuffer() throws Exception {
    final AudioBuffer audioBuffer = audioBufferQueue.poll();
    if (audioBuffer != null) {
      final AudioInputStream sourceStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBuffer.getAudioData()));
      if (!AudioSystem.isConversionSupported(audioFormat, sourceStream.getFormat())) {
        throw new IllegalArgumentException("Incompatible audio format: " + sourceStream.getFormat());
      }
      return AudioSystem.getAudioInputStream(audioFormat, sourceStream);
    } else {
      throw new EmptyQueueException("No audio buffer available");
    }
  }

  /**
   * Initializes and opens the audio device for playback.
   *
   * @throws UnsupportedAudioFileException if the stream does not point to valid audio file data recognized by the
   *                                         system.
   * @throws LineUnavailableException      if a matching source data line is not available due to resource restrictions.
   * @throws IllegalArgumentException      if the system does not support at least one source data line supporting
   *                                         the specified audio format through any installed mixer.
   * @throws IOException                   if an I/O exception occurs.
   */
  private void init() throws Exception {
    try {
      audioFormat = getCompatibleAudioFormat();
      if (audioFormat == null) {
        throw new LineUnavailableException("Could not find compatible audio format");
      }

      int bufferSize = getBufferSize(audioFormat, 100);
      dataLine = AudioSystem.getSourceDataLine(audioFormat);
      dataLine.addLineListener(this);
      dataLine.open(audioFormat, bufferSize);

      // allocated buffer size may differ from requested buffer size
      bufferSize = dataLine.getBufferSize();
      bufferBytes = new byte[bufferSize];
    } catch (Exception e) {
      closed = true;
      throw e;
    }
  }

  /**
   * Attempts to find an audio format that can be universally used to play back streamed audio.
   *
   * @return {@code AudioFormat} definition that is compatible with the default {@link SourceDataLine}, {@code null}
   *         otherwise.
   */
  private static AudioFormat getCompatibleAudioFormat() {
    AudioFormat format = null;

    // arrays of sensible values, sorted by usability in descending order
    final boolean[] bigEndianArray = { false, true };
    final AudioFormat.Encoding[] encodingArray = { AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED,
        AudioFormat.Encoding.PCM_FLOAT };
    final int[] channelsArray = { 2, 1 };
    final int[] sampleBitsArray = { 16, 32, 8 };
    final float[] sampleRateArray = { 48000.0f, 44100.0f, 32000.0f, 24000.0f, 22050.0f };

    // for-loops sorted by importance in ascending order (innermost loop: highest importance)
    out:  // label for outermost loop: saves us to put conditional breaks in any of the nested loops
    for (final boolean bigEndian : bigEndianArray) {
      for (final AudioFormat.Encoding encoding : encodingArray) {
        for (final int channels : channelsArray) {
          for (final int sampleBits : sampleBitsArray) {
            for (final float sampleRate : sampleRateArray) {
              final int frameSize = sampleBits * channels / 8;
              final float frameRate = sampleRate * frameSize;
              final AudioFormat f = new AudioFormat(encoding, sampleRate, sampleBits, channels, frameSize, frameRate,
                  bigEndian);
              final DataLine.Info info = new DataLine.Info(SourceDataLine.class, f);
              if (AudioSystem.isLineSupported(info)) {
                format = f;
                break out;
              }
            }
          }
        }
      }
    }
    return format;
  }

  /**
   * Calculates the buffer size to hold {@code lagMs} millisecond worth of audio data.
   *
   * @param format {@link AudioFormat} of the data.
   * @param lagMs  Storage capacity of the internal audio buffer, in milliseconds. Supported range: 10 to 1000
   * @return Buffer size in bytes. Buffer size always represents an integral number of audio frames. Returns a default
   *         buffer size if the audio format could not be determined.
   */
  private static int getBufferSize(AudioFormat format, int lagMs) {
    int retVal = 0x4000;

    if (format != null) {
      // allowed sound lag: 10..1000 ms
      lagMs = Math.max(10, Math.min(1000, lagMs));
      int bufSize = (int)(format.getFrameRate() * format.getFrameSize() * lagMs / 1000.0f);
      retVal = Math.max(0x100, Math.min(0x80000, bufSize));
      if (retVal % format.getFrameSize() != 0) {
        retVal = (retVal / format.getFrameSize() * format.getFrameSize()) + format.getFrameSize();
      }
    }

    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * Handles feeding audio data to the source line without blocking the main execution thread.
   */
  private class Runner implements Runnable {
    private final AtomicBoolean waitingForData = new AtomicBoolean();
    private final Thread thread;

    boolean running;

    public Runner() {
      running = true;
      thread = new Thread(this);
      thread.start();
    }

    /** Returns whether the background task is active. */
    public boolean isRunning() {
      return running;
    }

    /** Signals the runner to terminate the background task. */
    public void close() {
      running = false;
      signal();
    }

    /** Specialized signal that is triggered only if the background task waits for more audio data to play back. */
    public void signalBufferAvailable() {
      if (isRunning() && waitingForData.get()) {
        signal();
      }
    }

    /** Signals the background task to reevaluate state changes. */
    public void signal() {
      if (isRunning()) {
        thread.interrupt();
      }
    }

    /** Puts the current thread to sleep until interrupted. */
    private void sleep() {
      sleep(Long.MAX_VALUE);
    }

    /** Puts the current thread to sleep until interrupted or the specified time has passed. */
    private void sleep(long millis) {
      try {
        Thread.sleep(Math.max(0, millis));
      } catch (InterruptedException e) {
        // waking up
      }
    }

    @Override
    public void run() {
      while (isRunning()) {
        if (isPlaying()) {
          try (final AudioInputStream ais = pollAudioBuffer()) {
            int readBytes = -1;
            while (isPlaying() && (readBytes = ais.read(bufferBytes)) != -1) {
              final int numWritten = dataLine.write(bufferBytes, 0, readBytes);

              if (isPaused()) {
                sleep();
                if (isPlaying() && !isPaused() && numWritten < readBytes) {
                  // prevent clicks or "hickups" in audio playback
                  dataLine.write(bufferBytes, numWritten, readBytes - numWritten);
                }
              }
            }

            if (readBytes == -1) {
              fireBufferEmpty();
            }
          } catch (EmptyQueueException e) {
            // no audio queued
            fireBufferEmpty();
          } catch (Exception e) {
            fireError(e);
            Logger.error(e);
          }

          if (isPlaying() && !isPaused() && isAudioQueueEmpty()) {
            // waiting for more audio data or a state change
            waitingForData.set(true);
            sleep();
            waitingForData.set(false);
          }
        } else {
          sleep();
        }
      }
    }
  }
}
