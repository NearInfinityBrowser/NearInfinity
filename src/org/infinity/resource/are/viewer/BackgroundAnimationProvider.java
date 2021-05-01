// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamV1Decoder;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Implements functionality for properly displaying background animations.
 */
public class BackgroundAnimationProvider extends AbstractAnimationProvider
{
  private static final Color TransparentColor = new Color(0, true);

  // upscaled luma weights for use with faster right-shift (by 16)
  private static final int LumaR = 19595, LumaG = 38470, LumaB = 7471;

  // lookup table for alpha transparency on blended animations
  private static final int[] TableAlpha = new int[256];
  static {
    for (int i = 0; i < TableAlpha.length; i++) {
      TableAlpha[i] = (int)(Math.pow((double)i / 255.0, 0.5) * 256.0);
    }
  }

  private BamDecoder bam;
  private BamDecoder.BamControl control;
  private boolean isBlended, isMirrored, isLooping, isSelfIlluminated,
                  isMultiPart, isPaletteEnabled;
  private int lighting, baseAlpha;
  private int firstFrame, lastFrame;
  private int[] palette;    // external palette
  private Rectangle imageRect;

  public BackgroundAnimationProvider()
  {
    this(null);
  }

  public BackgroundAnimationProvider(BamDecoder bam)
  {
    setDefaults();
    setAnimation(bam);
  }

  /** Returns the BAM animation object. */
  public BamDecoder getAnimation()
  {
    return bam;
  }

  public void setAnimation(BamDecoder bam)
  {
    if (bam != null) {
      this.bam = bam;
    } else {
      // setting pseudo bam
      this.bam = BamDecoder.loadBam(ColorConvert.createCompatibleImage(1, 1, true));
    }
    this.control = this.bam.createControl();
    this.control.setMode(BamDecoder.BamControl.Mode.INDIVIDUAL);
    this.control.setSharedPerCycle(!isMultiPart());
    resetFrame();

    updateCanvas();
    updateGraphics();
  }

  /** Sets an external palette that can be used to recolor the BAM animation. */
  public void setPalette(int[] palette)
  {
    if (palette != null && palette.length >= 256) {
      this.palette = new int[256];
      System.arraycopy(palette, 0, this.palette, 0, 256);
    } else {
      this.palette = null;
    }
    if (isPaletteEnabled() && control instanceof BamV1Decoder.BamV1Control) {
      ((BamV1Decoder.BamV1Control)control).setExternalPalette(this.palette);
    }
    updateGraphics();
  }

  public boolean isPaletteEnabled()
  {
    return isPaletteEnabled;
  }

  public void setPaletteEnabled(boolean set)
  {
    if (set != isPaletteEnabled) {
      isPaletteEnabled = set;
      if (control instanceof BamV1Decoder.BamV1Control) {
        if (isPaletteEnabled) {
          ((BamV1Decoder.BamV1Control)control).setExternalPalette(this.palette);
        } else {
          ((BamV1Decoder.BamV1Control)control).setExternalPalette(null);
        }
      }
      updateGraphics();
    }
  }

  /** Returns whether the current frame of all available cycles are displayed simultanuously. */
  public boolean isMultiPart()
  {
    return isMultiPart;
  }

  /** Sets whether the current frame of all available cycles are displayed simultanuously. */
  public void setMultiPart(boolean set)
  {
    if (set != isMultiPart) {
      isMultiPart = set;
      control.setSharedPerCycle(!isMultiPart);
      updateCanvas();
      updateGraphics();
    }
  }

  /** Returns whether the animation uses brightness as alpha transparency. */
  public boolean isBlended()
  {
    return isBlended;
  }

  /** Sets whether the animation uses its brightness as alpha transparency. */
  public void setBlended(boolean set)
  {
    if (set != isBlended) {
      isBlended = set;
      updateGraphics();
    }
  }

  /** Returns whether the animation is drawn mirrored on the x axis. */
  public boolean isMirrored()
  {
    return isMirrored;
  }

  /** Sets whether the animation is mirrored on the x axis. */
  public void setMirrored(boolean set)
  {
    if (set != isMirrored) {
      isMirrored = set;
      updateCanvas();
      updateGraphics();
    }
  }

  /** Returns whether the animation ignores lighting conditions. */
  public boolean isSelfIlluminated()
  {
    return isSelfIlluminated;
  }

  /** Sets whether the animation ignores lighting conditions */
  public void setSelfIlluminated(boolean set)
  {
    if (set != isSelfIlluminated) {
      isSelfIlluminated = set;
      updateGraphics();
    }
  }

  /** Returns the lighting condition of the animation. */
  public int getLighting()
  {
    return lighting;
  }

  /** Defines a new lighting condition for the animation. No change if the animation is self-illuminated. */
  public void setLighting(int state)
  {
    switch (state) {
      case ViewerConstants.LIGHTING_DAY:
      case ViewerConstants.LIGHTING_TWILIGHT:
      case ViewerConstants.LIGHTING_NIGHT:
        if (state != lighting) {
          lighting = state;
          if (!isSelfIlluminated()) {
            updateGraphics();
          }
        }
        break;
    }
  }

  public int getBaseAlpha()
  {
    return baseAlpha;
  }

  public void setBaseAlpha(int alpha)
  {
    if (alpha >= 0 && alpha < 256 && alpha != baseAlpha) {
      baseAlpha = alpha;
      updateGraphics();
    }
  }

  /** Sets a new looping state. */
  public void setLooping(boolean set)
  {
    if (set != isLooping) {
      isLooping = set;
    }
  }

  /**
   * Returns the currently selected cycle if {@link #isMultiPart()} is {@code false}.
   * @return The currently selected cycle
   */
  public int getCycle()
  {
    if (!isMultiPart()) {
      return control.cycleGet();
    } else {
      return -1;
    }
  }

  /**
   * Sets the current BAM cycle. Does nothing if {@link #isMultiPart()} is {@code true}.
   * @param cycle The BAM cycle to set.
   */
  public void setCycle(int cycle)
  {
    control.cycleSet(cycle);
    updateCanvas();
    updateGraphics();
  }

  /**
   * Returns the initial starting frame of the animation when starting playback.
   * @return The starting frame of the animation.
   */
  public int getStartFrame()
  {
    return firstFrame;
  }

  /**
   * Sets the initial starting frame of the animation when starting playback.
   * @param frameIdx The starting frame of the animation.
   */
  public void setStartFrame(int frameIdx)
  {
    if (frameIdx >= 0 && frameIdx < control.cycleFrameCount()) {
      if (lastFrame >= 0 && frameIdx > lastFrame) {
        firstFrame = lastFrame;
      } else {
        firstFrame = frameIdx;
      }
      if (control.cycleGetFrameIndex() < firstFrame) {
        control.cycleSetFrameIndex(firstFrame);
        updateGraphics();
      }
    }
  }

  /**
   * Returns the frame index after which the animation sequence ends.
   */
  public int getFrameCap()
  {
    return (lastFrame < 0) ? control.cycleFrameCount() - 1 : lastFrame;
  }

  /**
   * Sets the frame index after which the animation sequence ends.
   * @param frameIdx Index of the last frame in the animation sequence to be displayed.
   *                 Set to -1 to disable frame cap.
   */
  public void setFrameCap(int frameIdx)
  {
    if (frameIdx < 0) {
      lastFrame = -1;
    } else if (frameIdx < control.cycleFrameCount()) {
      lastFrame = frameIdx;
      if (firstFrame > lastFrame) {
        firstFrame = lastFrame;
      }
      if (control.cycleGetFrameIndex() > lastFrame) {
        control.cycleSetFrameIndex(lastFrame);
        updateGraphics();
      }
    }
  }

//--------------------- Begin Interface BasicAnimationProvider ---------------------

  @Override
  public boolean advanceFrame()
  {
    boolean retVal = false;
    if (lastFrame >= 0) {
      if (control.cycleGetFrameIndex() < lastFrame) {
        retVal = control.cycleNextFrame();
      }
    } else {
      retVal = control.cycleNextFrame();
    }
    updateGraphics();
    return retVal;
  }

  @Override
  public void resetFrame()
  {
    control.cycleSetFrameIndex(getStartFrame());
    updateGraphics();
  }

  @Override
  public boolean isLooping()
  {
    return isLooping;
  }

  @Override
  public Point getLocationOffset()
  {
    return imageRect.getLocation();
  }

//--------------------- End Interface BasicAnimationProvider ---------------------

  // Sets sane default values for all properties
  private void setDefaults()
  {
    setImage(null);
    setWorkingImage(null);
    setActive(true);
    setActiveIgnored(false);
    imageRect = null;
    isBlended = false;
    isMirrored = false;
    isLooping = true;
    isSelfIlluminated = true;
    isMultiPart = false;
    isPaletteEnabled = false;
    palette = null;
    lighting = ViewerConstants.LIGHTING_TWILIGHT;
    baseAlpha = 255;
    firstFrame = 0;
    lastFrame = -1;
  }

  // Updates the global image object to match the shared size of the current BAM (cycle).
  private void updateCanvas()
  {
    imageRect = control.calculateSharedCanvas(isMirrored());
    BufferedImage image = (BufferedImage)getImage();
    if (getWorkingImage() == null || image == null ||
        image.getWidth() != imageRect.width || image.getHeight() != imageRect.height) {
      setImage(ColorConvert.createCompatibleImage(imageRect.width, imageRect.height, true));
      setWorkingImage(new BufferedImage(imageRect.width, imageRect.height, BufferedImage.TYPE_INT_ARGB));
    }
  }

  // Renders the current frame
  @Override
  protected synchronized void updateGraphics()
  {
    BufferedImage image = (BufferedImage)getImage();
    if (image != null) {
      if (isActive() || isActiveIgnored()) {
        // preparing frames
        int[] frameIndices = null;
        if (isMultiPart()) {
          frameIndices = new int[control.cycleCount()];
          for (int i = 0, cCount = control.cycleCount(); i < cCount; i++) {
            frameIndices[i] = control.cycleGetFrameIndexAbsolute(i, control.cycleGetFrameIndex());
          }
        } else {
          frameIndices = new int[]{control.cycleGetFrameIndexAbsolute()};
        }

        // clearing old content
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.setColor(TransparentColor);
          g.fillRect(0, 0, image.getWidth(), image.getHeight());

          // rendering frame
          BufferedImage working = getWorkingImage();
          for (int i = 0; i < frameIndices.length; i++) {
            // fetching frame data
            int[] buffer = ((DataBufferInt)working.getRaster().getDataBuffer()).getData();
            Arrays.fill(buffer, 0);
            if (bam instanceof BamV1Decoder) {
              ((BamV1Decoder)bam).frameGet(control, frameIndices[i], working);
            } else {
              bam.frameGet(control, frameIndices[i], working);
            }

            // post-processing frame
            buffer = ((DataBufferInt)working.getRaster().getDataBuffer()).getData();
            int canvasWidth = working.getWidth();
            int canvasHeight = working.getHeight();
            int frameWidth = bam.getFrameInfo(frameIndices[i]).getWidth();
            int frameHeight = bam.getFrameInfo(frameIndices[i]).getHeight();

            if (isMirrored()) {
              mirrorImage(buffer, canvasWidth, canvasHeight, frameWidth, frameHeight);
            }
            if (baseAlpha < 255) {
              applyAlpha(buffer, canvasWidth, canvasHeight, frameWidth, frameHeight, getBaseAlpha());
            }
            if (isBlended()) {
              applyBlending(buffer, canvasWidth, canvasHeight, frameWidth, frameHeight);
            }
            if (!isSelfIlluminated()) {
              applyLighting(buffer, canvasWidth, canvasHeight, frameWidth, frameHeight, getLighting());
            }
            buffer = null;

            // rendering frame
            int left, top;
            if (isMirrored()) {
              left = -imageRect.x - (bam.getFrameInfo(frameIndices[i]).getWidth() - bam.getFrameInfo(frameIndices[i]).getCenterX() - 1);
              top = -imageRect.y - bam.getFrameInfo(frameIndices[i]).getCenterY();
            } else {
              left = -imageRect.x - bam.getFrameInfo(frameIndices[i]).getCenterX();
              top = -imageRect.y - bam.getFrameInfo(frameIndices[i]).getCenterY();
            }

            g.drawImage(working, left, top, left+frameWidth, top+frameHeight, 0, 0, frameWidth, frameHeight, null);
          }
        } finally {
          g.dispose();
          g = null;
        }
      } else {
        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.setColor(TransparentColor);
          g.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
          g.dispose();
          g = null;
        }
      }
    }
  }

  // Mirrors image along the x axis
  private void mirrorImage(int[] buffer, int cw, int ch, int fw, int fh)
  {
    if (buffer != null && cw > 0 && ch > 0 && fw > 0 && fh > 0) {
      int maxOfs = fh*cw;
      if (buffer.length >= maxOfs) {
        int ofs = 0;
        while (ofs < maxOfs) {
          int left = 0;
          int right = fw - 1;
          while (left < right) {
            int pixel = buffer[ofs+left];
            buffer[ofs+left] = buffer[ofs+right];
            buffer[ofs+right] = pixel;
            left++;
            right--;
          }
          ofs += cw;
        }
      }
    }
  }

  // applies a global alpha value to all pixels
  private void applyAlpha(int[] buffer, int cw, int ch, int fw, int fh, int alpha)
  {
    if (buffer != null && cw > 0 && ch > 0 && fw > 0 && fh > 0) {
      int maxOfs = fh*cw;
      if (buffer.length >= maxOfs) {
        if (alpha < 0) alpha = 0; else if (alpha > 255) alpha = 255;
        alpha *= 65793;   // upscaling for use with faster right-shift (by 24)
        int ofs = 0;
        while (ofs < maxOfs) {
          for (int x = 0; x < fw; x++) {
            int pixel = buffer[ofs+x];
            if ((pixel & 0xff000000) != 0) {
              int a = (pixel >>> 24) & 0xff;    // using right shift because of upscaled alpha
              a = (a*alpha) >>> 24;
              buffer[ofs+x] = (a << 24) | (pixel & 0x00ffffff);
            }
          }
          ofs += cw;
        }
      }
    }
  }

  // Blends pixels based on their brightness
  private void applyBlending(int[] buffer, int cw, int ch, int fw, int fh)
  {
    if (buffer != null && cw > 0 && ch > 0 && fw > 0 && fh > 0) {
      int maxOfs = fh*cw;
      if (buffer.length >= maxOfs) {
        int ofs = 0;
        while (ofs < maxOfs) {
          for (int x = 0; x < fw; x++) {
            int pixel = buffer[ofs+x];
            if ((pixel & 0xff000000) != 0) {
              int a = (pixel >>> 24) & 0xff;
              int r = (pixel >>> 16) & 0xff;
              int g = (pixel >>> 8) & 0xff;
              int b = pixel & 0xff;
              a = (a*TableAlpha[((r*LumaR) + (g*LumaG) + (b*LumaB)) >>> 16]) >>> 8;
              if (a > 255) a = 255;
              buffer[ofs+x] = (a << 24) | (pixel & 0x00ffffff);
            }
          }
          ofs += cw;
        }
      }
    }
  }
}
