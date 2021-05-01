// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.util.MapEntry;

import tv.porst.jhexview.IColormap;

/**
 * Defines color schemes for specific resource types to be used in JHexView components.
 */
public class BasicColorMap implements IColormap
{
  /**
   * Use one of the defined color values for background colors of specific structure types.
   * Note: Colors are duplicated to provide coloring support for more than 7 structure types.
   */
  public enum Coloring {
    BLUE, GREEN, RED, CYAN, MAGENTA, YELLOW, LIGHT_GRAY, VIOLET, AQUAMARINE, PEACH, SKY, PINK, LIME,
    BLUE2, GREEN2, RED2, CYAN2, MAGENTA2, YELLOW2, LIGHT_GRAY2, VIOLET2, AQUAMARINE2, PEACH2, SKY2, PINK2, LIME2,
    BLUE3, GREEN3, RED3, CYAN3, MAGENTA3, YELLOW3, LIGHT_GRAY3, VIOLET3, AQUAMARINE3, PEACH3, SKY3, PINK3, LIME3,
  }

  // Color definitions. Each entry consists of two slightly different color tones
  // that will be used alternately.
  private static final EnumMap<Coloring, Color[]> colorMap = new EnumMap<>(Coloring.class);

  static {
    // Populating color map
    colorMap.put(Coloring.BLUE,         new Color[]{new Color(0xd8d8ff), new Color(0xe8e8ff)});
    colorMap.put(Coloring.GREEN,        new Color[]{new Color(0xb8ffb8), new Color(0xd8ffd8)});
    colorMap.put(Coloring.RED,          new Color[]{new Color(0xffd0d0), new Color(0xffe8e8)});
    colorMap.put(Coloring.CYAN,         new Color[]{new Color(0xb8ffff), new Color(0xe0ffff)});
    colorMap.put(Coloring.MAGENTA,      new Color[]{new Color(0xffc8ff), new Color(0xffe0ff)});
    colorMap.put(Coloring.YELLOW,       new Color[]{new Color(0xffffa0), new Color(0xffffe0)});
    colorMap.put(Coloring.LIGHT_GRAY,   new Color[]{new Color(0xe0e0e0), new Color(0xf0f0f0)});
    colorMap.put(Coloring.VIOLET,       new Color[]{new Color(0xdfbfff), new Color(0xf0e1ff)});
    colorMap.put(Coloring.AQUAMARINE,   new Color[]{new Color(0x85ffc2), new Color(0xc2ffe1)});
    colorMap.put(Coloring.PEACH,        new Color[]{new Color(0xffd3a6), new Color(0xffe8d1)});
    colorMap.put(Coloring.SKY,          new Color[]{new Color(0x99ccff), new Color(0xbfe0ff)});
    colorMap.put(Coloring.PINK,         new Color[]{new Color(0xffa3d1), new Color(0xffd1e8)});
    colorMap.put(Coloring.LIME,         new Color[]{new Color(0xb9ff73), new Color(0xdcffb8)});
    colorMap.put(Coloring.BLUE2,        colorMap.get(Coloring.BLUE));
    colorMap.put(Coloring.GREEN2,       colorMap.get(Coloring.GREEN));
    colorMap.put(Coloring.RED2,         colorMap.get(Coloring.RED));
    colorMap.put(Coloring.CYAN2,        colorMap.get(Coloring.CYAN));
    colorMap.put(Coloring.MAGENTA2,     colorMap.get(Coloring.MAGENTA));
    colorMap.put(Coloring.YELLOW2,      colorMap.get(Coloring.YELLOW));
    colorMap.put(Coloring.LIGHT_GRAY2,  colorMap.get(Coloring.LIGHT_GRAY));
    colorMap.put(Coloring.VIOLET2,      colorMap.get(Coloring.VIOLET));
    colorMap.put(Coloring.AQUAMARINE2,  colorMap.get(Coloring.AQUAMARINE));
    colorMap.put(Coloring.PEACH2,       colorMap.get(Coloring.PEACH));
    colorMap.put(Coloring.SKY2,         colorMap.get(Coloring.SKY));
    colorMap.put(Coloring.PINK2,        colorMap.get(Coloring.PINK));
    colorMap.put(Coloring.LIME2,        colorMap.get(Coloring.LIME));
    colorMap.put(Coloring.BLUE3,        colorMap.get(Coloring.BLUE));
    colorMap.put(Coloring.GREEN3,       colorMap.get(Coloring.GREEN));
    colorMap.put(Coloring.RED3,         colorMap.get(Coloring.RED));
    colorMap.put(Coloring.CYAN3,        colorMap.get(Coloring.CYAN));
    colorMap.put(Coloring.MAGENTA3,     colorMap.get(Coloring.MAGENTA));
    colorMap.put(Coloring.YELLOW3,      colorMap.get(Coloring.YELLOW));
    colorMap.put(Coloring.LIGHT_GRAY3,  colorMap.get(Coloring.LIGHT_GRAY));
    colorMap.put(Coloring.VIOLET3,      colorMap.get(Coloring.VIOLET));
    colorMap.put(Coloring.AQUAMARINE3,  colorMap.get(Coloring.AQUAMARINE));
    colorMap.put(Coloring.PEACH3,       colorMap.get(Coloring.PEACH));
    colorMap.put(Coloring.SKY3,         colorMap.get(Coloring.SKY));
    colorMap.put(Coloring.PINK3,        colorMap.get(Coloring.PINK));
    colorMap.put(Coloring.LIME3,        colorMap.get(Coloring.LIME));
  }

  // Contains color definitions for specific data types.
  // Works only on top-level datatypes that are preferably described by a section offset and count.
  private final EnumMap<Coloring, Structure> typeMap = new EnumMap<>(Coloring.class);
  private final MapEntry<Long, Color> cachedColor = new MapEntry<>();
  private final AbstractStruct struct;

  private List<ColoredBlock> listBlocks;

  /**
   * Constructs a new color map and attempts to initialize structures automatically.
   * @param struct The associated resource structure.
   */
  public BasicColorMap(AbstractStruct struct)
  {
    this(struct, true);
  }

  /**
   * Constructs a new color map and optionally attempts to initialize structures automatically.
   * @param struct The associated resource structure.
   * @param autoInit If true, attempts to initialize structures automatically.
   */
  public BasicColorMap(AbstractStruct struct, boolean autoInit)
  {
    if (struct == null) {
      throw new NullPointerException("struct is null");
    }
    this.struct = struct;

    if (autoInit) {
      autoInitColoredEntries();
    }
  }

//--------------------- Begin Interface IColormap ---------------------

  @Override
  public boolean colorize(byte value, long currentOffset)
  {
    return (getCachedColor(currentOffset) != null);
  }

  @Override
  public Color getBackgroundColor(byte value, long currentOffset)
  {
    return getCachedColor(currentOffset);
  }

  @Override
  public Color getForegroundColor(byte value, long currentOffset)
  {
    // Use component's foreground colors
    return null;
  }

//--------------------- End Interface IColormap ---------------------

  /** Cleans up resources. */
  public void close()
  {
    if (listBlocks != null) {
      listBlocks.clear();
      listBlocks = null;
    }
  }

  /** Re-initializes data cache. */
  public void reset()
  {
    close();

    if (listBlocks == null) {
      listBlocks = new ArrayList<>();
    }

    if (!listBlocks.isEmpty()) {
      listBlocks.clear();
    }

    for (final StructEntry curEntry : getStruct().getFlatFields()) {
      List<StructEntry> chain = curEntry.getStructChain();
      boolean found = false;
      for (int i = 0; !found && i < chain.size(); i++) {
        final StructEntry e = chain.get(i);
        Iterator<Map.Entry<Coloring, Structure>> iter = typeMap.entrySet().iterator();
        while (!found && iter.hasNext()) {
          Map.Entry<Coloring, Structure> entry = iter.next();
          Structure s = entry.getValue();
          if (s.getStructureClass().isInstance(e)) {
            int index = s.getStructureIndex(e.getOffset());
            if (index >= 0) {
              ColoredBlock cb = new ColoredBlock(curEntry.getOffset(), curEntry.getSize(),
                                                 entry.getKey(), index);
              listBlocks.add(cb);
              if (curEntry instanceof AbstractCode) {
                AbstractCode ac = (AbstractCode)curEntry;
                cb = new ColoredBlock(ac.getTextOffset(), ac.getTextLength(), entry.getKey(), index);
                listBlocks.add(cb);
              }
              found = true;
            }
          }
        }
        if (found) break;
      }
      chain.clear();
      chain = null;
    }

    combineColoredBlocks();
  }

  /**
   * Attempts to find and add all top-level structures in the associated resource structure.
   * Old entries in the color map will be removed.
   */
  public void autoInitColoredEntries()
  {
    typeMap.clear();
    Coloring[] colors = Coloring.values();
    int colIdx = 0;
    final List<StructEntry> list = getStruct().getFields();
    Collections.sort(list);
    for (final StructEntry entry: list) {
      if (entry instanceof SectionOffset) {
        setColoredEntry(colors[colIdx], ((SectionOffset)entry).getSection());
        colIdx++;
      }
      if (colIdx >= colors.length) {
        // no use overwriting already initialized color entries
        break;
      }
    }
  }

  /**
   * Removes the specified color entry from the map.
   * @param color The color entry to remove.
   */
  public void clearColoredEntry(Coloring color)
  {
    if (color != null) {
      typeMap.remove(color);
    }
  }

  /**
   * Returns the class type associated with the specified color entry.
   * @param color The color entry of the class.
   * @return The class type associated with the specified color entry,
   *         or null if no entry found.
   */
  public Class<? extends StructEntry> getColoredEntry(Coloring color)
  {
    if (color != null) {
      Structure s = typeMap.get(color);
      if (s != null) {
        return s.getStructureClass();
      }
    }
    return null;
  }

  /** Returns the associated resource structure. */
  public AbstractStruct getStruct()
  {
    return struct;
  }

  /**
   * Adds a new color entry to the map. Previous entries using the same color will be overwritten.
   * @param color The coloring value to use.
   * @param classType The class type that should be colorized.
   */
  public void setColoredEntry(Coloring color, Class<? extends StructEntry> classType)
  {
    if (color != null && classType != null) {
      typeMap.put(color, new Structure(getStruct(), classType));
    }
  }

  // Returns the cached color for a specific offset.
  private Color getCachedColor(long offset)
  {
    if (cachedColor.getKey() != Long.valueOf(offset)) {
      cachedColor.setKey(Long.valueOf(offset));
      ColoredBlock cb = findColoredBlock((int)offset);
      if (cb != null) {
        cachedColor.setValue(colorMap.get(cb.getColoring())[cb.getColorIndex()]);
      } else {
        cachedColor.setValue(Color.WHITE);
      }
    }
    return cachedColor.getValue();
  }

  private boolean isCacheInitialized()
  {
    return (listBlocks != null);
  }

  // Minimizes size of block list by combining adjacent blocks into a single block
  private void combineColoredBlocks()
  {
    // XXX: current implementation too time-consuming
//    if (listBlocks != null && !listBlocks.isEmpty()) {
//      Collections.sort(listBlocks);
//      int idx = 0;
//      while (idx < listBlocks.size()) {
//        ColoredBlock cb = listBlocks.get(idx);
//        while (idx+1 < listBlocks.size()) {
//          ColoredBlock cb2 = listBlocks.get(idx+1);
//          if (cb.getColoring() == cb2.getColoring() &&
//              cb.getColorIndex() == cb2.getColorIndex() &&
//              cb2.getOffset() <= cb.getOffset()+cb.getSize()) {
//            int minOfs = Math.min(cb.getOffset(), cb2.getOffset());
//            int maxOfs = Math.max(cb.getOffset()+cb.getSize(), cb2.getOffset()+cb2.getSize());
//            cb.setOffset(minOfs);
//            cb.setSize(maxOfs-minOfs);
//            listBlocks.remove(idx+1);
//          } else {
//            break;
//          }
//        }
//        idx++;
//      }
//    }
  }

  // Attempts to find and return the ColoredBlock object containing the specified offset
  private ColoredBlock findColoredBlock(int offset)
  {
    if (!isCacheInitialized()) {
      reset();
    }

    ColoredBlock cb = ColoredBlock.getSearchBlock(offset);
    int index = Collections.binarySearch(listBlocks, cb, new Comparator<ColoredBlock>() {
      @Override
      public int compare(ColoredBlock obj, ColoredBlock key)
      {
        if (key.getOffset() < obj.getOffset()) {
          return 1;
        } else if (key.getOffset() >= obj.getOffset()+obj.getSize()) {
          return -1;
        } else {
          return 0;
        }
      }
    });
    if (index >= 0 && index < listBlocks.size()) {
      return listBlocks.get(index);
    }
    return null;
  }

//-------------------------- INNER CLASSES --------------------------

  private class Structure
  {
    // only used if isTable = true
    private final List<StructEntry> structures = new ArrayList<>();

    private final Class<? extends StructEntry> classType;

    private boolean isTable;
    private SectionOffset so;
    private SectionCount sc;
    private int structureSize;

    public Structure(AbstractStruct struct, Class<? extends StructEntry> classType)
    {
      if (struct == null || classType == null) {
        throw new NullPointerException();
      }

      this.classType = classType;

      // caches the size of the specified structure for faster index calculation
      structureSize = -1;

      // check if structure is defined by section offset and count fields
      isTable = false;
      so = null; sc = null;
      for (final StructEntry entry: struct.getFields()) {
        if (so == null &&
            entry instanceof SectionOffset &&
            ((SectionOffset)entry).getSection() == classType) {
          so = (SectionOffset)entry;
        }
        if (sc == null &&
            entry instanceof SectionCount &&
            ((SectionCount)entry).getSection() == classType) {
          sc = (SectionCount)entry;
        }
        if (so != null && sc != null) {
          break;
        }
      }

      if (so == null || sc == null) {
        // no section offset and count -> use static table lookup instead
        isTable = true;
        for (final StructEntry entry: struct.getFields()) {
          if (entry.getClass() == classType) {
            structures.add(entry);
          }
        }
        Collections.sort(structures);
      }
    }

    /** Returns the structure type for this object. */
    public Class<? extends StructEntry> getStructureClass()
    {
      return classType;
    }

    /** Returns the index of the structure located at the given offset. Index starts at 0. */
    public int getStructureIndex(int offset)
    {
      if (isTable) {
        for (int i = 0; i < structures.size(); i++) {
          if (offset >= structures.get(i).getOffset() &&
              offset < structures.get(i).getOffset() + structures.get(i).getSize()) {
            return i;
          }
        }
      } else if (sc.getValue() > 0) {
        // structure size not yet cached?
        if (structureSize < 0) {
          for (final StructEntry entry: getStruct().getFields()) {
            if(entry.getClass() == classType) {
              structureSize = entry.getSize();
              break;
            }
          }
        }

        // AbstractCode instances consist of two separate data blocks
        if (AbstractCode.class.isAssignableFrom(classType)) {
          int curIndex = 0;
          for (final StructEntry entry: getStruct().getFields()) {
            if (entry.getClass() == classType) {
              AbstractCode ac = (AbstractCode)entry;
              if ((offset >= ac.getTextOffset() && offset < ac.getTextOffset()+ac.getTextLength())) {
                return curIndex;
              }
              curIndex++;
            }
          }
        }

        // calculating index only on valid structure size
        if (structureSize > 0) {
          if (offset >= so.getValue() && offset < so.getValue() + sc.getValue()*structureSize) {
            int relOfs = offset - so.getValue();
            if (relOfs >= 0) {
              return relOfs / structureSize;
            }
          }
        }
      }

      return -1;
    }
  }


  private static class ColoredBlock implements Comparable<ColoredBlock>, Comparator<ColoredBlock>
  {
    private int offset, size, index;
    private Coloring color;

    /** Returns a dummy block that can be used as key for search operations. */
    public static ColoredBlock getSearchBlock(int offset)
    {
      return new ColoredBlock(offset, 0, null, 0);
    }

    public ColoredBlock(int offset, int size, Coloring color, int index)
    {
      this.offset = offset;
      this.size = size;
      this.color = color;
      this.index = index & 1;
    }

    public int getOffset() { return offset; }
//    public void setOffset(int offset) { this.offset = offset; }

    public int getSize() { return size; }
//    public void setSize(int size) { this.size = size; }

    public Coloring getColoring() { return color; }
//    public void setColoring(Coloring color) { this.color = color; }

    public int getColorIndex() { return index; }
//    public void setColorIndex(int index) { this.index = index & 1; }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 31 * hash + offset;
      hash = 31 * hash + size;
      hash = 31 * hash + index;
      hash = 31 * hash + ((color == null) ? 0 : color.hashCode());
      return hash;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof ColoredBlock) {
        return ((ColoredBlock)o).getOffset() == getOffset();
      } else {
        return false;
      }
    }

    @Override
    public int compare(ColoredBlock o1, ColoredBlock o2)
    {
      return o2.getOffset() - o1.getOffset();
    }

    @Override
    public int compareTo(ColoredBlock o)
    {
      return (getOffset() - o.getOffset());
    }
  }
}
