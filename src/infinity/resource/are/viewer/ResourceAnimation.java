// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.gui.layeritem.AnimatedLayerItem;

/**
 * A structure to hold a unique identifier and the associated animation data.
 * @author argent77
 */
public class ResourceAnimation extends BasicResource
{
  private final AnimatedLayerItem.Frame[] frameData;

  /**
   * Creates a new animation object that consists of a resource name (any unique name will do) and
   * the associated animation data.
   * @param key A unique keyword that can be used to identify the animation.
   * @param frameData The animation data.
   */
  public ResourceAnimation(String key, AnimatedLayerItem.Frame[] frameData)
  {
    super(key);
    this.frameData = frameData;
  }

  @Override
  public AnimatedLayerItem.Frame[] getData()
  {
    return frameData;
  }
}
