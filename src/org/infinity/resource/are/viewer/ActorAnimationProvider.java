// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
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
import java.util.Objects;

import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder.SpriteBamControl;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Implements functionality for properly displaying actor sprites.
 */
public class ActorAnimationProvider extends AbstractAnimationProvider
{
  private static final Color TransparentColor = new Color(0, true);

  private SpriteDecoder decoder;
  private SpriteBamControl control;
  private boolean isLooping, isSelectionCircleEnabled, isPersonalSpaceEnabled;
  private int lighting, orientation, cycle, startFrame, endFrame;
  private Rectangle imageRect;

  public ActorAnimationProvider(SpriteDecoder decoder)
  {
    setDefaults();
    setDecoder(decoder);
  }

  /** Returns the BAM sprite decoder instance. */
  public SpriteDecoder getDecoder()
  {
    return decoder;
  }

  public void setDecoder(SpriteDecoder decoder)
  {
    this.decoder = Objects.requireNonNull(decoder, "Sprite decoder cannot be null");
    control = this.decoder.createControl();
    control.setMode(SpriteBamControl.Mode.INDIVIDUAL);
    control.setSharedPerCycle(false);
    control.cycleSet(getCycle());
    resetFrame();

    updateCanvas();
    updateGraphics();
  }

  /** Returns whether the selection circle underneath actor sprites is drawn. */
  public boolean isSelectionCircleEnabled()
  {
    return isSelectionCircleEnabled;
  }

  /** Specify whether the selection circle underneath actor sprites should be drawn. */
  public void setSelectionCircleEnabled(boolean enable)
  {
    if (enable != isSelectionCircleEnabled) {
      isSelectionCircleEnabled = enable;
      decoder.setSelectionCircleEnabled(isSelectionCircleEnabled);
      updateCanvas();
      updateGraphics();
    }
  }

  /** Returns whether the personal space indicator underneath actor sprites is drawn. */
  public boolean isPersonalSpaceEnabled()
  {
    return isPersonalSpaceEnabled;
  }

  /** Specify whether the selection circle underneath actor sprites should be drawn. */
  public void setPersonalSpaceEnabled(boolean enable)
  {
    if (enable != isPersonalSpaceEnabled) {
      isPersonalSpaceEnabled = enable;
      decoder.setPersonalSpaceVisible(isPersonalSpaceEnabled);
      updateCanvas();
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
          if (!decoder.isLightSource()) {
            updateGraphics();
          }
        }
        break;
    }
  }

  /** Returns the numeric orientation value of the actor sprite. */
  public int getOrientation()
  {
    return orientation;
  }

  /**
   * Sets the specified orientation value and updates the sprite cycle accordingly.
   * Should be called after a sprite sequence has been loaded.
   */
  public void setOrientation(int dir)
  {
    dir = Math.abs(dir) % Direction.values().length;
    if (dir != orientation) {
      orientation = dir;
      Direction direction = getDecoder().getExistingDirection(Direction.from(orientation));
      int idx = getDecoder().getDirectionMap().getOrDefault(direction, 0);
      setCycle(idx);
    }
  }

  /**
   * Returns the currently selected animation cycle.
   */
  public int getCycle()
  {
    int idx = cycle < 0 ? (control.cycleCount() + cycle) : cycle;
    return Math.max(0, Math.min(control.cycleCount() - 1, idx));
  }

  /**
   * Sets the current BAM cycle.
   * Specify positive values to set an absolute cycle index.
   * Specify negative values to set the cycle relative to the cycle length
   * where -1 indicates the last BAM cycle.
   */
  public void setCycle(int cycleIdx)
  {
    cycleIdx = Math.min(control.cycleCount() - 1, cycleIdx);
    if (cycleIdx != cycle) {
      cycle = cycleIdx;
      control.cycleSet(getCycle());
      updateCanvas();
      resetFrame();
    }
  }

  /** Returns the first frame of the current BAM cycle to be displayed. */
  public int getStartFrame()
  {
    int idx = startFrame < 0 ? (control.cycleFrameCount() + startFrame) : startFrame;
    return Math.max(0, Math.min(control.cycleFrameCount() - 1, idx));
  }

  /**
   * Sets the first frame of the current BAM cycle to be displayed.
   * Specify positive values to set an absolute frame index.
   * Specify negative values to set the start frame relative to the number of frames in the cycle
   * where -1 indicates the last frame of the cycle.
   */
  public void setStartFrame(int frameIdx)
  {
    frameIdx = Math.min(control.cycleFrameCount() - 1, frameIdx);
    if (frameIdx != startFrame) {
      startFrame = frameIdx;
      resetFrame();
    }
  }

  /** Returns the frame index after which the animation sequence ends. */
  public int getFrameCap()
  {
    int idx = endFrame < 0 ? (control.cycleFrameCount() + endFrame) : endFrame;
    return Math.max(0, Math.min(control.cycleFrameCount() - 1, idx));
  }

  /**
   * Sets the frame index after which the animation sequence ends.
   * Specify positive values to set an absolute frame index.
   * Specify negative values to set the end frame relative to the number of frames in the cycle
   * where -1 indicates the last frame of the cycle.
   */
  public void setFrameCap(int frameIdx)
  {
    frameIdx = Math.min(control.cycleFrameCount() -1, frameIdx);
    if (frameIdx != endFrame) {
      endFrame = frameIdx;
      resetFrame();
    }
  }

  /** Sets a new looping state. */
  public void setLooping(boolean set)
  {
    if (set != isLooping) {
      isLooping = set;
    }
  }

//--------------------- Begin Interface BasicAnimationProvider ---------------------

  @Override
  public boolean advanceFrame()
  {
    boolean retVal = control.cycleGetFrameIndex() < getFrameCap() - 1;
    if (retVal) {
      control.cycleNextFrame();
    }
//    retVal = control.cycleNextFrame();
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
    isLooping = true;
    isSelectionCircleEnabled = true;
    isPersonalSpaceEnabled = false;
    orientation = 0;  // south
    cycle = 0;
    startFrame = 0;
    endFrame = -1;
    lighting = ViewerConstants.LIGHTING_TWILIGHT;
    imageRect = null;
  }

  // Updates the global image object to match the shared size of the current BAM (cycle).
  private void updateCanvas()
  {
    imageRect = control.calculateSharedCanvas(false);
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
      Graphics2D g;
      if (isActive()) {
        g = image.createGraphics();
        try {
          // clearing old content
          g.setComposite(AlphaComposite.Src);
          g.setColor(TransparentColor);
          g.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
          g.dispose();
          g = null;
        }

        g = image.createGraphics();
        try {
          int frameIndex = control.cycleGetFrameIndexAbsolute();
          int left = -imageRect.x - decoder.getFrameInfo(frameIndex).getCenterX();
          int top = -imageRect.y - decoder.getFrameInfo(frameIndex).getCenterY();
          Point pos = new Point(left, top);

          // rendering visual markers
          control.getVisualMarkers(g, pos);

          // rendering frame
          // fetching frame data
          BufferedImage working = getWorkingImage();
          int[] buffer = ((DataBufferInt)working.getRaster().getDataBuffer()).getData();
          Arrays.fill(buffer, 0);
          decoder.frameGet(control, frameIndex, working);

          // post-processing frame
          buffer = ((DataBufferInt)working.getRaster().getDataBuffer()).getData();
          int canvasWidth = working.getWidth();
          int canvasHeight = working.getHeight();
          int frameWidth = decoder.getFrameInfo(frameIndex).getWidth();
          int frameHeight = decoder.getFrameInfo(frameIndex).getHeight();

          if (!decoder.isLightSource()) {
            applyLighting(buffer, canvasWidth, canvasHeight, frameWidth, frameHeight, getLighting());
          }
          buffer = null;

          // rendering frame
          g.drawImage(working, left, top, left+frameWidth, top+frameHeight, 0, 0, frameWidth, frameHeight, null);
        } finally {
          g.dispose();
          g = null;
        }
      } else {
        // draw placeholder instead
        g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.Src);
          g.setColor(TransparentColor);
          g.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
          g.dispose();
          g = null;
        }
      }
    }
  }
}
