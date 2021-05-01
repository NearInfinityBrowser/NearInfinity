// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.util.EnumMap;
import java.util.Objects;

import org.infinity.resource.key.ResourceEntry;

/**
 * Definition of a single segment within a cycle definition.
 */
public class SegmentDef implements Cloneable
{
  /** Indicates the sprite type for this particular BAM segment (such as avatar, weapon, shield or helmet). */
  public enum SpriteType {
    /** Indicate that the current segment belongs to the creature animation. */
    AVATAR,
    /** Indicate that the current segment belongs to the weapon overlay. */
    WEAPON,
    /** Indicate that the current segment belongs to the shield overlay. */
    SHIELD,
    /** Indicate that the current segment belongs to the helmet overlay. */
    HELMET,
  }

  /**
   * Indicates the playback behavior of this particular BAM segment.
   */
  public enum Behavior {
    /**
     * Segment runs from start to end and repeats the process if other segments of the current cycle contain
     * more frames. This is the default if no behavior has been specified.
     */
    REPEAT,
    /**
     * Segment runs from start to end and stops even if other segments of the current cycle contain more frames.
     */
    SINGLE,
    /**
     * Segment runs from start to end and continues to provide the last frame if other segments of the
     * current cycle contain more frames.
     */
    FREEZE,
    /**
     * Segment runs from start to the minimum number of cycle frames available in all segment definitions and stops.
     */
    CUT,
    /** The same as {@link #REPEAT} but runs from end to start. */
    REVERSE_REPEAT,
    /** The same as {@link #SINGLE} but runs from end to start. */
    REVERSE_SINGLE,
    /** The same as {@link #FREEZE} but runs from end to start. */
    REVERSE_FREEZE,
    /** The same as {@link #CUT} but runs from end to start. */
    REVERSE_CUT;

    private static final EnumMap<Behavior, Behavior> opposites = new EnumMap<Behavior, Behavior>(Behavior.class) {{
      put(REPEAT, REVERSE_REPEAT);
      put(SINGLE, REVERSE_SINGLE);
      put(FREEZE, REVERSE_FREEZE);
      put(CUT, REVERSE_CUT);
      put(REVERSE_REPEAT, REPEAT);
      put(REVERSE_SINGLE, SINGLE);
      put(REVERSE_FREEZE, FREEZE);
      put(REVERSE_CUT, CUT);
    }};

    /** Returns the opposite value of the specified behavior regarding frame playback. */
    public static Behavior getOpposite(Behavior b)
    {
      return opposites.getOrDefault(b, REVERSE_REPEAT);
    }
  }

  private final ResourceEntry bamEntry;
  private final int cycleIndex;
  private final SpriteType type;
  private final Behavior behavior;
  private final int numFrames;
  private final Composite composite;

  private CycleDef parent;
  private int curFrame;

  /**
   * Determines the behavior variant specified by the given BAM animation suffix.
   * If an exclamation mark is found it returns the opposite of the default behavior.
   * Otherwise it will return the default behavior.
   */
  public static Behavior getBehaviorOf(String suffix)
  {
    return getBehaviorOf(suffix, null);
  }

  /**
   * Determines the behavior variant specified by the given BAM animation suffix.
   * If an exclamation mark is found it returns the opposite of the specified behavior.
   * Otherwise it will return the specified behavior.
   */
  public static Behavior getBehaviorOf(String suffix, Behavior behavior)
  {
    Behavior retVal = (behavior != null) ? behavior : Behavior.REPEAT;
    if (suffix != null && suffix.indexOf('!') >= 0) {
      retVal = Behavior.getOpposite(retVal);
    }
    return retVal;
  }

  /** Returns the specified string without any behavior markers. */
  public static String fixBehaviorSuffix(String suffix)
  {
    String retVal = suffix;
    if (retVal != null && retVal.indexOf('!') >= 0) {
      retVal = retVal.replace("!", "");
    }
    return retVal;
  }

  /**
   * Creates a new independent segment definition with the specified parameters.
   * Behavior is assumed to be {@link Behavior#REPEAT}. Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the sprite type of this segment.
   */
  public SegmentDef(ResourceEntry entry, int cycleIdx, SpriteType type)
  {
    this(null, entry, cycleIdx, type, null, null);
  }

  /**
   * Creates a new independent segment definition with the specified parameters.
   * Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the sprite type of this segment.
   * @param behavior the playback behavior of this segment.
   */
  public SegmentDef(ResourceEntry entry, int cycleIdx, SpriteType type, Behavior behavior)
  {
    this(null, entry, cycleIdx, type, behavior, null);
  }

  /**
   * Creates a new independent segment definition with the specified parameters.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the sprite type of this segment.
   * @param behavior the playback behavior of this segment.
   * @param composite the {@link Composite} object used for rendering sprite frames onto the canvas.
   */
  public SegmentDef(ResourceEntry entry, int cycleIdx, SpriteType type, Behavior behavior, Composite composite)
  {
    this(null, entry, cycleIdx, type, behavior, composite);
  }

  /**
   * Creates a new segment definition with the specified parameters linked to the specified {@link CycleDef} instance.
   * Behavior is assumed to be {@link Behavior#REPEAT}. Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param parent the parent {@code CycleDef} instance.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the sprite type of this segment.
   */
  public SegmentDef(CycleDef parent, ResourceEntry entry, int cycleIdx, SpriteType type)
  {
    this(parent, entry, cycleIdx, type, null, null);
  }

  /**
   * Creates a new segment definition with the specified parameters linked to the specified {@link CycleDef} instance.
   * Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param parent the parent {@code CycleDef} instance.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the spriter type of this segment.
   * @param behavior the playback behavior of this segment.
   */
  public SegmentDef(CycleDef parent, ResourceEntry entry, int cycleIdx, SpriteType type, Behavior behavior)
  {
    this(parent, entry, cycleIdx, type, behavior, null);
  }

  /**
   * Creates a new segment definition with the specified parameters linked to the specified {@link CycleDef} instance.
   * @param parent the parent {@code CycleDef} instance.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle index
   * @param type the spriter type of this segment.
   * @param behavior the playback behavior of this segment.
   * @param composite the {@link Composite} object used for rendering sprite frames onto the canvas.
   */
  public SegmentDef(CycleDef parent, ResourceEntry entry, int cycleIdx, SpriteType type, Behavior behavior, Composite composite)
  {
    this.parent = parent;
    this.bamEntry = Objects.requireNonNull(entry, "BAM resource entry cannot be null");
    this.cycleIndex = cycleIdx;
    this.type = (type != null) ? type : SpriteType.AVATAR;
    this.behavior = (behavior != null) ? behavior : Behavior.REPEAT;
    this.numFrames = SpriteUtils.getBamCycleFrames(this.bamEntry, this.cycleIndex);
    this.composite = (composite != null) ? composite : AlphaComposite.SrcOver;
    reset();
  }

  /**
   * Creates a new segment definition with the attributes defined in the specified {@code SegmentDef} argument.
   * Parent attribute is set to {@code null}.
   * @param sd the {@code SegmentDef} object to clone.
   */
  public SegmentDef(SegmentDef sd)
  {
    Objects.requireNonNull(sd, "SegmentDef instance cannot be null");
    this.parent = null;
    this.bamEntry = sd.bamEntry;
    this.cycleIndex = sd.cycleIndex;
    this.type = sd.type;
    this.behavior = sd.behavior;
    this.numFrames = sd.numFrames;
    this.curFrame = sd.curFrame;
    this.composite = sd.composite;
  }

  /** Returns the parent {@link CycleDef} instance linked with this object. */
  public CycleDef getParent() { return parent; }

  /** Updates the parent link. Should not be called directly. */
  void setParent(CycleDef parent) { this.parent = parent; }

  /** Returns the BAM resource entry associated with the segment. */
  public ResourceEntry getEntry() { return bamEntry; }

  /** Returns the {@link Composite} object used to render the sprite frames onto a canvas. */
  public Composite getComposite() { return composite; }

  /** Returns the cycle index associated with the segment */
  public int getCycleIndex() { return cycleIndex; }

  /** Returns the sprite type assigned to the segment. */
  public SpriteType getSpriteType() { return type; }

  /** Returns the playback behavior for the current segment. */
  public Behavior getBehavior() { return behavior; }

  /** Returns the total number of frames in this cycle. */
  public int getFrameCount() { return numFrames; }

  /** Returns the current cycle frame. Returns -1 if no frame is currently selected. */
  public int getCurrentFrame() { return curFrame; }

  /** Advances the segment animation by one frame according to {@link Behavior}. */
  public void advance()
  {
    switch (behavior) {
      case SINGLE:
        if (curFrame >= 0 && curFrame < numFrames - 1) {
          curFrame++;
        } else {
          curFrame = -1;
        }
        break;
      case FREEZE:
        if (curFrame < numFrames - 1) {
          curFrame++;
        }
        break;
      case CUT:
        if (curFrame < getMinimumFrames() - 1) {
          curFrame++;
        } else {
          curFrame = -1;
        }
        break;
      case REVERSE_REPEAT:
        if (curFrame > 0) {
          curFrame--;
        } else if (curFrame <= 0) {
          curFrame = numFrames - 1;
        } else if (curFrame >= getMaximumFrames() - 1) {
          curFrame = numFrames - 1;
        }
        break;
      case REVERSE_SINGLE:
        if (curFrame > 0 && curFrame < numFrames) {
          curFrame--;
        } else {
          curFrame = -1;
        }
        break;
      case REVERSE_FREEZE:
        if (curFrame > 0) {
          curFrame--;
        } else {
          curFrame = -1;
        }
        break;
      case REVERSE_CUT:
        if (curFrame >= numFrames - getMinimumFrames() + 1) {
          curFrame--;
        } else {
          curFrame = -1;
        }
        break;
      default:  // Repeat
        if (curFrame < numFrames - 1) {
          curFrame++;
        } else if (curFrame >= numFrames - 1) {
          curFrame = 0;
        } else if (curFrame >= getMaximumFrames() - 1) {
          curFrame = 0;
        }
    }
  }

  /** Resets the BAM cycle back to the "first" frame as defined by {@link Behavior}. */
  public void reset()
  {
    switch (behavior) {
      case REVERSE_REPEAT:
      case REVERSE_SINGLE:
      case REVERSE_FREEZE:
      case REVERSE_CUT:
        curFrame = numFrames - 1;
        break;
      default:
        curFrame = 0;
    }
  }

  @Override
  public SegmentDef clone()
  {
    return new SegmentDef(this);
  }

  @Override
  public String toString()
  {
    return "entry=" + bamEntry.toString() + ", cycle=" + cycleIndex + ", type=" + type.toString() +
           ", behavior=" + behavior.toString() + ", numFrames=" + numFrames + ", curFrame=" + curFrame;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((bamEntry == null) ? 0 : bamEntry.hashCode());
    hash = 31 * hash + cycleIndex;
    hash = 31 * hash + ((type == null) ? 0 : type.hashCode());
    hash = 31 * hash + ((behavior == null) ? 0 : behavior.hashCode());
    hash = 31 * hash + numFrames;
    hash = 31 * hash + curFrame;
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof SegmentDef)) {
      return false;
    }
    SegmentDef other = (SegmentDef)o;
    boolean retVal = (this.bamEntry == null && other.bamEntry == null) ||
                     (this.bamEntry != null && this.bamEntry.equals(other.bamEntry));
    retVal &= (this.cycleIndex == other.cycleIndex);
    retVal &= (this.type == null && other.type == null) ||
              (this.type != null && this.type.equals(other.type));
    retVal &= (this.behavior == null && other.behavior == null) ||
              (this.behavior != null && this.behavior.equals(other.behavior));
    retVal &= (this.numFrames == other.numFrames);
    retVal &= (this.curFrame == other.curFrame);
    return retVal;
  }

  // Determines the minimum number of frames in the whole list of segment definitions.
  private int getMinimumFrames()
  {
    return (parent != null) ? parent.getMinimumFrames() : 0;
  }

  // Determines the maximum number of frames in the whole list of segment definitions.
  private int getMaximumFrames()
  {
    return (parent != null) ? parent.getMaximumFrames() : numFrames;
  }
}
