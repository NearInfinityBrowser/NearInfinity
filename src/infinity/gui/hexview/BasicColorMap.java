// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.hexview;

import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;
import infinity.resource.dlg.AbstractCode;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import tv.porst.jhexview.IColormap;

/**
 * Defines color schemes for specific resource types to be used in JHexView components.
 *
 * TODO: Add support for 3rd-level fields and higher
 *
 * @author argent77
 */
public class BasicColorMap implements IColormap
{
  /**
   * Use one of the defined color values for background colors of specific structure types.
   * Note: Colors are duplicated to provide coloring support for more than 7 structure types.
   */
  public enum Coloring {
    BLUE, GREEN, RED, CYAN, MAGENTA, YELLOW, LIGHT_GRAY,
    BLUE2, GREEN2, RED2, CYAN2, MAGENTA2, YELLOW2, LIGHT_GRAY2,
    BLUE3, GREEN3, RED3, CYAN3, MAGENTA3, YELLOW3, LIGHT_GRAY3,
  }

  // Color definitions. Each entry consists of two slightly different color tones
  // that will be used alternately.
  private static final EnumMap<Coloring, Color[]> colorMap = new EnumMap<Coloring, Color[]>(Coloring.class);

  static {
    // Populating color map
    colorMap.put(Coloring.BLUE,       new Color[]{new Color(0xd8d8ff), new Color(0xe8e8ff)});
    colorMap.put(Coloring.GREEN,      new Color[]{new Color(0xb8ffb8), new Color(0xd8ffd8)});
    colorMap.put(Coloring.RED,        new Color[]{new Color(0xffd0d0), new Color(0xffe8e8)});
    colorMap.put(Coloring.CYAN,       new Color[]{new Color(0xb8ffff), new Color(0xe0ffff)});
    colorMap.put(Coloring.MAGENTA,    new Color[]{new Color(0xffc8ff), new Color(0xffe0ff)});
    colorMap.put(Coloring.YELLOW,     new Color[]{new Color(0xffffa0), new Color(0xffffe0)});
    colorMap.put(Coloring.LIGHT_GRAY, new Color[]{new Color(0xe0e0e0), new Color(0xf0f0f0)});
    colorMap.put(Coloring.BLUE2,        colorMap.get(Coloring.BLUE));
    colorMap.put(Coloring.GREEN2,       colorMap.get(Coloring.GREEN));
    colorMap.put(Coloring.RED2,         colorMap.get(Coloring.RED));
    colorMap.put(Coloring.CYAN2,        colorMap.get(Coloring.CYAN));
    colorMap.put(Coloring.MAGENTA2,     colorMap.get(Coloring.MAGENTA));
    colorMap.put(Coloring.YELLOW2,      colorMap.get(Coloring.YELLOW));
    colorMap.put(Coloring.LIGHT_GRAY2,  colorMap.get(Coloring.LIGHT_GRAY));
    colorMap.put(Coloring.BLUE3,        colorMap.get(Coloring.BLUE));
    colorMap.put(Coloring.GREEN3,       colorMap.get(Coloring.GREEN));
    colorMap.put(Coloring.RED3,         colorMap.get(Coloring.RED));
    colorMap.put(Coloring.CYAN3,        colorMap.get(Coloring.CYAN));
    colorMap.put(Coloring.MAGENTA3,     colorMap.get(Coloring.MAGENTA));
    colorMap.put(Coloring.YELLOW3,      colorMap.get(Coloring.YELLOW));
    colorMap.put(Coloring.LIGHT_GRAY3,  colorMap.get(Coloring.LIGHT_GRAY));
  }

  // Contains color definitions for specific data types.
  // Works only on top-level datatypes that are preferably described by a section offset and count.
  private final EnumMap<Coloring, Structure> typeMap = new EnumMap<Coloring, Structure>(Coloring.class);

  private final AbstractStruct struct;

  private Color cachedColor = null;
  private List<StructEntry> listStructures;

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
    reset();

    if (autoInit) {
      autoInitColoredEntries();
    }
  }

//--------------------- Begin Interface IColormap ---------------------

  @Override
  public boolean colorize(byte value, long currentOffset)
  {
    cachedColor = getStructureColor((int)currentOffset);
    return (cachedColor != null);
  }

  @Override
  public Color getBackgroundColor(byte value, long currentOffset)
  {
    if (cachedColor == null) {
      cachedColor = getStructureColor((int)currentOffset);
    }
    return cachedColor;
  }

  @Override
  public Color getForegroundColor(byte value, long currentOffset)
  {
    // Use component's foreground colors
    return null;
  }

//--------------------- End Interface IColormap ---------------------

  /** Re-initializes data cache. */
  public void reset()
  {
    if (listStructures != null) {
      listStructures.clear();
      listStructures = null;
    }
    listStructures = getStruct().getList();
    Collections.sort(listStructures);
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
    for (final StructEntry entry: getCachedList()) {
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

  // Returns the Color defined for the value at the specified offset or null otherwise.
  private Color getStructureColor(int offset)
  {
    if (!typeMap.isEmpty()) {
      Iterator<Coloring> iter = typeMap.keySet().iterator();
      while (iter.hasNext()) {
        Coloring c = iter.next();
        Structure s = typeMap.get(c);
        if (s != null) {
          int index = s.getStructureIndex(offset);
          if (index >= 0) {
            return colorMap.get(c)[index & 1];
          }
        }
      }
    }

    return Color.WHITE;
  }

  // Returns the list of cached top-level StructEntry objects
  private List<StructEntry> getCachedList()
  {
    if (listStructures == null) {
      reset();
    }
    return listStructures;
  }


//-------------------------- INNER CLASSES --------------------------

  private class Structure
  {
    // only used if isTable = true
    private final List<StructEntry> structures = new ArrayList<StructEntry>();

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
      for (final StructEntry entry: struct.getList()) {
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
        for (final StructEntry entry: struct.getList()) {
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
      } else {
        // structure size not yet cached?
        if (structureSize < 0) {
          for (final StructEntry entry: getStruct().getList()) {
            if(entry.getClass() == classType) {
              structureSize = entry.getSize();
              break;
            }
          }
        }

        // AbstractCode instances consist of two separate data blocks
        if (AbstractCode.class.isAssignableFrom(classType)) {
          int curIndex = 0;
          for (final StructEntry entry: getStruct().getList()) {
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
}
