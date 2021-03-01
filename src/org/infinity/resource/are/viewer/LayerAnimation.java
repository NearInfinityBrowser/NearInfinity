// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.infinity.datatype.Flag;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.are.Animation;
import org.infinity.resource.are.AreResource;
import static org.infinity.resource.are.AreResource.ARE_NUM_ANIMATIONS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_ANIMATIONS;

/**
 * Manages background animation layer objects.
 */
public class LayerAnimation extends BasicLayer<LayerObjectAnimation, AreResource>
{
  private static final String AvailableFmt = "Background animations: %d";

  private boolean realEnabled, realPlaying, forcedInterpolation, isAnimActiveIgnored;
  private int frameState;
  private Object interpolationType = ViewerConstants.TYPE_NEAREST_NEIGHBOR;
  private double frameRate = ViewerConstants.FRAME_AUTO;

  public LayerAnimation(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ANIMATION, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    // loading animations from ARE
    loadLayerItems(ARE_OFFSET_ANIMATIONS, ARE_NUM_ANIMATIONS,
                   Animation.class, a -> new LayerObjectAnimation(parent, a));

    // sorting entries (animations not flagged as "draw as background" come first)
    Collections.sort(getLayerObjects(), new Comparator<LayerObjectAnimation>() {
      @Override
      public int compare(LayerObjectAnimation o1, LayerObjectAnimation o2) {
        boolean isBackground1, isBackground2;
        try {
          isBackground1 = ((Flag)((Animation)o1.getViewable()).getAttribute(Animation.ARE_ANIMATION_APPEARANCE)).isFlagSet(8);
          isBackground2 = ((Flag)((Animation)o2.getViewable()).getAttribute(Animation.ARE_ANIMATION_APPEARANCE)).isFlagSet(8);
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
  }

  @Override
  public String getAvailability()
  {
    int cnt = getLayerObjectCount();
    return String.format(AvailableFmt, cnt);
  }

  /**
   * Sets the visibility state of all items in the layer. Takes enabled states of the different
   * item types into account.
   */
  @Override
  public void setLayerVisible(boolean visible)
  {
    setVisibilityState(visible);
    List<LayerObjectAnimation> list = getLayerObjects();
    for (int i = 0, size = list.size(); i < size; i++) {
      boolean state = isLayerVisible() && (!isScheduleEnabled() || isScheduled(i));
      LayerObjectAnimation obj = list.get(i);
      IconLayerItem iconItem = (IconLayerItem)obj.getLayerItem(ViewerConstants.ITEM_ICON);
      if (iconItem != null) {
        iconItem.setVisible(state && !realEnabled);
      }
      AnimatedLayerItem animItem = (AnimatedLayerItem)obj.getLayerItem(ViewerConstants.ITEM_REAL);
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
   * Sets the interpolation type for real animations.
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealAnimationInterpolation(Object interpolationType)
  {
    if (interpolationType != this.interpolationType) {
      this.interpolationType = interpolationType;
      for (final LayerObjectAnimation layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item != null) {
          item.setInterpolationType(interpolationType);
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
      for (final LayerObjectAnimation layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item != null) {
          item.setForcedInterpolation(forced);
        }
      }
    }
  }

  /**
   * Returns whether real animation items or iconic animation items are enabled.
   * @return If {@code true}, real animation items are enabled.
   *         If {@code false}, iconic animation items are enabled.
   */
  public boolean isRealAnimationEnabled()
  {
    return realEnabled;
  }

  /**
   * Specify whether iconic animation type or real animation type is enabled.
   * @param enable If {@code true}, real animation items will be shown.
   *               If {@code false}, iconic animation items will be shown.
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
   * Specify whether real animation should be animated. Setting to {@code true} will enable
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
      for (final LayerObjectAnimation layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item != null) {
          item.setFrameRate(frameRate);
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
    for (final LayerObjectAnimation layer : getLayerObjects()) {
      final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
      if (item != null) {
        if (item.getAnimation() instanceof AbstractAnimationProvider) {
          ((AbstractAnimationProvider)item.getAnimation()).setActiveIgnored(set);
        }
      }
    }
  }


  private void updateFrameState()
  {
    for (final LayerObjectAnimation layer : getLayerObjects()) {
      final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
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
