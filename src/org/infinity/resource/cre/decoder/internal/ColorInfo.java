// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides information about color definitions for sprites and sprite overlays.
 */
public class ColorInfo
{
  private static final int[] EMPTY_INT_ARRAY = new int[0];

  // Maps value (Map<color index, color entry>) to an individual sprite overlay types (avatar, weapon, shield, ...)
  private final EnumMap<SegmentDef.SpriteType, Map<Integer, Integer>> colorMap = new EnumMap<>(SegmentDef.SpriteType.class);

  public ColorInfo()
  {
  }

  /** Returns an iterator over the sprite overlay types for color definitions. */
  public Iterator<SegmentDef.SpriteType> getTypeIterator() { return colorMap.keySet().iterator(); }

  /** Returns an array of sprite overlay types for color definitions. */
  public SegmentDef.SpriteType[] getTypes() { return colorMap.keySet().toArray(new SegmentDef.SpriteType[colorMap.size()]); }

  /** Returns an iterator over the color locations for the specified sprite overlay type. */
  public Iterator<Integer> getLocationIterator(SegmentDef.SpriteType type)
  {
    Map<Integer, Integer> map = colorMap.get(type);
    if (map != null) {
      return map.keySet().iterator();
    }
    return Collections.<Integer>emptyList().iterator(); // empty iterator
  }

  /** Returns an array of color location indices for the specified sprite overlay type. */
  public int[] getLocations(SegmentDef.SpriteType type)
  {
    int[] retVal = EMPTY_INT_ARRAY;
    Map<Integer, Integer> map = colorMap.get(type);
    if (map != null) {
      retVal = new int[map.size()];
      int i = 0;
      for (final Integer v : map.keySet()) {
        retVal[i] = v.intValue();
        i++;
      }
    }
    return retVal;
  }

  /**
   * Returns the color value for the specified sprite overlay type and location index.
   * Returns -1 if value is not available.
   */
  public int getValue(SegmentDef.SpriteType type, int index)
  {
    Map<Integer, Integer> map = colorMap.get(type);
    if (map != null) {
      Integer v = map.get(index);
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
   * @param locationIndex the color location.
   * @param colorIndex the color index.
   */
  public void add(SegmentDef.SpriteType type, int locationIndex, int colorIndex)
  {
    if (type == null) {
      return;
    }
    if (locationIndex >= -1 && locationIndex < 7) {
      Map<Integer, Integer> map = colorMap.get(type);
      if (map == null) {
        map = new HashMap<Integer, Integer>();
      }
      map.put(locationIndex, colorIndex);
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
