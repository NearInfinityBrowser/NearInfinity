// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.util.Objects;

/**
 * Definition of a single direction in an animation sequence.
 */
public class DirDef implements Cloneable
{
  private final Direction direction;
  private final CycleDef cycle;
  private final boolean mirrored;

  private SeqDef parent;

  /**
   * Creates a new independent direction definition.
   * @param dir the sprite direction
   * @param mirrored whether the BAM frames of the sprite should be horizontally mirrored (fake eastern direction)
   * @param cycle the cycle definition
   */
  public DirDef(Direction dir, boolean mirrored, CycleDef cycle)
  {
    this(null, dir, mirrored, cycle);
  }

  /**
   * Creates a new direction definition linked to the specified {@link SeqDef} instance.
   * @param parent the parent {@code SeqDef} instance
   * @param dir the sprite direction
   * @param mirrored whether the BAM frames of the sprite should be horizontally mirrored (fake eastern direction)
   * @param cycle the cycle definition
   */
  public DirDef(SeqDef parent, Direction dir, boolean mirrored, CycleDef cycle)
  {
    this.parent = parent;
    this.direction = Objects.requireNonNull(dir, "Creature direction cannot be null");
    this.cycle = Objects.requireNonNull(cycle, "Cycle definition cannot be null");
    this.cycle.setParent(this);
    this.mirrored = mirrored;
  }

  /**
   * Creates a new direction definition with the attributes defined in the specified {@code DirDef} argument.
   * Parent attribute is set to {@code null}.
   * @param dd the {@code DirDef} object to clone.
   */
  public DirDef(DirDef dd)
  {
    Objects.requireNonNull(dd, "DirDef instance cannot be null");
    this.parent = null;
    this.direction = dd.direction;
    this.cycle = new CycleDef(dd.cycle);
    this.cycle.setParent(this);
    this.mirrored = dd.mirrored;
  }

  /** Returns the parent {@link SeqDef} instance linked to this object. */
  public SeqDef getParent() { return parent; }

  /** Updates the parent link. Should not be called directly. */
  void setParent(SeqDef parent) { this.parent = parent; }

  /** Returns the direction defined by this instance. */
  public Direction getDirection() { return direction; }

  /** Returns the cycle definition associated with this direction. */
  public CycleDef getCycle() { return cycle; }

  /** Returns whether the BAM frames of this direction should be horizontally mirrored (fake eastern direction). */
  public boolean isMirrored() { return mirrored; }

  /**
   * Advances the animation by one frame for all segment definitions in the associated cycle
   * according to their respective behavior.
   */
  public void advance() { cycle.advance(); }

  /** Resets BAM cycles in all segment definitions in the associated cycle back to the first frame. */
  public void reset() { cycle.reset(); }

  @Override
  public DirDef clone()
  {
    return new DirDef(this);
  }

  @Override
  public String toString()
  {
    return "direction=" + direction.toString() + ", mirrored=" + Boolean.toString(mirrored) + ", cycle={" + cycle.toString() + "}";
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((direction == null) ? 0 : direction.hashCode());
    hash = 31 * hash + ((cycle == null) ? 0 : cycle.hashCode());
    hash = 31 * hash + Boolean.valueOf(mirrored).hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof DirDef)) {
      return false;
    }
    DirDef other = (DirDef)o;
    boolean retVal = (this.direction == null && other.direction == null) ||
                     (this.direction != null && this.direction.equals(other.direction));
    retVal &= (this.cycle == null && other.cycle == null) ||
              (this.cycle != null && this.cycle.equals(other.cycle));
    retVal &= (this.mirrored == other.mirrored);
    return retVal;
  }
}
