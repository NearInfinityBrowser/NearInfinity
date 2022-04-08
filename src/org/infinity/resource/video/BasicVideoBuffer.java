// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.awt.Image;
import java.util.Arrays;
import java.util.Collection;

import org.infinity.resource.graphics.ColorConvert;

/**
 * Basic implementation of the BufferedRenderer interface.
 */
public class BasicVideoBuffer implements VideoBuffer {
  private Image[] buffer;
  private Object[] extraData;
  private int numBuffers;
  private int currentBuffer;

  /**
   * Creates an empty buffer chain.
   */
  public BasicVideoBuffer() {
    buffer = null;
    extraData = null;
  }

  /**
   * Creates a new BufferedRenderer with the specified parameters.
   *
   * @param numBuffers      The number of buffers in the buffer chain (usually 1 to 3).
   * @param width           The image width in pixels.
   * @param height          The image height in pixels.
   * @param hasTransparency Enable/Disable transparency/alpha support.
   */
  public BasicVideoBuffer(int numBuffers, int width, int height, boolean hasTransparency) {
    buffer = null;
    extraData = null;
    if (!create(numBuffers, width, height, hasTransparency)) {
      throw new NullPointerException();
    }
  }

  /**
   * Creates a BufferedRenderer using the specified BufferedImage objects for the buffer chain.
   *
   * @param buffers The array of BufferedImage objects to use.
   */
  public BasicVideoBuffer(Image[] buffers) {
    buffer = null;
    extraData = null;
    if ((buffers == null) || !create(Arrays.asList(buffers))) {
      throw new NullPointerException();
    }
  }

  /**
   * Creates a BufferedRenderer using the specified BufferedImage objects for the buffer chain.
   *
   * @param buffers The collection of BufferedImage objects to use.
   */
  public BasicVideoBuffer(Collection<Image> buffers) {
    buffer = null;
    extraData = null;
    if (!create(buffers)) {
      throw new NullPointerException();
    }
  }

  @Override
  public Image frontBuffer() {
    return (buffer != null) ? buffer[currentBuffer] : null;
  }

  @Override
  public Image backBuffer() {
    return (buffer != null) ? buffer[(currentBuffer + numBuffers - 1) % numBuffers] : null;
  }

  @Override
  public void flipBuffers() {
    if (buffer != null) {
      currentBuffer = (currentBuffer + 1) % numBuffers;
      extraData[(currentBuffer + numBuffers - 1) % numBuffers] = null;
    }
  }

  @Override
  public int bufferCount() {
    return numBuffers;
  }

  @Override
  public void attachData(Object data) {
    if (extraData != null) {
      int bufferIndex = (currentBuffer + numBuffers - 1) % numBuffers;
      extraData[bufferIndex] = data;
    }
  }

  @Override
  public Object fetchData() {
    if (extraData != null) {
      return extraData[currentBuffer];
    } else {
      return null;
    }
  }

  /**
   * Initializes the buffer chain using the specified parameters. Old buffers will be discarded.
   *
   * @param numBuffers      The number of buffers in the buffer chain (usually 1 to 3).
   * @param width           The image width in pixels
   * @param height          The image height in pixels
   * @param hasTransparency Enable/Disable transparency/alpha support.
   * @return {@code true} if the new buffer chain has been created successfully, {@code false} otherwise.
   */
  public boolean create(int numBuffers, int width, int height, boolean hasTransparency) {
    release();

    if (width <= 0 || height <= 0) {
      return false;
    }
    if (numBuffers < 1) {
      numBuffers = 1;
    }

    this.numBuffers = numBuffers;
    buffer = new Image[this.numBuffers];
    extraData = new Object[this.numBuffers];
    currentBuffer = 0;
    for (int i = 0; i < this.numBuffers; i++) {
      if (width > 0 && height > 0) {
        buffer[i] = ColorConvert.createCompatibleImage(width, height, hasTransparency);
      } else {
        buffer[i] = null;
      }
      extraData[i] = null;
    }

    return (width > 0 && height > 0);
  }

  /**
   * Initializes the buffer chain using the BufferedImage objects from the specified list. Old buffers will be
   * discarded.
   *
   * @param bufferList The collection of BufferedImage objects.
   * @return {@code true} if the new buffer chain has been created successfully, {@code false} otherwise.
   */
  public boolean create(Collection<Image> bufferList) {
    release();

    if (bufferList == null) {
      throw new NullPointerException();
    }

    // check for valid content
    int num = 0;
    for (final Image image : bufferList) {
      if (image != null) {
        num++;
      }
    }

    // add buffers to chain
    if (num > 0) {
      this.numBuffers = num;
      buffer = new Image[this.numBuffers];
      currentBuffer = 0;
      num = 0;
      for (final Image image : bufferList) {
        if (image != null) {
          buffer[num++] = image;
        }
      }
      extraData = new Object[this.numBuffers];
      for (num = 0; num < this.numBuffers; num++) {
        extraData[num] = null;
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Releases old BufferedImage objects.
   */
  public void release() {
    if (buffer != null && extraData != null) {
      for (int i = 0; i < numBuffers; i++) {
        if (buffer[i] != null) {
          buffer[i].flush();
        }
        buffer[i] = null;
        extraData[i] = null;
      }
      buffer = null;
      extraData = null;
    }
  }
}
