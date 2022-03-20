// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

import org.infinity.gui.RenderCanvas;

/**
 * A component optimized to display rapidly changing graphics data (e.g. videos, animations), providing support for
 * scaling (with and without aspect ratio preservation) and filtering.
 */
public class ImageRenderer extends JComponent implements VideoBuffer, ComponentListener, SwingConstants {
  // interpolation types used in scaling
  public static final Object TYPE_NEAREST_NEIGHBOR  = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
  public static final Object TYPE_BILINEAR          = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
  public static final Object TYPE_BICUBIC           = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

  private final BasicVideoBuffer videoBuffer;
  private final RenderCanvas canvas;

  private boolean isAspect;

  /**
   * Creates an empty component. Call {@link #createBuffer(int, int, int)} to properly make use of the component's
   * features.
   */
  public ImageRenderer() {
    this(0, 0, 0);
  }

  /**
   * Creates a buffer chain with the specified parameters.
   *
   * @param numBuffers The number of buffers to create.
   * @param width      The new width of the buffer in pixels.
   * @param height     The new height of the buffer in pixels.
   */
  public ImageRenderer(int numBuffers, int width, int height) {
    setOpaque(false);
    setLayout(null);
    videoBuffer = new BasicVideoBuffer();
    canvas = new RenderCanvas();
    add(canvas);
    createBuffer(numBuffers, width, height);
    addComponentListener(this);
  }

  /**
   * Creates a new buffer chain with the specified dimensions. Old buffers will be discarded.
   *
   * @param numBuffers The number of buffers to create.
   * @param width      The new width of the buffer in pixels.
   * @param height     The new height of the buffer in pixels.
   * @return {@code true} if the buffer chain has been created successfully, {@code false} otherwise.
   */
  public boolean createBuffer(int numBuffers, int width, int height) {
    if (numBuffers > 0 && width > 1 && height > 1) {
      boolean res = videoBuffer.create(numBuffers, width, height, false);
      if (res) {
        updateDefaultSize();
        updateCanvasBounds();
        updateRenderer();
      }
      return res;
    }
    return false;
  }

  /**
   * Gets the alignment of the component's content along the X axis.
   *
   * @return One of the following constants defined in SwingConstants: LEFT, CENTER or RIGHT.
   */
  public int getHorizontalAlignment() {
    return canvas.getHorizontalAlignment();
  }

  /**
   * Sets the alignment of the component's content along the X axis. The default property is CENTER.
   *
   * @param alignment One of the following constants defined in SwingConstants: LEFT, CENTER or RIGHT.
   */
  public void setHorizontalAlignment(int alignment) {
    if (alignment != canvas.getHorizontalAlignment()) {
      canvas.setHorizontalAlignment(alignment);
      updateCanvasBounds();
    }
  }

  /**
   * Gets the alignment of the component's content along the Y axis.
   *
   * @return One of the following constants defined in SwingConstants: TOP, CENTER or BOTTOM.
   */
  public int getVerticalAlignment() {
    return canvas.getVerticalAlignment();
  }

  /**
   * Sets the alignment of the component's content along the Y axis. The default property is CENTER.
   *
   * @param alignment One of the following constants defined in SwingConstants: TOP, CENTER or BOTTOM.
   */
  public void setVerticalAlignment(int alignment) {
    if (alignment != canvas.getVerticalAlignment()) {
      canvas.setVerticalAlignment(alignment);
      updateCanvasBounds();
    }
  }

  /**
   * Returns the width of the image buffers set in the constructor or {@code createBuffer()} method.
   *
   * @return Image buffer's width in pixels.
   */
  public int getBufferWidth() {
    if (videoBuffer.frontBuffer() != null) {
      return videoBuffer.frontBuffer().getWidth(null);
    } else {
      return 0;
    }
  }

  /**
   * Returns the height of the image buffers set in the constructor or {@code createBuffer()} method.
   *
   * @return Image buffer's height in pixels.
   */
  public int getBufferHeight() {
    if (videoBuffer.frontBuffer() != null) {
      return videoBuffer.frontBuffer().getHeight(null);
    } else {
      return 0;
    }
  }

  /**
   * Returns whether scaling has been activated.
   *
   * @return {@code true} if scaling has been activated.
   */
  public boolean getScalingEnabled() {
    return canvas.isScalingEnabled();
  }

  /**
   * Sets whether the image buffer's content should be drawn scaled.
   *
   * @param enable {@code true} to enable scaling.
   */
  public void setScalingEnabled(boolean enable) {
    if (enable != canvas.isScalingEnabled()) {
      canvas.setScalingEnabled(enable);
      updateDefaultSize();
      updateCanvasBounds();
    }
  }

  /**
   * Returns the interpolation type used when scaling has been enabled.
   *
   * @return The interpolation type.
   */
  public Object getInterpolationType() {
    return canvas.getInterpolationType();
  }

  /**
   * Specify the interpolation type used when scaling has been enabled. One of TYPE_NEAREST_NEIGHBOR, TYPE_BILINEAR and
   * TYPE_BICUBIC (Default: TYPE_NEAREST_NEIGHBOR).
   *
   * @param interpolationType The new interpolation type to set.
   */
  public void setInterpolationType(Object interpolationType) {
    canvas.setInterpolationType(interpolationType);
  }

  /**
   * Returns whether the aspect ratio of the displayed image will be preserved.
   *
   * @return {@code true} if aspect ratio preservation has been enabled, {@code false} otherwise.
   */
  public boolean getAspectRatioEnabled() {
    return isAspect;
  }

  /**
   * Sets whether the aspect ratio of the displayed graphics should be preserved. This flag is used in conjunction with
   * with {@link #setScalingEnabled(boolean)}.
   *
   * @param enable Set to {@code true} if aspect ratio should be preserved.
   */
  public void setAspectRatioEnabled(boolean enable) {
    if (enable != isAspect) {
      isAspect = enable;
      if (canvas.isScalingEnabled()) {
        updateCanvasBounds();
      }
    }
  }

  /**
   * Updates the renderer to use the current front buffer to be displayed. The renderer will display a cached image
   * otherwise.
   */
  public void updateRenderer() {
    canvas.setImage(videoBuffer.frontBuffer());
  }

  /**
   * Removes old content from all buffers in the buffer chain.
   */
  public void clearBuffers() {
    for (int i = 0; i < videoBuffer.bufferCount(); i++) {
      BufferedImage img = (BufferedImage) videoBuffer.frontBuffer();
      if (img != null) {
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
      }
      videoBuffer.flipBuffers();
    }
    updateRenderer();
  }

  // --------------------- Begin Interface VideoBuffer ---------------------

  @Override
  public Image frontBuffer() {
    return videoBuffer.frontBuffer();
  }

  @Override
  public Image backBuffer() {
    return videoBuffer.backBuffer();
  }

  @Override
  public synchronized void flipBuffers() {
    videoBuffer.flipBuffers();
  }

  @Override
  public int bufferCount() {
    return videoBuffer.bufferCount();
  }

  @Override
  public void attachData(Object data) {
    videoBuffer.attachData(data);
  }

  @Override
  public Object fetchData() {
    return videoBuffer.fetchData();
  }

  // --------------------- End Interface VideoBuffer ---------------------

  // --------------------- Begin Interface ComponentListener ---------------------

  @Override
  public void componentHidden(ComponentEvent event) {
  }

  @Override
  public void componentMoved(ComponentEvent event) {
  }

  @Override
  public void componentResized(ComponentEvent event) {
    if (event.getSource() == this) {
      updateCanvasBounds();
    }
  }

  @Override
  public void componentShown(ComponentEvent event) {
  }

  // --------------------- End Interface ComponentListener ---------------------

  private void updateDefaultSize() {
    if (getScalingEnabled()) {
      setPreferredSize(getMinimumSize());
    } else {
      setPreferredSize(new Dimension(getBufferWidth(), getBufferHeight()));
    }
  }

  private void updateCanvasBounds() {
    if (getWidth() > 0 && getHeight() > 0) {
      Rectangle newBounds = new Rectangle();

      // updating canvas size
      if (canvas.isScalingEnabled()) {
        newBounds.width = getWidth();
        newBounds.height = getHeight();
        if (isAspect && getBufferHeight() > 0) {
          float ar = (float) getBufferWidth() / (float) getBufferHeight();
          if (newBounds.width / ar <= newBounds.height) {
            // use width as base
            newBounds.height = (int) (newBounds.width / ar);
          } else {
            // use height as base
            newBounds.width = (int) (newBounds.height * ar);
          }
        }
      } else {
        newBounds.width = getBufferWidth();
        newBounds.height = getBufferHeight();
      }

      // updating canvas position
      switch (canvas.getHorizontalAlignment()) {
        case SwingConstants.LEFT:
          newBounds.x = 0;
          break;
        case SwingConstants.RIGHT:
          newBounds.x = getWidth() - newBounds.width;
          break;
        default:
          newBounds.x = (getWidth() - newBounds.width) / 2;
      }
      switch (canvas.getVerticalAlignment()) {
        case SwingConstants.TOP:
          newBounds.y = 0;
          break;
        case SwingConstants.BOTTOM:
          newBounds.y = getHeight() - newBounds.height;
          break;
        default:
          newBounds.y = (getHeight() - newBounds.height) / 2;
      }

      canvas.setBounds(newBounds);
    }
  }
}
