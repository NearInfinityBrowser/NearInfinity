// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.resource.graphics.ColorConvert;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A component that displays an image. Provides support for scaling and filtering.
 * @author argent77
 */
public class RenderCanvas extends JComponent implements SwingConstants
{
  // interpolation types used in scaling
  public static final int TYPE_NEAREST_NEIGHBOR = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
  public static final int TYPE_BILINEAR         = AffineTransformOp.TYPE_BILINEAR;
  public static final int TYPE_BICUBIC          = AffineTransformOp.TYPE_BICUBIC;

  private Image currentImage;
  private int interpolationType;
  private boolean isScaling;
  private int verticalAlignment, horizontalAlignment;

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

  public RenderCanvas(Image image, boolean scaled, int interpolationType)
  {
    this(image, scaled, interpolationType, CENTER, CENTER);
  }

  public RenderCanvas(Image image, boolean scaled, int interpolationType,
                      int horizontalAlign, int verticalAlign)
  {
    setOpaque(false);
    currentImage = null;
    interpolationType = TYPE_NEAREST_NEIGHBOR;
    isScaling = false;
    verticalAlignment = horizontalAlignment = CENTER;
    setInterpolationType(interpolationType);
    setScalingEnabled(scaled);
    setHorizontalAlignment(horizontalAlign);
    setVerticalAlignment(verticalAlign);
    setImage(image);
  }

  /**
   * Returns the currently assigned image.
   * @return Image object if an image has been assigned, <code>false</code> otherwise.
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
      currentImage = image;
      updateSize();
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
   * @return <code>true</code> if scaling has been activated.
   */
  public boolean getScalingEnabled()
  {
    return isScaling;
  }

  /**
   * Sets whether the image buffer's content should be drawn scaled.
   * @param enable <code>true</code> to enable scaling.
   */
  public void setScalingEnabled(boolean enable)
  {
    if (enable != isScaling) {
      isScaling = enable;
      repaint();
    }
  }

  /**
   * Returns the interpolation type used when scaling has been enabled.
   * @return The interpolation type.
   */
  public int getInterpolationType()
  {
    return interpolationType;
  }

  /**
   * Specify the interpolation type used when scaling has been enabled.
   * One of TYPE_NEAREST_NEIGHBOR, TYPE_BILINEAR and TYPE_BICUBIC (Default: TYPE_NEAREST_NEIGHBOR).
   * @param interpolationType The new interpolation type to set.
   */
  public void setInterpolationType(int interpolationType)
  {
    if (this.interpolationType != interpolationType) {
      switch (interpolationType) {
        case TYPE_NEAREST_NEIGHBOR:
        case TYPE_BILINEAR:
        case TYPE_BICUBIC:
          this.interpolationType = interpolationType;
          break;
        default:
          return;
      }
      if (isScaling) {
        repaint();
      }
    }
  }


  private void updateSize()
  {
    if (currentImage != null) {
      setPreferredSize(new Dimension(currentImage.getWidth(null), currentImage.getHeight(null)));
      if (!isScaling) {
        setSize(getPreferredSize());
      }
    } else {
      setPreferredSize(new Dimension(16, 16));
      setSize(getPreferredSize());
    }
    repaint();
  }

//--------------------- Begin Class Component ---------------------

  @Override
  public void paint(Graphics g)
  {
    if (currentImage != null && currentImage.getWidth(null) > 0 && currentImage.getHeight(null) > 0) {
      Graphics2D g2 = (Graphics2D)g;
      if (isScaling) {
        BufferedImage image = ColorConvert.toBufferedImage(currentImage, true);
        double sx = (double)getWidth() / (double)currentImage.getWidth(null);
        double sy = (double)getHeight() / (double)currentImage.getHeight(null);
        BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(sx, sy),
                                                   interpolationType);
        g2.drawImage(image, op, 0, 0);
        image = null;
      } else {
        Point srcPos = new Point(0, 0);
        switch (horizontalAlignment) {
          case LEFT:
            srcPos.x = 0;
            break;
          case RIGHT:
            srcPos.x = getWidth() - currentImage.getWidth(null);
            break;
          default:
            srcPos.x = (getWidth() - currentImage.getWidth(null)) / 2;
        }
        switch (verticalAlignment) {
          case TOP:
            srcPos.y = 0;
            break;
          case BOTTOM:
            srcPos.y = getHeight() - currentImage.getHeight(null);
            break;
          default:
            srcPos.y = (getHeight() - currentImage.getHeight(null)) / 2;
        }
        g2.drawImage(currentImage, srcPos.x, srcPos.y, null);
      }
    }
  }

//--------------------- End Class Component ---------------------
}
