// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A component that displays an image. Provides support for scaling and filtering.
 */
public class RenderCanvas extends JComponent implements SwingConstants
{
  // interpolation types used in scaling
  public static final Object TYPE_NEAREST_NEIGHBOR  = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
  public static final Object TYPE_BILINEAR          = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
  public static final Object TYPE_BICUBIC           = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

  private Image currentImage;
  private Object interpolationType;
  private boolean isScaling, isAutoScale;
  private int scaledWidth, scaledHeight;
  private int verticalAlignment, horizontalAlignment;
  private Composite composite;

  public RenderCanvas()
  {
    this(null, false, TYPE_NEAREST_NEIGHBOR, CENTER, CENTER);
  }

  public RenderCanvas(Image image)
  {
    this(image, false, TYPE_NEAREST_NEIGHBOR, CENTER, CENTER);
  }

  public RenderCanvas(Image image, boolean scaled)
  {
    this(image, scaled, TYPE_NEAREST_NEIGHBOR, CENTER, CENTER);
  }

  public RenderCanvas(Image image, boolean scaled, Object interpolationType)
  {
    this(image, scaled, interpolationType, CENTER, CENTER);
  }

  public RenderCanvas(Image image, boolean scaled, Object interpolationType,
                      int horizontalAlign, int verticalAlign)
  {
    setOpaque(false);
    this.currentImage = null;
    this.interpolationType = TYPE_NEAREST_NEIGHBOR;
    this.isScaling = false;
    this.isAutoScale = true;   // defaults to "true" to preserve backwards compatibility
    this.scaledWidth = this.scaledHeight = -1;
    this.verticalAlignment = this.horizontalAlignment = CENTER;
    setInterpolationType(interpolationType);
    setScalingEnabled(scaled);
    setComposite(null);
    setHorizontalAlignment(horizontalAlign);
    setVerticalAlignment(verticalAlign);
    setImage(image);
  }

  /**
   * Returns the currently assigned image.
   * @return Image object if an image has been assigned, {@code false} otherwise.
   */
  public Image getImage()
  {
    return currentImage;
  }

  /**
   * Sets a new image
   * @param image
   */
  public void setImage(Image image)
  {
    if (currentImage != image) {
      if (image == null) {
        currentImage.flush();
      }
      currentImage = image;

      if (currentImage != null) {
        setPreferredSize(new Dimension(currentImage.getWidth(null), currentImage.getHeight(null)));
      } else {
        setPreferredSize(new Dimension(16, 16));
      }
      update();
    }
  }

  /**
   * Gets the alignment of the component's content along the Y axis.
   * @return One of the following constants defined in SwingConstants: TOP, CENTER or BOTTOM.
   */
  public int getVerticalAlignment()
  {
    return verticalAlignment;
  }

  /**
   * Sets the alignment of the component's content along the Y axis. The default property is CENTER.
   * @param alignment One of the following constants defined in SwingConstants: TOP, CENTER or BOTTOM.
   */
  public void setVerticalAlignment(int alignment)
  {
    if (alignment != verticalAlignment) {
      switch (alignment) {
        case SwingConstants.TOP:
        case SwingConstants.LEFT:
          verticalAlignment = SwingConstants.TOP;
          break;
        case SwingConstants.BOTTOM:
        case SwingConstants.RIGHT:
          verticalAlignment = SwingConstants.BOTTOM;
          break;
        default:
          verticalAlignment = SwingConstants.CENTER;
      }
      repaint();
    }
  }

  /**
   * Gets the alignment of the component's content along the X axis.
   * @return One of the following constants defined in SwingConstants: LEFT, CENTER or RIGHT.
   */
  public int getHorizontalAlignment()
  {
    return horizontalAlignment;
  }

  /**
   * Sets the alignment of the component's content along the X axis. The default property is CENTER.
   * @param alignment One of the following constants defined in SwingConstants: LEFT, CENTER or RIGHT.
   */
  public void setHorizontalAlignment(int alignment)
  {
    if (alignment != horizontalAlignment) {
      switch (alignment) {
        case SwingConstants.LEFT:
        case SwingConstants.TOP:
          verticalAlignment = SwingConstants.LEFT;
          break;
        case SwingConstants.RIGHT:
        case SwingConstants.BOTTOM:
          verticalAlignment = SwingConstants.RIGHT;
          break;
        default:
          verticalAlignment = SwingConstants.CENTER;
      }
      repaint();
    }
  }

  /**
   * Returns whether scaling has been activated.
   * @return {@code true} if scaling has been activated.
   */
  public boolean isScalingEnabled()
  {
    return isScaling;
  }

  /**
   * Sets whether the image buffer's content should be drawn scaled.
   * @param enable {@code true} to enable scaling.
   */
  public void setScalingEnabled(boolean enable)
  {
    if (enable != isScaling) {
      isScaling = enable;
      repaint();
    }
  }

  /**
   * Returns whether the image should be scaled to full size of the component.
   * Values set by {@link #setScaledWidth(int)}, {@link #setScaledHeight(int) or {@link #setScaleFactor(float)}
   * are ignored while this setting is enabled.
   * @return {@code true} if image will be auto-scaled.
   */
  public boolean isAutoScaleEnabled()
  {
    return isAutoScale;
  }

  /**
   * Sets whether the image will be scaled to full size of the component.
   * Values set by {@link #setScaledWidth(int)}, {@link #setScaledHeight(int) or {@link #setScaleFactor(float)}
   * are ignored while this setting is enabled.
   * @param enable {@code true} to enable auto-scale.
   */
  public void setAutoScaleEnabled(boolean enable)
  {
    if (enable != isAutoScale) {
      isAutoScale = enable;
      repaint();
    }
  }

  /**
   * A convenience method that applies a uniform scale to the current image in both vertical and
   * horizontal direction.
   * @param factor The scaling factor. Must be > 0.0.
   */
  public void setScaleFactor(float factor)
  {
    if (factor <= 0.0f) return;

    if (currentImage != null) {
      setScaledWidth((int)(currentImage.getWidth(null) * factor));
      setScaledHeight((int)(currentImage.getHeight(null) * factor));
    }
  }

  /**
   * Returns the scaled width of the current image.
   * @return scaled width of the current image.
   */
  public int getScaledWidth()
  {
    return scaledWidth;
  }

  /**
   * Sets the new width of the image when scaled. This value is ignored while
   * {@link #setAutoScaleEnabled(boolean)} is active.
   * @param newWidth New width of the image. Must be > 0.
   */
  public void setScaledWidth(int newWidth)
  {
    if (newWidth > 0 && newWidth != scaledWidth) {
      scaledWidth = newWidth;
      update();
    }
  }

  /**
   * Returns the scaled height of the current image.
   * @return scaled height of the current image.
   */
  public int getScaledHeight()
  {
    return scaledHeight;
  }

  /**
   * Sets the new height of the image when scaled. This value is ignored while
   * {@link #setAutoScaleEnabled(boolean)} is active.
   * @param newHeight New height of the image. Must be > 0.
   */
  public void setScaledHeight(int newHeight)
  {
    if (newHeight > 0 && newHeight != scaledHeight) {
      scaledHeight = newHeight;
      update();
    }
  }

  /**
   * Returns the interpolation type used when scaling has been enabled.
   * @return The interpolation type.
   */
  public Object getInterpolationType()
  {
    return interpolationType;
  }

  /**
   * Specify the interpolation type used when scaling has been enabled.
   * One of TYPE_NEAREST_NEIGHBOR, TYPE_BILINEAR and TYPE_BICUBIC (Default: TYPE_NEAREST_NEIGHBOR).
   * @param interpolationType The new interpolation type to set.
   */
  public void setInterpolationType(Object interpolationType)
  {
    if (this.interpolationType != interpolationType) {
      if (interpolationType == TYPE_NEAREST_NEIGHBOR ||
          interpolationType == TYPE_BILINEAR ||
          interpolationType == TYPE_BICUBIC) {
        this.interpolationType = interpolationType;
        if (isScaling) {
          repaint();
        }
      }
    }
  }

  /**
   * Returns the {@link Composite} object used to draw the image on the canvas.
   */
  public Composite getComposite()
  {
    return composite;
  }

  /**
   * Sets the {@link Composite} object that is used to draw the image on the canvas.
   * @param c the {@code Composite} object. Specify {@code null} to use a default composite object.
   */
  public void setComposite(Composite c)
  {
    composite = (c != null) ? c : AlphaComposite.SrcOver;
  }


  protected void update()
  {
    invalidate();
    if (getParent() != null) {
      getParent().repaint();
    }
  }

  protected Rectangle getCanvasSize()
  {
    Rectangle rect = new Rectangle();
    if (currentImage != null) {
      rect.width = currentImage.getWidth(null);
      rect.height = currentImage.getHeight(null);
      if (isScaling) {
        rect.width = isAutoScale ? getWidth() : scaledWidth;
        rect.height = isAutoScale ? getHeight() : scaledHeight;
      }
      switch (horizontalAlignment) {
        case LEFT:
          break;
        case RIGHT:
          rect.x = getWidth() - rect.width;
          break;
        default:
          rect.x = (getWidth() - rect.width) / 2;
      }
      switch (verticalAlignment) {
        case TOP:
          break;
        case BOTTOM:
          rect.y = getHeight() - rect.height;
          break;
        default:
          rect.y = (getHeight() - rect.height) / 2;
      }
    } else {
      rect.width = rect.height = 16;
    }
    return rect;
  }

  /**
   * Renders the image to the canvas.
   * @param g The graphics context in which to paint.
   */
  protected void paintCanvas(Graphics g)
  {
    if (currentImage != null && currentImage.getWidth(null) > 0 && currentImage.getHeight(null) > 0) {
      Graphics2D g2 = (Graphics2D)g;
      Composite oldComposite = g2.getComposite();
      g2.setComposite(getComposite());
      Rectangle rect = getCanvasSize();
      if (isScaling) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationType);
        g2.drawImage(currentImage, rect.x, rect.y, rect.width, rect.height, null);
      } else {
        g2.drawImage(currentImage, rect.x, rect.y, null);
      }
      if (oldComposite != null) {
        g2.setComposite(oldComposite);
      }
    }
  }

//--------------------- Begin Class Component ---------------------

  @Override
  public void paint(Graphics g)
  {
    paintCanvas(g);
    super.paint(g);
  }

//--------------------- End Class Component ---------------------
}
