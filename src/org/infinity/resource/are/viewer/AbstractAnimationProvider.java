// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;

import org.infinity.gui.layeritem.BasicAnimationProvider;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Provides base functionality for rendering animations.
 */
public abstract class AbstractAnimationProvider implements BasicAnimationProvider
{
  /** A default animation provider that can be used as placeholder. */
  public static final DefaultAnimationProvider DEFAULT_ANIMATION_PROVIDER = new DefaultAnimationProvider();

  private BufferedImage image, working;
  private boolean isActive, isActiveIgnored;

  protected AbstractAnimationProvider()
  {
    this.image = null;
    this.working = null;
    this.isActive = false;
  }

  /** Returns the active/visibility state of the animation. */
  public boolean isActive()
  {
    return isActive;
  }

  /** Specify whether to actually display the animation on screen. */
  public void setActive(boolean set)
  {
    if (set != isActive) {
      isActive = set;
      updateGraphics();
    }
  }

  /**
   * Returns whether to ignore the activation state of the animation.
   */
  public boolean isActiveIgnored()
  {
    return isActiveIgnored;
  }

  /**
   * Specify whether to ignore the activation state of the animation and display it regardless.
   */
  public void setActiveIgnored(boolean set)
  {
    if (set != isActiveIgnored) {
      isActiveIgnored = set;
      updateGraphics();
    }
  }

  @Override
  public Image getImage()
  {
    return image;
  }

  protected void setImage(Image img)
  {
    if (img instanceof BufferedImage) {
      this.image = (BufferedImage)img;
    }
  }

  @Override
  public boolean isLooping()
  {
    return true;
  }

  protected BufferedImage getWorkingImage()
  {
    return working;
  }

  protected void setWorkingImage(BufferedImage img)
  {
    this.working = img;
  }

  protected abstract void updateGraphics();


  // Applies lightning conditions to all pixels
  protected void applyLighting(int[] buffer, int cw, int ch, int fw, int fh, int lighting)
  {
    if (buffer != null && cw > 0 && ch > 0 && fw > 0 && fh > 0) {
      int maxOfs = fh*cw;
      if (buffer.length >= maxOfs) {
        if (lighting < ViewerConstants.LIGHTING_DAY) lighting = ViewerConstants.LIGHTING_DAY;
          else if (lighting > ViewerConstants.LIGHTING_NIGHT) lighting = ViewerConstants.LIGHTING_NIGHT;
        int ofs = 0;
        while (ofs < maxOfs) {
          for (int x = 0; x < fw; x++) {
            int pixel = buffer[ofs+x];
            if ((pixel & 0xff000000) != 0) {
              int r = (pixel >>> 16) & 0xff;
              int g = (pixel >>> 8) & 0xff;
              int b = pixel & 0xff;
              r = (r*TilesetRenderer.LightingAdjustment[lighting][0]) >>> TilesetRenderer.LightingAdjustmentShift;
              g = (g*TilesetRenderer.LightingAdjustment[lighting][1]) >>> TilesetRenderer.LightingAdjustmentShift;
              b = (b*TilesetRenderer.LightingAdjustment[lighting][2]) >>> TilesetRenderer.LightingAdjustmentShift;
              if (r > 255) r = 255;
              if (g > 255) g = 255;
              if (b > 255) b = 255;
              buffer[ofs+x] = (pixel & 0xff000000) | (r << 16) | (g << 8) | b;
            }
          }
          ofs += cw;
        }
      }
    }
  }


//-------------------------- INNER CLASSES --------------------------

  /** A pseudo animation provider that always returns a transparent image of 16x16 size. */
  public static final class DefaultAnimationProvider implements BasicAnimationProvider
  {
    private final BufferedImage image;

    public DefaultAnimationProvider()
    {
      image = ColorConvert.createCompatibleImage(16, 16, true);
    }

    @Override
    public Image getImage()
    {
      return image;
    }

    @Override
    public boolean advanceFrame()
    {
      return false;
    }

    @Override
    public void resetFrame()
    {
    }

    @Override
    public boolean isLooping()
    {
      return false;
    }

    @Override
    public Point getLocationOffset()
    {
      return new Point();
    }
  }

}
