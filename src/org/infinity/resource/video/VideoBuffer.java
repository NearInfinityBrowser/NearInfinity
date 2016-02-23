// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.awt.Image;

/**
 * Methods required to work with chained image buffers.
 * @author argent77
 */
public interface VideoBuffer
{
  /**
   * Returns the buffer that is visible on screen.
   * @return The BufferedImage object of the currently visible image.
   * @see #backBuffer()
   */
  public Image frontBuffer();

  /**
   * Returns the buffer that is prepared to be shown.
   * @return The BufferedImage object of the image in preparation.
   * @see #frontBuffer()
   */
  public Image backBuffer();

  /**
   * The buffer chain advances one step forward (e.g. in a double buffered chain, the
   * front buffer becomes the back buffer and vice versa).
   * Optional data attached to the front buffer should be discarded.
   */
  public void flipBuffers();

  /**
   * Returns the number of video buffers in the buffer chain.
   * @return Number of video buffers in the buffer chain.
   */
  public int bufferCount();

  /**
   * Attaches the specified data object to the current back buffer.
   * <b>Note:</b> The object's life time should be limited to one full cycle of the buffer chain.
   * @param data The data object that will be attached to the current back buffer.
   * @see #fetchData()
   */
  public void attachData(Object data);

  /**
   * Returns the data object associated with the current front buffer.
   * The data should be discarded automatically with the next call of {@link #flipBuffers()}.
   * @return The data object associated with the current front buffer,
   *         or <code>null</code> if no data is available.
   * @see #attachData(Object)
   */
  public Object fetchData();
}
