// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.EnumMap;
import java.util.Objects;

import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;

/**
 * A structure that fully describes the location of a single BAM frame.
 */
public class FrameInfo
{
  /** Predefined stroke instance for drawing bounding boxes around creature sprite elements. */
  public static final Stroke STROKE_BOUNDING_BOX = new BasicStroke(1.0f);

  /** Predefined colors for the bounding box around creature sprite elements. */
  public static final EnumMap<SegmentDef.SpriteType, Color> SPRITE_COLOR = new EnumMap<SegmentDef.SpriteType, Color>(SegmentDef.SpriteType.class) {{
    put(SegmentDef.SpriteType.AVATAR, new Color(0x800000ff, true)); // blue
    put(SegmentDef.SpriteType.WEAPON, new Color(0x80ff0000, true)); // red
    put(SegmentDef.SpriteType.SHIELD, new Color(0x8000ff00, true)); // green
    put(SegmentDef.SpriteType.HELMET, new Color(0x8000ffff, true)); // cyan
  }};

  /** Color definition as fallback solution. */
  public static final Color SPRITE_COLOR_DEFAULT = new Color(0x80808080, true); // gray

  private final BamV1Control bamControl;
  private final SegmentDef segmentDef;

  public FrameInfo(BamV1Control bamControl, SegmentDef sd)
  {
    this.bamControl = Objects.requireNonNull(bamControl, "BAM controller cannot be null");
    this.segmentDef = Objects.requireNonNull(sd, "Segment definition cannot be null");
  }

  /** Returns the BAM control instance. */
  public BamV1Control getController() { return bamControl; }

  public SegmentDef getSegmentDefinition() { return segmentDef; }

  /** Returns the absolute cycle index. */
  public int getCycle() { return segmentDef.getCycleIndex(); }

  /** Returns the frame index relative to the cycle. */
  public int getFrame() { return segmentDef.getCurrentFrame(); }

  @Override
  public String toString()
  {
    return "cycle=" + getCycle() + ", frame=" + getFrame();
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((bamControl == null) ? 0 : bamControl.hashCode());
    hash = 31 * hash + ((segmentDef == null) ? 0 : segmentDef.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof FrameInfo)) {
      return false;
    }
    FrameInfo other = (FrameInfo)o;
    boolean retVal = (this.bamControl == null && other.bamControl == null) ||
                     (this.bamControl != null && this.bamControl.equals(other.bamControl));
    retVal &= (this.segmentDef == null && other.segmentDef == null) ||
              (this.segmentDef != null && this.segmentDef.equals(other.segmentDef));
    return retVal;
  }
}
