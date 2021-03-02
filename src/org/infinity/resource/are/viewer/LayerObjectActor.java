// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Point;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.AnimatedLayerItem;
import org.infinity.gui.layeritem.BasicAnimationProvider;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.graphics.BamDecoder;

/**
 * Base class for layer type: Actor
 */
public abstract class LayerObjectActor extends LayerObject
{
  /** Available creature allegiance types. */
  protected enum Allegiance {
    GOOD,
    NEUTRAL,
    ENEMY,
  }

  protected static final Color COLOR_FRAME_NORMAL = new Color(0xA02020FF, true);
  protected static final Color COLOR_FRAME_HIGHLIGHTED = new Color(0xFF2020FF, false);

  // Default animation sequence to load if available; fall back to the first available sequence if no default is available
  private static final SpriteDecoder.Sequence[] DEFAULT_SEQUENCE = {
      SpriteDecoder.Sequence.STAND,
      SpriteDecoder.Sequence.STAND2,
      SpriteDecoder.Sequence.STAND3,
      SpriteDecoder.Sequence.STAND_EMERGED,
      SpriteDecoder.Sequence.PST_STAND,
      SpriteDecoder.Sequence.STANCE,
      SpriteDecoder.Sequence.STANCE2,
      SpriteDecoder.Sequence.PST_STANCE,
      SpriteDecoder.Sequence.WALK,
      SpriteDecoder.Sequence.PST_WALK,
  };
  // Potential sequences for "death" state
  private static final SpriteDecoder.Sequence[] DEATH_SEQUENCE = {
      SpriteDecoder.Sequence.TWITCH,
      SpriteDecoder.Sequence.DIE,
      SpriteDecoder.Sequence.PST_DIE_FORWARD,
      SpriteDecoder.Sequence.PST_DIE_BACKWARD,
      SpriteDecoder.Sequence.PST_DIE_COLLAPSE,
  };
  // Potential sequences for "unconscious" state
  private static final SpriteDecoder.Sequence[] SLEEP_SEQUENCE = {
      SpriteDecoder.Sequence.SLEEP,
      SpriteDecoder.Sequence.SLEEP2,
      SpriteDecoder.Sequence.TWITCH,
      SpriteDecoder.Sequence.DIE,
      SpriteDecoder.Sequence.PST_DIE_FORWARD,
      SpriteDecoder.Sequence.PST_DIE_BACKWARD,
      SpriteDecoder.Sequence.PST_DIE_COLLAPSE,
  };

  protected final Point location = new Point();
  protected final AbstractLayerItem[] items = new AbstractLayerItem[2];



  protected LayerObjectActor(Class<? extends AbstractStruct> classType, AbstractStruct parent)
  {
    super("Actor", classType, parent);
  }

  //<editor-fold defaultstate="collapsed" desc="LayerObject">
  @Override
  public void close()
  {
    super.close();
    // removing cached references
    for (int i = 0; i < items.length; i++) {
      Object key = items[i].getData();
      if (key != null) {
        switch (i) {
          case ViewerConstants.ITEM_ICON:
            SharedResourceCache.remove(SharedResourceCache.Type.ICON, key);
            break;
          case ViewerConstants.ITEM_REAL:
            SharedResourceCache.remove(SharedResourceCache.Type.ACTOR, key);
            break;
        }
      }
    }
  }

  /**
   * Returns the layer item of the specific state. (either ACTOR_ITEM_ICON or ACTOR_ITEM_REAL).
   * @param type The state of the item to be returned.
   * @return The desired layer item, or {@code null} if not available.
   */
  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    type = (type == ViewerConstants.ITEM_REAL) ? ViewerConstants.ITEM_REAL : ViewerConstants.ITEM_ICON;
    return items[type];
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return items;
  }

  @Override
  public void update(double zoomFactor)
  {
    for (int i = 0; i < items.length; i++) {
      items[i].setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                               (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
      if (i == ViewerConstants.ITEM_REAL) {
        ((AnimatedLayerItem)items[i]).setZoomFactor(zoomFactor);
      }
    }
  }

  /**
   * Loads animation data if it hasn't been loaded yet.
   */
  public abstract void loadAnimation();

  /**
   * Sets the lighting condition of the actor. Does nothing if the actor is flagged as
   * self-illuminating.
   * @param dayTime One of the constants: {@code TilesetRenderer.LIGHTING_DAY},
   *                {@code TilesetRenderer.LIGHTING_TWILIGHT}, {@code TilesetRenderer.LIGHTING_NIGHT}.
   */
  public void setLighting(int dayTime)
  {
    AnimatedLayerItem item = (AnimatedLayerItem)items[ViewerConstants.ITEM_REAL];
    BasicAnimationProvider provider = item.getAnimation();
    if (provider instanceof ActorAnimationProvider) {
      ActorAnimationProvider anim = (ActorAnimationProvider)provider;
      anim.setLighting(dayTime);
    }
    item.repaint();
  }

  /** Returns the allegiance of the specified EA value. */
  protected static Allegiance getAllegiance(int ea)
  {
    if (ea >= 2 && ea <= 30) {
      return Allegiance.GOOD;
    } else if (ea >= 200) {
      return Allegiance.ENEMY;
    } else {
      return Allegiance.NEUTRAL;
    }
  }

  /**
   * Creates an {@code AnimationProvider} object and initializes it with the creature animation defined by the
   * specified CRE resource.
   * @param cre the CRE resource
   * @return a initialized {@link ActorAnimationProvider} instance
   * @throws Exception if animation provider could not be created or initialized.
   */
  protected static ActorAnimationProvider createAnimationProvider(CreResource cre) throws Exception
  {
    Objects.requireNonNull(cre);
    ActorAnimationProvider retVal = null;
    SpriteDecoder decoder = null;

    final int maskDeath = 0xfc0;  // actor is dead?
    final int maskSleep = 0x1;    // actor is unconscious?
    int status = ((IsNumeric)cre.getAttribute(CreResource.CRE_STATUS)).getValue();
    boolean isDead = (status & maskDeath) != 0;
    boolean isUnconscious = (status & maskSleep) != 0;

    // loading SpriteDecoder instance from cache if available
    String key = createKey(cre);
    if (!SharedResourceCache.contains(SharedResourceCache.Type.ACTOR, key)) {
      // create new
      decoder = SpriteDecoder.importSprite(cre);
      decoder.setSelectionCircleEnabled(Settings.ShowActorSelectionCircle);
      decoder.setPersonalSpaceVisible(Settings.ShowActorPersonalSpace);

      SpriteDecoder.Sequence sequence = null;

      // check for special animation sequence
      if (isDead) {
        sequence = getMatchingSequence(decoder, DEATH_SEQUENCE);
      } else if (isUnconscious) {
        sequence = getMatchingSequence(decoder, SLEEP_SEQUENCE);
      }

      if (sequence == null) {
        // improve visualization of flying creatures
        if (decoder.getAnimationType() == SpriteDecoder.AnimationType.FLYING &&
            decoder.isSequenceAvailable(SpriteDecoder.Sequence.WALK)) {
          sequence = SpriteDecoder.Sequence.WALK;
        }
      }

      if (sequence == null) {
        // determine default animation sequence to load
        sequence = getMatchingSequence(decoder, DEFAULT_SEQUENCE);
      }

      if (sequence == null) {
        // use first animation sequence if no default sequence is available
        sequence = getMatchingSequence(decoder, null);
      }

      if (sequence != null) {
        decoder.loadSequence(sequence);
      } else {
        String creName = "";
        if (cre.getResourceEntry() != null) {
          creName =  cre.getResourceEntry().getResourceName();
        } else if (cre.getName() != null) {
          creName = cre.getName();
        }
        throw new UnsupportedOperationException("Could not find animation sequence for CRE: " + creName);
      }

      SharedResourceCache.add(SharedResourceCache.Type.ACTOR, key, new ResourceAnimation(key, decoder));
    } else {
      // use existing
      SharedResourceCache.add(SharedResourceCache.Type.ACTOR, key);
      BamDecoder bam = ((ResourceAnimation)SharedResourceCache.get(SharedResourceCache.Type.ACTOR, key)).getData();
      if (bam instanceof SpriteDecoder) {
        decoder = (SpriteDecoder)bam;
      } else {
        throw new Exception("Could not load actor animation");
      }
    }

    // initial settings
    retVal = new ActorAnimationProvider(decoder);
    retVal.setActive(true);

    if (isDead || isUnconscious) {
      // using second last frame to avoid glitches for selected creature animations
      retVal.setStartFrame(-2);
      retVal.setFrameCap(-2);
      retVal.setLooping(false);
    } else {
      retVal.setLooping(true);
    }

    return retVal;
  }

  /** Returns a key which is identical for all actors based on the same CRE resource. */
  protected static String createKey(CreResource cre)
  {
    String retVal;
    Objects.requireNonNull(cre);

    if (cre.getResourceEntry() != null) {
      // regular CRE resource
      retVal = cre.getResourceEntry().getResourceName();
    } else if (cre.getParent() != null) {
      // CRE attached to ARE > Actor
      retVal = cre.getParent().getName();
    } else {
      // failsafe
      retVal = Integer.toString(cre.hashCode());
    }

    int status = ((IsNumeric)cre.getAttribute(CreResource.CRE_STATUS)).getValue();
    retVal += "?status=" + status;

    return retVal;
  }

  /** Returns the first matching animation sequence listed in {@code sequences} that is available in the {@code SpriteDecoder} instance. */
  protected static SpriteDecoder.Sequence getMatchingSequence(SpriteDecoder decoder, SpriteDecoder.Sequence[] sequences)
  {
    SpriteDecoder.Sequence retVal = null;
    if (sequences == null) {
      sequences = SpriteDecoder.Sequence.values();
    }

    if (decoder == null || sequences.length == 0) {
      return retVal;
    }

    for (final SpriteDecoder.Sequence seq : sequences) {
      if (decoder.isSequenceAvailable(seq)) {
        retVal = seq;
        break;
      }
    }

    return retVal;
  }

  //</editor-fold>
}
