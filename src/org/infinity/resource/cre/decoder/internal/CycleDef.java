// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.cre.decoder.internal.SegmentDef.Behavior;
import org.infinity.resource.cre.decoder.internal.SegmentDef.SpriteType;
import org.infinity.resource.key.ResourceEntry;

/**
 * Definition of a single cycle for a specific direction in an animation sequence.
 */
public class CycleDef
{
  private final ArrayList<SegmentDef> cycles = new ArrayList<>();

  private DirDef parent;

  /**
   * Creates a new independent cycle definition with the specified parameters.
   * Sprite type is assumed to be {@link SpriteType#AVATAR}. Behavior is assumed to be {@link Behavior#REPEAT}.
   * @param bamResource the BAM resource
   * @param cycle the BAM cycle index
   */
  public CycleDef(ResourceEntry bamResource, int cycle)
  {
    this(null, bamResource, cycle, null, null);
  }

  /**
   * Creates a new independent cycle definition with the specified parameters.
   * Behavior is assumed to be {@link Behavior#REPEAT}.
   * @param bamResource the BAM resource
   * @param cycle the BAM cycle index
   * @param type the sprite type of the specified segment.
   */
  public CycleDef(ResourceEntry bamResource, int cycle, SegmentDef.SpriteType type)
  {
    this(null, bamResource, cycle, type, null);
  }

  /**
   * Creates a new independent cycle definition with the specified parameters.
   * Sprite type is assumed to be {@link SpriteType#AVATAR}.
   * @param bamResource the BAM resource
   * @param cycle the BAM cycle index
   * @param behavior the playback behavior of the BAM cycle
   */
  public CycleDef(ResourceEntry bamResource, int cycle, SegmentDef.Behavior behavior)
  {
    this(null, bamResource, cycle, null, behavior);
  }

  /**
   * Creates a new independent cycle definition with the specified parameters.
   * @param bamResource the BAM resource
   * @param cycle the BAM cycle index
   * @param type the sprite type of the specified segment
   * @param behavior the playback behavior of the BAM cycle
   */
  public CycleDef(ResourceEntry bamResource, int cycle, SegmentDef.SpriteType type, SegmentDef.Behavior behavior)
  {
    this(null, bamResource, cycle, type, behavior);
  }

  /**
   * Creates a new cycle definition with the specified parameters linked to the specified {@link DirDef} instance.
   * @param parent the parent {@code DirDef} instance
   * @param bamResource the BAM resource
   * @param cycle the BAM cycle index
   * @param type the sprite type of the specified segment
   * @param behavior the playback behavior of the BAM cycle
   */
  public CycleDef(DirDef parent, ResourceEntry bamResource, int cycle, SegmentDef.SpriteType type, SegmentDef.Behavior behavior)
  {
    this.parent = parent;
    addCycle(bamResource, cycle, type, behavior);
  }

  public CycleDef(DirDef parent, Collection<SegmentDef> cycleInfo)
  {
    this.parent = parent;
    addCycles(cycleInfo);
  }

  /** Returns the parent {@link DirDef} instance linked to this object. */
  public DirDef getParent() { return parent; }

  /** Updates the parent link. Should not be called directly. */
  void setParent(DirDef parent) { this.parent = parent; }

  /** Provides access to the list of segment definitions for this cycle. */
  public List<SegmentDef> getCycles() { return cycles; }

  /** Adds a new cycle definition. */
  public void addCycle(ResourceEntry bamResource, int cycle)
  {
    addCycle(bamResource, cycle, null, null);
  }

  /** Adds a new cycle definition. */
  public void addCycle(ResourceEntry bamResource, int cycle, SegmentDef.SpriteType type)
  {
    addCycle(bamResource, cycle, type, null);
  }

  /** Adds a new cycle definition. */
  public void addCycle(ResourceEntry bamResource, int cycle, SegmentDef.SpriteType type, SegmentDef.Behavior behavior)
  {
    this.cycles.add(new SegmentDef(this, bamResource, cycle, type, behavior));
  }

  /** Adds new cycle definitions. */
  public void addCycles(Collection<SegmentDef> cycleInfo)
  {
    for (Iterator<SegmentDef> iter = cycleInfo.iterator(); iter.hasNext();) {
      final SegmentDef sd = Objects.requireNonNull(iter.next(), "Segment definition cannot be null");
      if (!this.cycles.stream().anyMatch(sd2 -> sd2.equals(sd))) {
        sd.setParent(this);
        this.cycles.add(sd);
      }
    }
  }

  /** Advances the animation by one frame for all segment definitions according to their respective behavior. */
  public void advance()
  {
    this.cycles.forEach(sd -> sd.advance());
  }

  /** Resets BAM cycles in all segment definitions back to the first frame. */
  public void reset()
  {
    this.cycles.forEach(sd -> sd.reset());
  }

  /** Determines the minimum number of frames in the whole list of segment definitions. */
  public int getMinimumFrames()
  {
    return cycles.stream().mapToInt(v -> v.getFrameCount()).min().orElse(0);
  }

  /** Determines the maximum number of frames in the whole list of segment definitions. */
  public int getMaximumFrames()
  {
    return cycles.stream().mapToInt(v -> v.getFrameCount()).max().orElse(0);
  }

  @Override
  public String toString()
  {
    return "cycles=" + cycles.toString();
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((cycles == null) ? 0 : cycles.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof CycleDef)) {
      return false;
    }
    CycleDef other = (CycleDef)o;
    return (this.cycles == null && other.cycles == null) ||
           (this.cycles != null && this.cycles.equals(other.cycles));
  }
}
