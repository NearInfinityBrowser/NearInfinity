// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import infinity.datatype.Flag;
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

  private boolean realEnabled, realPlaying, forcedInterpolation, isAnimActiveIgnored;
  private int frameState;
  private Object interpolationType;
  private double frameRate;

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
      close();
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

      // sorting entries (animations not flagged as "draw as background" come first)
      Collections.sort(list, new Comparator<LayerObjectAnimation>() {
        @Override
        public int compare(LayerObjectAnimation o1, LayerObjectAnimation o2) {
          boolean isBackground1, isBackground2;
          try {
            isBackground1 = ((Flag)((Animation)o1.getStructure()).getAttribute("Appearance")).isFlagSet(8);
            isBackground2 = ((Flag)((Animation)o2.getStructure()).getAttribute("Appearance")).isFlagSet(8);
          } catch (Exception e) {
            isBackground1 = false;
            isBackground2 = false;
          }
          if (!isBackground1 && isBackground2) {
            return -1;
          } else if (isBackground1 && !isBackground2) {
            return 1;
          } else {
            return 0;
          }
        }
      });

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
  public Object getRealAnimationInterpolation()
  {
    return interpolationType;
  }

  /**
   * Sets the interpolation type for real animations
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealAnimationInterpolation(Object interpolationType)
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
        frameState = state;
        updateFrameState();
        break;
      }
    }
  }

  /**
   * Returns the frame rate used for playing back background animations.
   * @return Frame rate in frames/second.
   */
  public double getRealAnimationFrameRate()
  {
    return frameRate;
  }

  /**
   * Specify a new frame rate for real animations.
   * @param frameRate Frame rate in frames/second.
   */
  public void setRealAnimationFrameRate(double frameRate)
  {
    frameRate = Math.min(Math.max(frameRate, 1.0), 30.0);
    if (frameRate != this.frameRate) {
      this.frameRate = frameRate;
      List<LayerObjectAnimation> list = getLayerObjects();
      if (list != null) {
        for (int i = 0; i < list.size(); i++) {
          AnimatedLayerItem item = (AnimatedLayerItem)list.get(i).getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
          if (item != null) {
            item.setFrameRate(this.frameRate);
          }
        }
      }
    }
  }

  /**
   * Returns whether the current activation states of real animations are ignored
   * (i.e. treated as always active).
   */
  public boolean isRealAnimationActiveIgnored()
  {
    return isAnimActiveIgnored;
  }

  /**
   * Sets whether the activation state of real animations are ignored (i.e. treated as always active).
   * @param set
   */
  public void setRealAnimationActiveIgnored(boolean set)
  {
    isAnimActiveIgnored = set;
    List<LayerObjectAnimation> list = getLayerObjects();
    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        AnimatedLayerItem item = (AnimatedLayerItem)list.get(i).getLayerItem(ViewerConstants.ANIM_ITEM_REAL);
        if (item != null) {
          if (item.getAnimation() instanceof BackgroundAnimationProvider) {
            ((BackgroundAnimationProvider)item.getAnimation()).setActiveIgnored(isAnimActiveIgnored);
          }
        }
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
