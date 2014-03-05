// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.List;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.AnimatedLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.resource.StructEntry;
import infinity.resource.are.Animation;
import infinity.resource.are.AreResource;

/**
 * Manages background animation layer objects.
 * @author argent77
 */
public class LayerAnimation extends BasicLayer<LayerObjectAnimation>
{
  private static final String AvailableFmt = "%1$d background animation%2$s available";

  private boolean realEnabled, realPlaying, forcedInterpolation;
  private int frameState, interpolationType;

  public LayerAnimation(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.Animation, viewer);
    realEnabled = realPlaying = false;
    frameState = ViewerConstants.FRAME_AUTO;
    forcedInterpolation = false;
    interpolationType = ViewerConstants.TYPE_NEAREST_NEIGHBOR;
    loadLayer(false);
  }

  @Override
  public int loadLayer(boolean forced)
  {
    if (forced || !isInitialized()) {
      clear();
      List<LayerObjectAnimation> list = getLayerObjects();
      if (hasAre()) {
        AreResource are = getAre();
        SectionOffset so = (SectionOffset)are.getAttribute("Animations offset");
        SectionCount sc = (SectionCount)are.getAttribute("# animations");
        if (so != null && sc != null) {
          int ofs = so.getValue();
          int count = sc.getValue();
          List<StructEntry> listStruct = getStructures(ofs, count, Animation.class);
          for (int i = 0; i < listStruct.size(); i++) {
            LayerObjectAnimation obj = new LayerObjectAnimation(are, (Animation)listStruct.get(i));
            setListeners(obj);
            list.add(obj);
          }
          setInitialized(true);
        }
      }
      return list.size();
    }
    return 0;
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt, (cnt == 1) ? "" : "s");
  }

  /**
   * Sets the visibility state of all items in the layer. Takes enabled states of the different
   * item types into account.
   */
  public void setLayerVisible(boolean visible)
  {
    setVisibilityState(visible);
    List<LayerObjectAnimation> list = getLayerObjects();
    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        boolean state = isLayerVisible() && (!isScheduleEnabled() || (isScheduleEnabled() && isScheduled(i)));
        LayerObjectAnimation obj = list.get(i);
        IconLayerItem iconItem = (IconLayerItem)obj.getLayerItem(ViewerConstants.ANIM_ITEM_ICON);
        if (iconItem != null) {
          iconItem.setVisible(state && !realEnabled);
        }
        AnimatedLayerItem animItem = (AnimatedLayerItem)obj.getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
        if (animItem != null) {
          animItem.setVisible(state && realEnabled);
          if (isRealAnimationEnabled() && isRealAnimationPlaying()) {
            animItem.play();
          } else {
            animItem.stop();
          }
        }
      }
    }
  }

  /**
   * Returns the currently active interpolation type for real animations.
   * @return Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR, ViewerConstants.TYPE_NEAREST_BILINEAR
   *         or ViewerConstants.TYPE_BICUBIC.
   */
  public int getRealAnimationInterpolation()
  {
    return interpolationType;
  }

  /**
   * Sets the interpolation type for real animations
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealAnimationInterpolation(int interpolationType)
  {
    if (interpolationType != this.interpolationType) {
      this.interpolationType = interpolationType;
      List<LayerObjectAnimation> list = getLayerObjects();
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          AnimatedLayerItem item = (AnimatedLayerItem)list.get(i).getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
          if (item != null) {
            item.setInterpolationType(this.interpolationType);
          }
        }
      }
    }
  }

  /**
   * Returns whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public boolean isRealAnimationForcedInterpolation()
  {
    return forcedInterpolation;
  }

  /**
   * Specify whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public void setRealAnimationForcedInterpolation(boolean forced)
  {
    if (forced != forcedInterpolation) {
      forcedInterpolation = forced;
      List<LayerObjectAnimation> list = getLayerObjects();
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          AnimatedLayerItem item = (AnimatedLayerItem)list.get(i).getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
          if (item != null) {
            item.setForcedInterpolation(forcedInterpolation);
          }
        }
      }
    }
  }

  /**
   * Returns whether real animation items or iconic animation items are enabled.
   * @return If <code>true</code>, real animation items are enabled.
   *         If <code>false</code>, iconic animation items are enabled.
   */
  public boolean isRealAnimationEnabled()
  {
    return realEnabled;
  }

  /**
   * Specify whether iconic animation type or real animation type is enabled.
   * @param enable If <code>true</code>, real animation items will be shown.
   *               If <code>false</code>, iconic animation items will be shown.
   */
  public void setRealAnimationEnabled(boolean enable)
  {
    if (enable != realEnabled) {
      realEnabled = enable;
      if (isLayerVisible()) {
        setLayerVisible(isLayerVisible());
      }
    }
  }

  /**
   * Returns whether real animation items are enabled and animated.
   */
  public boolean isRealAnimationPlaying()
  {
    return realEnabled && realPlaying;
  }

  /**
   * Specify whether real animation should be animated. Setting to <code>true</code> will enable
   * real animations automatically.
   */
  public void setRealAnimationPlaying(boolean play)
  {
    if (play != realPlaying) {
      realPlaying = play;
      if (realPlaying && !realEnabled) {
        realEnabled = true;
      }
      if (isLayerVisible()) {
        setLayerVisible(isLayerVisible());
      }
    }
  }

  /**
   * Returns the current frame visibility.
   * @return One of ViewerConstants.FRAME_NEVER, ViewerConstants.FRAME_AUTO or ViewerConstants.FRAME_ALWAYS.
   */
  public int getRealAnimationFrameState()
  {
    return frameState;
  }

  /**
   * Specify the frame visibility for real animations
   * @param state One of ViewerConstants.FRAME_NEVER, ViewerConstants.FRAME_AUTO or ViewerConstants.FRAME_ALWAYS.
   */
  public void setRealAnimationFrameState(int state)
  {
    switch (state) {
      case ViewerConstants.FRAME_NEVER:
      case ViewerConstants.FRAME_AUTO:
      case ViewerConstants.FRAME_ALWAYS:
      {
        if (frameState != state) {
          frameState = state;
          updateFrameState();
        }
        break;
      }
    }
  }

  private void updateFrameState()
  {
    List<LayerObjectAnimation> list = getLayerObjects();
    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        AnimatedLayerItem item = (AnimatedLayerItem)list.get(i).getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
        if (item != null) {
          switch (frameState) {
            case ViewerConstants.FRAME_NEVER:
              item.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, false);
              item.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, false);
              break;
            case ViewerConstants.FRAME_AUTO:
              item.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, false);
              item.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, true);
              break;
            case ViewerConstants.FRAME_ALWAYS:
              item.setFrameEnabled(AbstractLayerItem.ItemState.NORMAL, true);
              item.setFrameEnabled(AbstractLayerItem.ItemState.HIGHLIGHTED, true);
              break;
          }
        }
      }
    }
  }

  // TODO: add interpolation support
}
