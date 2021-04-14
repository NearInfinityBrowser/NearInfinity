// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.infinity.util.tuples.Couple;

/**
 * Provides information about color definitions for sprites and sprite overlays.
 */
public class ColorInfo
{
  /** Effect opcode 7: Set color */
  public static final int OPCODE_SET_COLOR      = 7;
  /** Effect opcode 8: Set color glow solid */
  public static final int OPCODE_SET_COLOR_GLOW = 8;
  /** Effect opcode 51: Character tint solid */
  public static final int OPCODE_TINT_SOLID     = 51;
  /** Effect opcode 52: Character tint bright */
  public static final int OPCODE_TINT_BRIGHT    = 52;
  /** Effect opcode 64: Blur */
  public static final int OPCODE_BLUR           = 65;
  /** Effect opcode 64: Translucency */
  public static final int OPCODE_TRANSLUCENCY   = 66;
  /** Effect opcode 134: Petrification */
  public static final int OPCODE_PETRIFICATION  = 134;
  /** Effect opcode 218: Stoneskin */
  public static final int OPCODE_STONESKIN      = 218;

  private static final int[] EMPTY_INT_ARRAY = new int[0];

  // Maps value (Map<Couple<opcode, color location>, color value>) to an individual sprite overlay types (avatar, weapon, shield, ...)
  private final EnumMap<SegmentDef.SpriteType, Map<Couple<Integer, Integer>, Integer>> colorMap = new EnumMap<>(SegmentDef.SpriteType.class);

  public ColorInfo()
  {
  }

  /** Returns an iterator over the sprite overlay types for color definitions. */
  public Iterator<SegmentDef.SpriteType> getTypeIterator() { return colorMap.keySet().iterator(); }

  /** Returns an array of sprite overlay types for color definitions. */
  public SegmentDef.SpriteType[] getTypes() { return colorMap.keySet().toArray(new SegmentDef.SpriteType[colorMap.size()]); }

  /** Returns an iterator over the color locations for the specified sprite overlay type. */
  public Iterator<Couple<Integer, Integer>> getEffectIterator(SegmentDef.SpriteType type)
  {
    Map<Couple<Integer, Integer>, Integer> map = colorMap.get(type);
    if (map != null) {
      return map.keySet().iterator();
    }
    return Collections.<Couple<Integer, Integer>>emptyList().iterator(); // empty iterator
  }

  /** Returns an array of color location indices for the specified sprite overlay type. */
  public int[] getLocations(SegmentDef.SpriteType type, int opcode)
  {
    int[] retVal = EMPTY_INT_ARRAY;
    Map<Couple<Integer, Integer>, Integer> map = colorMap.get(type);
    if (map != null) {
      retVal = new int[map.size()];
      int i = 0;
      for (final Couple<Integer, Integer> pair : map.keySet()) {
        if (pair.getValue0().intValue() == opcode) {
          retVal[i] = pair.getValue1().intValue();
          i++;
        }
      }
      if (i < retVal.length) {
        retVal = Arrays.copyOf(retVal, i);
      }
    }
    return retVal;
  }

  /**
   * Returns the color value for the specified sprite overlay type and location index.
   * Returns -1 if value is not available.
   */
  public int getValue(SegmentDef.SpriteType type, int opcode, int index)
  {
    Map<Couple<Integer, Integer>, Integer> map = colorMap.get(type);
    if (map != null) {
      Integer v = map.get(Couple.with(opcode, index));
      if (v != null) {
        return v.intValue();
      }
    }
    return -1;
  }

  /**
   * Adds a color entry and associates it with a sprite overlay type and color location.
   * Existing color entries will be updated.
   * @param type the sprite overlay type.
   * @param opcode the effect opcode.
   * @param location the color location.
   * @param value the unprocessed color value.
   */
  public void add(SegmentDef.SpriteType type, int opcode, int location, int value)
  {
    if (type == null) {
      return;
    }
    if (location >= -1 && location < 7) {
      Map<Couple<Integer, Integer>, Integer> map = colorMap.get(type);
      if (map == null) {
        map = new HashMap<>();
      }

      // swapping byte order of color value
      switch (opcode) {
        case OPCODE_SET_COLOR_GLOW:
        case OPCODE_TINT_SOLID:
        case OPCODE_TINT_BRIGHT:
        {
          int tmp = 0;
          for (int i = 0; i < 4; i++) {
            tmp <<= 8;
            tmp |= value & 0xff;
            value >>= 8;
          }
          value = tmp;
          break;
        }
      }

      map.put(Couple.with(opcode, location), value);
      colorMap.put(type, map);
    }
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((colorMap == null) ? 0 : colorMap.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ColorInfo)) {
      return false;
    }
    ColorInfo other = (ColorInfo)o;
    return (this.colorMap == null && other.colorMap == null) ||
           (this.colorMap != null && this.colorMap.equals(other.colorMap));
  }
}
