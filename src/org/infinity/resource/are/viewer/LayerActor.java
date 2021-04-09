// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import org.infinity.datatype.IsTextual;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.gam.GamResource;
import org.infinity.resource.gam.PartyNPC;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;

import static org.infinity.resource.are.AreResource.ARE_NUM_ACTORS;
import static org.infinity.resource.are.AreResource.ARE_OFFSET_ACTORS;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;

/**
 * Manages actor layer objects.
 */
public class LayerActor extends BasicLayer<LayerObjectActor, AreResource> implements PropertyChangeListener
{
  private static final String AvailableFmt = "Actors: %d";

  private boolean realEnabled, realPlaying, forcedInterpolation, selectionCircleEnabled, personalSpaceEnabled;
  private int frameState;
  private Object interpolationType = ViewerConstants.TYPE_NEAREST_NEIGHBOR;
  private double frameRate = ViewerConstants.FRAME_AUTO;
  private SwingWorker<Void, Void> loadWorker;
  private WindowBlocker blocker;

  public LayerActor(AreResource are, AreaViewer viewer)
  {
    super(are, ViewerConstants.LayerType.ACTOR, viewer);
    loadLayer();
  }

  @Override
  protected void loadLayer()
  {
    // loading actors from ARE
    loadLayerItems(ARE_OFFSET_ACTORS, ARE_NUM_ACTORS,
                   Actor.class, a -> new LayerObjectAreActor(parent, a));

    final List<LayerObjectActor> objectList = getLayerObjects();
    // loading actors from associated INI
    final String iniFile = parent.getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH).replace(".ARE", ".INI");
    IniMap ini = ResourceFactory.resourceExists(iniFile) ? IniMapCache.get(iniFile) : null;
    if (ini != null) {
      for (final IniMapSection section : ini) {
        IniMapEntry creFile = section.getEntry("cre_file");
        IniMapEntry spawnPoint = section.getEntry("spawn_point");
        if (creFile != null && spawnPoint != null) {
          String[] position = IniMapEntry.splitValues(spawnPoint.getValue(), IniMapEntry.REGEX_POSITION);
          for (int j = 0; j < position.length; j++) {
            try {
              PlainTextResource iniRes = new PlainTextResource(ResourceFactory.getResourceEntry(iniFile));
              LayerObjectActor obj = new LayerObjectIniActor(iniRes, section, j);
              setListeners(obj);
              objectList.add(obj);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    // loading global actors from save's baldur.gam or default .gam
    ResourceEntry areEntry = parent.getResourceEntry();
    if (areEntry != null) {
      // loading associated GAM resource
      ResourceEntry gamEntry = null;
      Path arePath = areEntry.getActualPath();
      if (arePath != null) {
        Path gamPath = arePath.getParent().resolve((String)Profile.getProperty(Profile.Key.GET_GAM_NAME));
        if (Files.isRegularFile(gamPath)) {
          gamEntry = new FileResourceEntry(gamPath, false);
        }
      }
      if (gamEntry == null) {
        gamEntry = ResourceFactory.getResourceEntry(Profile.getProperty(Profile.Key.GET_GAM_NAME));
      }

      // scanning global NPCs
      if (gamEntry != null) {
        Resource res = ResourceFactory.getResource(gamEntry);
        if (res instanceof GamResource) {
          GamResource gamRes = (GamResource)res;
          List<StructEntry> npcList = gamRes.getFields(PartyNPC.class);
          for (int i = 0, cnt = npcList.size(); i < cnt; i++) {
            PartyNPC npc = (PartyNPC)npcList.get(i);
            String area = ((IsTextual)npc.getAttribute(PartyNPC.GAM_NPC_CURRENT_AREA)).getText();
            if (areEntry.getResourceRef().equalsIgnoreCase(area)) {
              try {
                LayerObjectActor loa = new LayerObjectGlobalActor(gamRes, npc);
                setListeners(loa);
                objectList.add(loa);
              } catch (Exception e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }

    // sorting entries by vertical position to fix overlapping issues
    getLayerObjects().sort((c1, c2) -> c2.location.y - c1.location.y);
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

    loadWorker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground()
      {
        final List<LayerObjectActor> list = getLayerObjects();
        final ProgressMonitor progress = new ProgressMonitor(getViewer(), "Loading actor animations...", "0 %", 0, list.size());
        progress.setMillisToDecideToPopup(500);
        progress.setMillisToPopup(1000);

        try {
          int visualState = getViewer().getVisualState();
          for (int i = 0, size = list.size(); i < size; i++) {
            boolean state = isLayerVisible() && (!isScheduleEnabled() || isScheduled(i));
            LayerObjectActor loa = list.get(i);

            IconLayerItem iconItem = (IconLayerItem)loa.getLayerItem(ViewerConstants.ITEM_ICON);
            if (iconItem != null) {
              iconItem.setVisible(state && !realEnabled);
            }

            AnimatedLayerItem animItem = (AnimatedLayerItem)loa.getLayerItem(ViewerConstants.ITEM_REAL);
            if (animItem != null) {
              if (animItem.getAnimation() == AbstractAnimationProvider.DEFAULT_ANIMATION_PROVIDER && state && realEnabled) {
                // real actor animations loaded on demand
                if (blocker == null) {
                  blocker = new WindowBlocker(getViewer());
                  blocker.setBlocked(true);
                }
                loa.loadAnimation();
                loa.setLighting(visualState);
              }

              animItem.setVisible(state && realEnabled);
              if (isRealActorEnabled() && isRealActorPlaying()) {
                animItem.play();
              } else {
                animItem.stop();
              }
            }
            progress.setNote(((i + 1) * 100 / size) + " %");
            progress.setProgress(i + 1);
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          progress.close();
          if (blocker != null) {
            blocker.setBlocked(false);
            blocker = null;
          }
        }
        return null;
      }
    };

    loadWorker.addPropertyChangeListener(this);
    loadWorker.execute();
  }

//--------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent e)
  {
    if (e.getSource() == loadWorker) {
      if ("state".equals(e.getPropertyName()) &&
          SwingWorker.StateValue.DONE == e.getNewValue()) {
        loadWorker = null;
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

  /**
   * Returns the currently active interpolation type for real actors.
   * @return Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR, ViewerConstants.TYPE_NEAREST_BILINEAR
   *         or ViewerConstants.TYPE_BICUBIC.
   */
  public Object getRealActorInterpolation()
  {
    return interpolationType;
  }

  /**
   * Sets the interpolation type for real actors.
   * @param interpolationType Either one of ViewerConstants.TYPE_NEAREST_NEIGHBOR,
   *                          ViewerConstants.TYPE_NEAREST_BILINEAR or ViewerConstants.TYPE_BICUBIC.
   */
  public void setRealActorInterpolation(Object interpolationType)
  {
    if (interpolationType != this.interpolationType) {
      this.interpolationType = interpolationType;
      for (final LayerObjectActor layer : getLayerObjects()) {
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
  public boolean isRealActorForcedInterpolation()
  {
    return forcedInterpolation;
  }

  /**
   * Specify whether to force the specified interpolation type or use the best one available, depending
   * on the current zoom factor.
   */
  public void setRealActorForcedInterpolation(boolean forced)
  {
    if (forced != forcedInterpolation) {
      forcedInterpolation = forced;
      for (final LayerObjectActor layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item != null) {
          item.setForcedInterpolation(forced);
        }
      }
    }
  }

  /**
   * Returns whether real actor items or iconic actor items are enabled.
   * @return If {@code true}, real actor items are enabled.
   *         If {@code false}, iconic actor items are enabled.
   */
  public boolean isRealActorEnabled()
  {
    return realEnabled;
  }

  /**
   * Specify whether iconic actor type or real actor type is enabled.
   * @param enable If {@code true}, real actor items will be shown.
   *               If {@code false}, iconic actor items will be shown.
   */
  public void setRealActorEnabled(boolean enable)
  {
    if (enable != realEnabled) {
      realEnabled = enable;
      if (isLayerVisible()) {
        setLayerVisible(isLayerVisible());
      }
    }
  }

  /**
   * Returns whether real actor items are enabled and animated.
   */
  public boolean isRealActorPlaying()
  {
    return realEnabled && realPlaying;
  }

  /**
   * Specify whether real actor should be animated. Setting to {@code true} will enable
   * real actors automatically.
   */
  public void setRealActorPlaying(boolean play)
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
  public int getRealActorFrameState()
  {
    return frameState;
  }

  /**
   * Specify the frame visibility for real actors
   * @param state One of ViewerConstants.FRAME_NEVER, ViewerConstants.FRAME_AUTO or ViewerConstants.FRAME_ALWAYS.
   */
  public void setRealActorFrameState(int state)
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
   * Returns whether selection circle of actor sprites is enabled.
   */
  public boolean isRealActorSelectionCircleEnabled()
  {
    return selectionCircleEnabled;
  }

  /**
   * Specify whether selection circle of actor sprites is enabled.
   */
  public void setRealActorSelectionCircleEnabled(boolean enable)
  {
    if (enable != selectionCircleEnabled) {
      selectionCircleEnabled = enable;
      for (final LayerObjectActor layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item.getAnimation() instanceof ActorAnimationProvider) {
          ActorAnimationProvider aap = (ActorAnimationProvider)item.getAnimation();
          aap.setSelectionCircleEnabled(selectionCircleEnabled);
        }
      }
    }
  }

  /**
   * Returns whether personal space indicator of actor sprites is enabled.
   */
  public boolean isRealActorPersonalSpaceEnabled()
  {
    return personalSpaceEnabled;
  }

  /**
   * Specify whether personal space indicator of actor sprites is enabled.
   */
  public void setRealActorPersonalSpaceEnabled(boolean enable)
  {
    if (enable != personalSpaceEnabled) {
      personalSpaceEnabled = enable;
      for (final LayerObjectActor layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item.getAnimation() instanceof ActorAnimationProvider) {
          ActorAnimationProvider aap = (ActorAnimationProvider)item.getAnimation();
          aap.setPersonalSpaceEnabled(personalSpaceEnabled);
        }
      }
    }
  }

  /**
   * Returns the frame rate used for playing back actor sprites.
   * @return Frame rate in frames/second.
   */
  public double getRealActorFrameRate()
  {
    return frameRate;
  }

  /**
   * Specify a new frame rate for real actors.
   * @param frameRate Frame rate in frames/second.
   */
  public void setRealActorFrameRate(double frameRate)
  {
    frameRate = Math.min(Math.max(frameRate, 1.0), 30.0);
    if (frameRate != this.frameRate) {
      this.frameRate = frameRate;
      for (final LayerObjectActor layer : getLayerObjects()) {
        final AnimatedLayerItem item = (AnimatedLayerItem)layer.getLayerItem(ViewerConstants.ITEM_REAL);
        if (item != null) {
          item.setFrameRate(frameRate);
        }
      }
    }
  }

  private void updateFrameState()
  {
    for (final LayerObjectActor layer : getLayerObjects()) {
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
