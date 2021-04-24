// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Represents a container for audio chunks returned by the MveDecoder.
 */
public class AudioQueue
{
  private final Deque<AudioBlock> deque;

  /**
   * Constructs an empty audio queue.
   */
  public AudioQueue()
  {
    deque = new ArrayDeque<>();
  }

  /**
   * Returns whether one or more audio blocks are available.
   * @return {@code true} if one or more audio blocks are available, {@code false} otherwise.
   */
  public synchronized boolean hasNext()
  {
    return !deque.isEmpty();
  }

  /**
   * Returns the index of the next audio block without actually removing it.
   * @return The index of the next audio block, or -1 if no audio block is available.
   */
  public synchronized int peekNextIndex()
  {
    return hasNext() ? deque.peekFirst().index : -1;
  }

  /**
   * Returns the size of the next audio block without actually removing it.
   * @return The size in bytes of the next audio block, or -1 if no audio block is available.
   */
  public synchronized int peekNextDataSize()
  {
    return hasNext() ? deque.peekFirst().data.length : -1;
  }

  /**
   * Returns the next available audio block without actually removing it.
   * @return The next available audio block as byte array, or {@code null} otherwise.
   */
  public synchronized byte[] peekNextData()
  {
    return hasNext() ? deque.peekFirst().data : null;
  }

  /**
   * Returns the next available audio block.
   * @return The next available audio block as byte array, or {@code null} otherwise.
   */
  public synchronized byte[] getNextData()
  {
    return hasNext() ? deque.pollFirst().data : null;
  }

  /**
   * Blocks the current thread until an audio block is available.
   * @return The next available audio block as byte array.
   */
  public byte[] getNextDataWait()
  {
    while (!hasNext()) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
      }
    }
    synchronized (deque) {
      return deque.pollFirst().data;
    }
  }

  /**
   * Removes the next available block in the queue.
   * @return {@code true} if the next block in the queue has been discarded,
   *         {@code false} if no blocks were available.
   */
  public boolean skipNext()
  {
    if (hasNext()) {
      deque.pollFirst();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Removes all remaining audio blocks from the queue.
   */
  public synchronized void clear()
  {
    deque.clear();
  }

  /**
   * Adds a new audio block to the queue. Note: This is done automatically by the decoder.
   * @param index The index of the audio block
   * @param data The audio data
   * @return {@code true} if a valid block has been added successfully, {@code false} otherwise.
   */
  public synchronized boolean addAudioBlock(int index, byte[] data)
  {
    if (index >= 0 && data != null) {
      try {
        deque.addLast(new AudioBlock(index, data));
        return true;
      } catch (Throwable t) {
      }
    }
    return false;
  }

//----------------------------- INNER CLASSES -----------------------------

  private class AudioBlock
  {
    public final int index;     // sequential number, can be used to sort audio blocks
    public final byte[] data;   // uncompressed audio data in the globally specified audio format

    public AudioBlock(int index, byte[] data)
    {
      this.index = index;
      this.data = data;
    }
  }
}
