// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Point;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.AbstractStruct;

/**
 * Base class for layer type: Actor
 */
public abstract class LayerObjectActor extends LayerObject
{
  protected final Point location = new Point();

  protected IconLayerItem item;


  protected LayerObjectActor(Class<? extends AbstractStruct> classType, AbstractStruct parent)
  {
    super("Actor", classType, parent);
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
  @Override
  public void close()
  {
    // TODO: implement method
    super.close();
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }
  //</editor-fold>
}
