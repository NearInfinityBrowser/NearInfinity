// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.layeritem;

import java.awt.Image;
import java.awt.Point;

/**
 * Required by the AnimatedLayerItem class to properly draw graphical data.
 */
public interface BasicAnimationProvider {
  /**
   * Returns the graphical data of the current frame. (Note: Subclasses have to make sure that this method always
   * returns a valid and up to date graphics object.)
   */
  Image getImage();

  /**
   * Advances the animation by one frame. Does not wrap around after reaching the last frame.
   *
   * @return Whether the frame has been advanced successfully.
   */
  boolean advanceFrame();

  /**
   * Selects the first frame of the animation.
   */
  void resetFrame();

  /**
   * Returns whether the animation should be played back continuously.
   */
  boolean isLooping();

  /**
   * Returns the animation origin relative to the top-left corner of the image.
   */
  Point getLocationOffset();
}
