// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.TextString;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;

/**
 * Handles specific layer type: ARE/Background Animation
 * @author argent77
 */
public class LayerObjectAnimation extends LayerObject
{
  private static final Image[][] Icon = new Image[][]{
    {Icons.getImage("Animation.png"), Icons.getImage("Animation_s.png")},
    {Icons.getImage("AnimationWBM.png"), Icons.getImage("AnimationWBM_s.png")},
    {Icons.getImage("AnimationPVRZ.png"), Icons.getImage("AnimationPVRZ_s.png")},
    {Icons.getImage("AnimationBAM.png"), Icons.getImage("AnimationBAM_s.png")}
  };
  private static Point Center = new Point(16, 17);

  private final Animation anim;
  private final Point location = new Point();

  private IconLayerItem item;


  public LayerObjectAnimation(AreResource parent, Animation anim)
  {
    super("Animation", Animation.class, parent);
    this.anim = anim;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return anim;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{anim};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (item != null && mapOrigin != null) {
      item.setItemLocation(mapOrigin.x + (int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           mapOrigin.y + (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }

  private void init()
  {
    if (anim != null) {
      String msg = "";
      int iconIdx = 0;
      try {
        location.x = ((DecNumber)anim.getAttribute("Location: X")).getValue();
        location.y = ((DecNumber)anim.getAttribute("Location: Y")).getValue();
        Flag flags = (Flag)anim.getAttribute("Appearance");
        if (ResourceFactory.getGameID() == ResourceFactory.ID_BGEE ||
            ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE) {
          if (flags.isFlagSet(13)) {
            iconIdx = 1;
          } else if (flags.isFlagSet(15)) {
            iconIdx = 2;
          } else {
            iconIdx = 3;
          }
        }
        msg = ((TextString)anim.getAttribute("Name")).toString();
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new IconLayerItem(location, anim, msg, Icon[iconIdx][0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, Icon[iconIdx][1]);
      item.setVisible(isVisible());
    }
  }
}
