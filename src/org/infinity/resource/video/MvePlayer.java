// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class MvePlayer
{
  private boolean playing, paused, stopped;
  private long startTime, delayTime;
  private AudioQueue audioQueue;
  private SourceDataLine dataLine;

  public MvePlayer()
  {
    playing = true;
    paused = false;
    stopped = true;
    dataLine = null;
    audioQueue = new AudioQueue();
  }

  public void play(ImageRenderer renderer, MveDecoder decoder) throws Exception
  {
    if (renderer == null || renderer.bufferCount() <= 0 || decoder == null || !decoder.isOpen())
      return;

    setPlaying(true);
    setStopped(false);
    setPaused(false);

    // initializing
    initAudio(decoder);
    decoder.setDefaultAudioOutput(audioQueue);

    // preloading frames
    for (int i = 0; i < renderer.bufferCount() && decoder.hasNextFrame(); i++) {
      if (decoder.processNextFrame()) {
        // attached frame data contains current frame delay as Integer object
        renderer.attachData(decoder.getFrameDelay());
        if (decoder.audioInitialized())
          initAudio(decoder);
      } else {
        decoder.setDefaultAudioOutput(null);
        stopPlay();
        throw new Exception("Error decoding video data");
      }
    }

    dataLine.start();
    outputAudioFrame(true);
    renderer.updateRenderer();
    while (isPlaying() && decoder.hasNextFrame()) {
      Object dataObj = renderer.fetchData();
      if (dataObj instanceof Integer) {
        setTimerDelay(((Integer)dataObj).longValue() * 1000L + timeRemaining());
      } else {
        // audio-only frames do not contain timing information
        setTimerDelay(0L);
      }

      if (decoder.processNextFrame()) {
        renderer.attachData(decoder.getFrameDelay());
        // has audio been re-initialized?
        if (decoder.audioInitialized()) {
          setTimerDelay(0L);
          initAudio(decoder);
          dataLine.start();
          outputAudioFrame(true);
          // flushing video buffer chain
          for (int i = 0; i < renderer.bufferCount(); i++)
            renderer.flipBuffers();
        }
      } else {
        decoder.setDefaultAudioOutput(null);
        stopPlay();
        throw new Exception("Error decoding video data");
      }

      // skip audio-only frames
      if (timeRemaining() == 0L)
        continue;

      outputAudioFrame(false);

      // has playback been paused?
      if (isPaused()) {
        dataLine.stop();
        dataLine.flush();
        while (isPlaying() && isPaused()) {
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
          }
        }
        dataLine.start();
        if (isPlaying()) {
          // reloading last audio block
          outputAudioFrame(true);
        }
        continue;
      }

      // waiting for the next frame to be displayed
      sleepUntil(5000000L);
      renderer.updateRenderer();
      audioQueue.skipNext();
    }

    // cleanup decoder related objects
    renderer.clearBuffers();
    decoder.setDefaultAudioOutput(null);
    audioQueue.clear();

    // clean up audio
    if (!isPlaying()) {
      dataLine.drain();
    }
    dataLine.stop();
    dataLine = null;

    setPlaying(false);
    setPaused(false);
    setStopped(true);
  }

  public void stopPlay()
  {
    setPlaying(false);
    while (!isStopped()) {
      try {
        Thread.sleep(50L);
      } catch (InterruptedException e) {
      }
    }
    try {
      Thread.sleep(100L);
    } catch (InterruptedException e) {
    }
  }

  public void pausePlay()
  {
    if (!isStopped()) {
      setPaused(true);
    }
  }

  public void continuePlay()
  {
    if (!isStopped()) {
      setPaused(false);
    }
  }

  public boolean isPlaying()
  {
    return playing;
  }

  public boolean isPaused()
  {
    return paused;
  }

  public boolean isStopped()
  {
    return stopped;
  }

  private synchronized void setStopped(boolean b)
  {
    if (b != stopped) {
      stopped = b;
    }
  }

  private synchronized void setPaused(boolean b)
  {
    if (b != paused) {
      paused = b;
    }
  }

  private synchronized void setPlaying(boolean b)
  {
    if (b != playing) {
      playing = b;
    }
  }


  private void setTimerDelay(long nanoseconds)
  {
    if (nanoseconds < 0L)
      nanoseconds = 0L;
    startTime = System.nanoTime() & Long.MAX_VALUE;
    delayTime = nanoseconds;
  }

  private long timeRemaining()
  {
    long res = 0L;
    long curTime = System.nanoTime() & Long.MAX_VALUE;
    if (curTime < startTime) {
      res = delayTime - (Long.MAX_VALUE - startTime + curTime);
    } else {
      res = delayTime - (curTime - startTime);
    }
    if (res < 0L)
      res = 0L;
    return res;
  }

  // waits until only 'remaining' time (in ns) of the current timer remains
  private void sleepUntil(long remaining)
  {
    if (timeRemaining() > 2000000L) {
      while (timeRemaining() > remaining) {
        // sleep as much as possible
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
        }
      }
    }
    if (timeRemaining() <= 2000000L) {
      while (timeRemaining() > remaining) {
        // waste remaining nanoseconds
      }
    }
  }

  private void initAudio(MveDecoder decoder) throws Exception
  {
    if (decoder != null) {
      // closing old source data line
      if (dataLine != null) {
        dataLine.drain();
        dataLine.stop();
        dataLine = null;
      }
      audioQueue.clear();

      // initializing new source data line
      AudioFormat audioFormat = decoder.getAudioFormat();
      try {
        dataLine = AudioSystem.getSourceDataLine(audioFormat);
      } catch (Exception e) {
        e.printStackTrace();
        throw new Exception("Unsupported audio format");
      }
      dataLine.open(audioFormat, 16384);
    }
  }

  private void outputAudioFrame(boolean autoRemove)
  {
    if (audioQueue.hasNext()) {
      byte[] audioBlock = autoRemove ? audioQueue.getNextData() : audioQueue.peekNextData();
      if (audioBlock != null) {
        dataLine.write(audioBlock, 0, audioBlock.length);
      }
    }
  }
}
