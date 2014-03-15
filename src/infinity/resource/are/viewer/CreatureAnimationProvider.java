// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.gui.layeritem.BasicAnimationProvider;

/**
 * Implements functionality for properly displaying creature animations.
 * @author argent77
 */
public class CreatureAnimationProvider implements BasicAnimationProvider
{
  public CreatureAnimationProvider()
  {
    // TODO
  }

//--------------------- Begin Interface BasicAnimationProvider ---------------------

  @Override
  public Image getImage()
  {
    // TODO
    return null;
  }

  @Override
  public boolean advanceFrame()
  {
    // TODO
    return false;
  }

  @Override
  public void resetFrame()
  {
    // TODO
  }

  @Override
  public boolean isLooping()
  {
    // TODO
    return false;
  }

  @Override
  public Point getLocationOffset()
  {
    // TODO
    return null;
  }

//--------------------- End Interface BasicAnimationProvider ---------------------
}
