// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.key.ResourceEntry;

/**
 * Definition of an animation sequence.
 */
public class SeqDef implements Cloneable
{
  /** Definition of an empty {@code SeqDef} object. */
  public static final SeqDef DEFAULT = new SeqDef(Sequence.NONE, new DirDef[0]);

  /** Full set of 16 directions (east & west). */
  public static final Direction[] DIR_FULL = Direction.values();
  /** Reduced set of 8 directions (east & west) */
  public static final Direction[] DIR_REDUCED = { Direction.S, Direction.SW, Direction.W, Direction.NW,
                                                  Direction.N, Direction.NE, Direction.E, Direction.SE };
  /** Full set of 9 non-eastern directions. */
  public static final Direction[] DIR_FULL_W = { Direction.S, Direction.SSW, Direction.SW, Direction.WSW,
                                                 Direction.W, Direction.WNW, Direction.NW, Direction.NNW,
                                                 Direction.N };
  /** Full set of 7 eastern directions. */
  public static final Direction[] DIR_FULL_E = { Direction.NNE, Direction.NE, Direction.ENE, Direction.E,
                                                 Direction.ESE, Direction.SE, Direction.SSE };
  /** Reduced set of 5 non-eastern directions. */
  public static final Direction[] DIR_REDUCED_W = { Direction.S, Direction.SW, Direction.W, Direction.NW, Direction.N };
  /** Reduced set of 3 eastern directions. */
  public static final Direction[] DIR_REDUCED_E = { Direction.NE, Direction.E, Direction.SE };

  private final Sequence sequence;
  private final ArrayList<DirDef> directions;

  /**
   * Creates a new sequence definition with the specified directions.
   * @param seq The defined sequence
   * @param dirs One or more direction definitions
   */
  public SeqDef(Sequence seq, DirDef... dirs)
  {
    this.sequence = Objects.requireNonNull(seq, "Sequence cannot be null");
    this.directions = new ArrayList<>();
    addDirections(dirs);
  }

  public SeqDef(SeqDef sd)
  {
    Objects.requireNonNull(sd, "SeqDef instance cannot be null");
    this.sequence = sd.sequence;
    this.directions = new ArrayList<>();
    for (final DirDef dd : sd.directions) {
      addDirections(new DirDef(dd));
    }
  }

  /** Returns the sequence type this definition is assigned to. */
  public Sequence getSequence() { return sequence; }

  /** Provides access to the list of available direction definitions associated with this sequence. */
  public List<DirDef> getDirections() { return directions; }

  /**
   * Appends list of direction definitions.
   * Cycles of existing directions are appended.
   */
  public void addDirections(DirDef... directions)
  {
    for (final DirDef dd : directions) {
      DirDef dir = this.directions.stream().filter(d -> d.getDirection() == dd.getDirection()).findAny().orElse(null);
      if (dir != null) {
        dir.getCycle().addCycles(dd.getCycle().getCycles());
      } else {
        dd.setParent(this);
        this.directions.add(dd);
      }
    }
  }

  /** Returns whether the sequence contains any directions with cycle definitions. */
  public boolean isEmpty()
  {
    boolean retVal = true;
    if (!directions.isEmpty()) {
      for (final DirDef dd : directions) {
        if (!dd.getCycle().getCycles().isEmpty()) {
          retVal = false;
          break;
        }
      }
    }
    return retVal;
  }

  @Override
  public SeqDef clone()
  {
    return new SeqDef(this);
  }

  @Override
  public String toString()
  {
    return "sequence=" + sequence.toString() + ", directions={" + directions.toString() + "}";
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((sequence == null) ? 0 : sequence.hashCode());
    hash = 31 * hash + ((directions == null) ? 0 : directions.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof SeqDef)) {
      return false;
    }
    SeqDef other = (SeqDef)o;
    boolean retVal = (this.sequence == null && other.sequence == null) ||
                     (this.sequence != null && this.sequence.equals(other.sequence));
    retVal &= (this.directions == null && other.directions == null) ||
              (this.directions != null && this.directions.equals(other.directions));
    return retVal;
  }

  /** Convenience method: Returns a list of all BAM resources associated with the specified sequence definitions. */
  public static ResourceEntry[] getBamResourceList(SeqDef... sequences)
  {
    return getBamResourceList(null, sequences);
  }

  /**
   * Convenience method: Returns a list of all BAM resources associated with the specified sequence definitions
   * matching the specified segment type.
   */
  public static ResourceEntry[] getBamResourceList(SegmentDef.SpriteType type, SeqDef... sequences)
  {
    HashSet<ResourceEntry> resources = new HashSet<>();
    for (final SeqDef sd : sequences) {
      for (final DirDef dd : sd.directions) {
        for (int i = 0, cnt = dd.getCycle().getCycles().size(); i < cnt; i++) {
          if (type == null || dd.getCycle().getCycles().get(i).getSpriteType() == type) {
            resources.add(dd.getCycle().getCycles().get(i).getEntry());
          }
        }
      }
    }
    return resources.toArray(new ResourceEntry[resources.size()]);
  }

  /**
   * Convenience method: Creates a fully defined sequence if specified directions are found within a single BAM resource.
   * Behavior is assumed to be {@link SegmentDef.Behavior#REPEAT}. Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param seq the animation {@link SpriteDecoder.Sequence}.
   * @param directions List of directions to add. Cycle indices are advanced accordingly for each direction.
   * @param mirrored indicates whether cycle indices are calculated in reversed direction
   * @param bamResource the BAM resource used for all cycle definitions.
   * @param cycleOfs the first BAM cycle index. Advanced by one for each direction.
   *                 Cycle indices are processed in reversed direction if {@code mirrored} is {@code true}.
   * @param type the {@link SegmentDef.SpriteType} assigned to all cycle definitions.
   */
  public static SeqDef createSequence(Sequence seq, Direction[] directions, boolean mirrored,
                                      ResourceEntry bamResource, int cycleOfs, SegmentDef.SpriteType type)
  {
    return createSequence(seq, directions, mirrored,
                          new ArrayList<SegmentDef>() {{ add(new SegmentDef(null, bamResource, cycleOfs, type)); }});
  }

  /**
   * Convenience method: Creates a fully defined sequence if specified directions are found within a single BAM resource.
   * Composite object is initialized with {@link AlphaComposite#SrcOver}.
   * @param seq the animation {@link SpriteDecoder.Sequence}.
   * @param directions List of directions to add. Cycle indices are advanced accordingly for each direction.
   * @param mirrored indicates whether cycle indices are calculated in reversed direction
   * @param bamResource the BAM resource used for all cycle definitions.
   * @param cycleOfs the first BAM cycle index. Advanced by one for each direction.
   *                 Cycle indices are processed in reversed direction if {@code mirrored} is {@code true}.
   * @param type the {@link SegmentDef.SpriteType} assigned to all cycle definitions.
   * @param behavior the {@link SegmentDef.Behavior} assigned to all cycle definitions.
   */
  public static SeqDef createSequence(Sequence seq, Direction[] directions, boolean mirrored,
                                      ResourceEntry bamResource, int cycleOfs, SegmentDef.SpriteType type,
                                      SegmentDef.Behavior behavior)
  {
    return createSequence(seq, directions, mirrored,
        new ArrayList<SegmentDef>() {{ add(new SegmentDef(null, bamResource, cycleOfs, type, behavior)); }});
  }

  /**
   * Convenience method: Creates a fully defined sequence if specified directions are found within a single BAM resource.
   * @param seq the animation {@link SpriteDecoder.Sequence}.
   * @param directions List of directions to add. Cycle indices are advanced accordingly for each direction.
   * @param mirrored indicates whether cycle indices are calculated in reversed direction
   * @param bamResource the BAM resource used for all cycle definitions.
   * @param cycleOfs the first BAM cycle index. Advanced by one for each direction.
   *                 Cycle indices are processed in reversed direction if {@code mirrored} is {@code true}.
   * @param type the {@link SegmentDef.SpriteType} assigned to all cycle definitions.
   * @param behavior the {@link SegmentDef.Behavior} assigned to all cycle definitions.
   * @param composite the {@link Composite} object used for rendering sprite frames onto the canvas.
   */
  public static SeqDef createSequence(Sequence seq, Direction[] directions, boolean mirrored,
                                      ResourceEntry bamResource, int cycleOfs, SegmentDef.SpriteType type,
                                      SegmentDef.Behavior behavior, Composite composite)
  {
    return createSequence(seq, directions, mirrored,
                          new ArrayList<SegmentDef>() {{ add(new SegmentDef(null, bamResource, cycleOfs, type, behavior, composite)); }});
  }

  /**
   * Convenience method: Creates a fully defined sequence for all directions and their associated segment definitions.
   * @param seq the animation {@link SpriteDecoder.Sequence}.
   * @param directions List of directions to add. Cycle indices are advanced accordingly for each direction.
   * @param mirrored indicates whether cycle indices are calculated in reversed direction
   * @param cycleInfo collection of {@link SegmentDef} instances that are to be associated with the directions.
   */
  public static SeqDef createSequence(Sequence seq, Direction[] directions, boolean mirrored,
                                      Collection<SegmentDef> cycleInfo)
  {
    SeqDef retVal = new SeqDef(seq);

    DirDef[] dirs = new DirDef[Objects.requireNonNull(directions, "Array of creature directions cannot be null").length];
    List<SegmentDef> cycleDefs = new ArrayList<>();
    for (int i = 0; i < directions.length; i++) {
      cycleDefs.clear();
      int inc = mirrored ? (directions.length - i - 1) : i;
      for (Iterator<SegmentDef> iter = cycleInfo.iterator(); iter.hasNext();) {
        SegmentDef sd = iter.next();
        cycleDefs.add(new SegmentDef(null, sd.getEntry(), sd.getCycleIndex() + inc, sd.getSpriteType(), sd.getBehavior()));
      }
      dirs[i] = new DirDef(retVal, directions[i], mirrored, new CycleDef(null, cycleDefs));
    }
    retVal.addDirections(dirs);

    return retVal;
  }
}
