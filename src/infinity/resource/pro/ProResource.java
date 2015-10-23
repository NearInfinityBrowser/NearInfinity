// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import java.util.List;

import javax.swing.JComponent;

import infinity.datatype.Bitmap;
import infinity.datatype.ColorPicker;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.FlagEx;
import infinity.datatype.HashBitmapEx;
import infinity.datatype.IDSTargetEffect;
import infinity.datatype.ResourceRef;
import infinity.datatype.SpellProtBitmap;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UpdateEvent;
import infinity.datatype.UpdateListener;
import infinity.gui.StructViewer;
import infinity.gui.hexview.BasicColorMap;
import infinity.gui.hexview.HexViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.EffectFactory;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;
import infinity.util.LongIntegerHashMap;

public final class ProResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs, UpdateListener
{
  public static final String[] s_color = {"", "Black", "Blue", "Chromatic", "Gold",
                                           "Green", "Purple", "Red", "White", "Ice",
                                           "Stone", "Magenta", "Orange"};
  public static final String[] s_behave = {"No flags set", "Show sparks", "Use height",
                                            "Loop fire sound", "Loop impact sound", "Ignore center",
                                            "Draw as background"};
  public static final String[] s_flagsEx = {
    "No flags set", "Bounce from walls", "Pass target", "Draw center VVC once", "Hit immediately",
    "Face target", "Curved path", "Start random frame", "Pillar", "Semi-trans. trail puff VEF",
    "Tinted trail puff VEF", "Multiple proj.", "Default spell on missed", "Falling path", "Comet",
    "Lined up AoE", "Rectangular AoE", "Draw behind target", "Casting glow fx", "Travel door",
    "Stop/fade after hit", "Display string", "Random path", "Start random seq.", "Color pulse on hit",
    "Touch projectile", "Neg. IDS1", "Neg. IDS2", "Use either IDS", "Delayed payload",
    "Limited path count", "IWD style check", "Caster affected"};

  public static final LongIntegerHashMap<String> m_projtype = new LongIntegerHashMap<String>();
  static {
    m_projtype.put(1L, "No BAM");
    m_projtype.put(2L, "Single target");
    m_projtype.put(3L, "Area of effect");
  }

  private HexViewer hexViewer;

  public ProResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

//--------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return null;
  }

//--------------------- End Interface HasAddRemovable ---------------------

//--------------------- Begin Interface UpdateListener ---------------------

  @Override
  public boolean valueUpdated(UpdateEvent event)
  {
    if (event.getSource() instanceof FlagEx &&
        ((StructEntry)event.getSource()).getName().equals("Extended flags")) {
      boolean isIwdStyle = ((FlagEx)event.getSource()).isFlagSet(30);
      AbstractStruct struct = event.getStructure();
      boolean bRet = false;
      if (isIwdStyle) {
        bRet |= setIwdStyleIdsType(struct, 60, 1);
        bRet |= setIwdStyleIdsType(struct, 64, 2);
      } else {
        bRet |= setOldStyleIdsType(struct, 60, 1);
        bRet |= setOldStyleIdsType(struct, 64, 2);
      }
      return bRet;
    }
    else if (event.getSource() instanceof HashBitmapEx &&
             ((StructEntry)event.getSource()).getName().equals("Projectile type")) {
      HashBitmapEx proType = (HashBitmapEx)event.getSource();
      AbstractStruct struct = event.getStructure();
      // add/remove extended sections in the parent structure depending on the current value
      if (struct instanceof Resource && struct instanceof HasAddRemovable) {
        if (proType.getValue() == 3L) {         // area of effect
          StructEntry entry = struct.getList().get(struct.getList().size() - 1);
          try {
            if (!(entry instanceof ProSingleType) && !(entry instanceof ProAreaType))
              struct.addDatatype(new ProSingleType(), struct.getList().size());
            entry = struct.getList().get(struct.getList().size() - 1);
            if (!(entry instanceof ProAreaType))
              struct.addDatatype(new ProAreaType(), struct.getList().size());
          } catch (Exception e) {
            e.printStackTrace();
            return false;
          }
        } else if (proType.getValue() == 2L) {  // single target
          StructEntry entry = struct.getList().get(struct.getList().size() - 1);
          if (entry instanceof ProAreaType)
            struct.removeDatatype((AddRemovable)entry, false);
          entry = struct.getList().get(struct.getList().size() - 1);
          if (!(entry instanceof ProSingleType)) {
            try {
              struct.addDatatype(new ProSingleType(), struct.getList().size());
            } catch (Exception e) {
              e.printStackTrace();
              return false;
            }
          }
        } else if (proType.getValue() == 1L) {  // no bam
          if (struct.getList().size() > 2) {
            StructEntry entry = struct.getList().get(struct.getList().size() - 1);
            if (entry instanceof ProAreaType)
              struct.removeDatatype((AddRemovable)entry, false);
            entry = struct.getList().get(struct.getList().size() - 1);
            if (entry instanceof ProSingleType)
              struct.removeDatatype((AddRemovable)entry, false);
          }
        } else {
          return false;
        }
        return true;
      }
    }
    return false;
  }

//--------------------- End Interface UpdateListener ---------------------

//--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      BasicColorMap colorMap = new BasicColorMap(this, false);
      colorMap.setColoredEntry(BasicColorMap.Coloring.BLUE, ProSingleType.class);
      colorMap.setColoredEntry(BasicColorMap.Coloring.GREEN, ProAreaType.class);
      hexViewer = new HexViewer(this, colorMap);
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    final String[] s_types = Profile.isEnhancedEdition() ? new String[]{"VVC", "BAM"}
                                                         : new String[]{"VEF", "VVC", "BAM"};

    addField(new TextString(buffer, offset, 4, "Signature"));
    addField(new TextString(buffer, offset + 4, 4, "Version"));
    HashBitmapEx projtype = new HashBitmapEx(buffer, offset + 8, 2, "Projectile type", m_projtype);
    projtype.addUpdateListener(this);
    addField(projtype);
    addField(new DecNumber(buffer, offset + 10, 2, "Speed"));
    addField(new Flag(buffer, offset + 12, 4, "Behavior", s_behave));
    addField(new ResourceRef(buffer, offset + 16, "Fire sound", "WAV"));
    addField(new ResourceRef(buffer, offset + 24, "Impact sound", "WAV"));
    addField(new ResourceRef(buffer, offset + 32, "Source animation", s_types));
    addField(new Bitmap(buffer, offset + 40, 2, "Particle color", s_color));
    if (Profile.isEnhancedEdition()) {
      addField(new DecNumber(buffer, offset + 42, 2, "Projectile width"));
      FlagEx flagEx = new FlagEx(buffer, offset + 44, 4, "Extended flags", s_flagsEx);
      addField(flagEx);
      addField(new StringRef(buffer, offset + 48, "String"));
      addField(new ColorPicker(buffer, offset + 52, "Color", ColorPicker.Format.BGRX));
      addField(new DecNumber(buffer, offset + 56, 2, "Color speed"));
      addField(new DecNumber(buffer, offset + 58, 2, "Screen shake amount"));
      if (ResourceFactory.resourceExists(SpellProtBitmap.getTableName())) {
        flagEx.addUpdateListener(this);
        if (flagEx.isFlagSet(30)) {
          addField(new DecNumber(buffer, offset + 60, 2, "Creature value 1"));
          addField(new SpellProtBitmap(buffer, offset + 62, 2, "Creature type 1"));
          addField(new DecNumber(buffer, offset + 64, 2, "Creature value 2"));
          addField(new SpellProtBitmap(buffer, offset + 66, 2, "Creature type 2"));
        } else {
          addField(new IDSTargetEffect(buffer, offset + 60, 4, "IDS target 1"));
          addField(new IDSTargetEffect(buffer, offset + 64, 4, "IDS target 2"));
        }
      } else {
        addField(new IDSTargetEffect(buffer, offset + 60, 4, "IDS target 1"));
        addField(new IDSTargetEffect(buffer, offset + 64, 4, "IDS target 2"));
      }
      addField(new ResourceRef(buffer, 68, "Default spell", "SPL"));
      addField(new ResourceRef(buffer, 76, "Success spell", "SPL"));
      addField(new Unknown(buffer, offset + 84, 172));
    } else {
      addField(new Unknown(buffer, offset + 42, 214));
    }
    offset += 256;

    if (projtype.getValue() > 1L) {
      ProSingleType single = new ProSingleType(this, buffer, offset);
      addField(single);
      offset += single.getSize();
    }
    if (projtype.getValue() > 2L) {
      ProAreaType area = new ProAreaType(this, buffer, offset);
      addField(area);
      offset += area.getSize();
    }

    return offset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  // Updates current IDS targeting to IWD style and returns true if changes have been made
  private boolean setIwdStyleIdsType(AbstractStruct struct, int offset, int nr)
  {
    if (struct != null && offset >= 0) {
      StructEntry e = struct.getAttribute(offset, false);
      if (e instanceof IDSTargetEffect) {
        e = removeEntry(struct, offset);
        if (e != null) {
          byte[] data = EffectFactory.getEntryData(e);
          e = new DecNumber(data, 0, 2, "Creature value " + nr);
          e.setOffset(offset);
          addEntry(struct, offset, e);
          e = new SpellProtBitmap(data, 2, 2, "Creature type " + nr);
          e.setOffset(offset + 2);
          addEntry(struct, offset + 2, e);
        }
        return true;
      }
    }
    return false;
  }

  // Updates current IDS targeting to old BG style and returns true if changes have been made
  private boolean setOldStyleIdsType(AbstractStruct struct, int offset, int nr)
  {
    if (struct != null && offset >= 0) {
      StructEntry e = struct.getAttribute(offset, false);
      if (!(e instanceof IDSTargetEffect)) {
        byte[] data = new byte[4];
        e = removeEntry(struct, offset);
        if (e != null) {
          System.arraycopy(EffectFactory.getEntryData(e), 0, data, 0, 2);
        }
        e = removeEntry(struct, offset + 2);
        if (e != null) {
          System.arraycopy(EffectFactory.getEntryData(e), 0, data, 2, 2);
        }
        e = new IDSTargetEffect(data, 0, 4, "IDS target " + nr);
        e.setOffset(offset);
        addEntry(struct, offset, e);
        return true;
      }
    }
    return false;
  }

  // Removes the StructEntry object at the specified offset and returns it.
  private StructEntry removeEntry(AbstractStruct struct, int offset)
  {
    if (struct != null && offset >= 0) {
      List<StructEntry> list = struct.getList();
      if (list != null) {
        for (int i = 0, size = list.size(); i < size; i++) {
          StructEntry e = list.get(i);
          if (offset >= e.getOffset() && offset < (e.getOffset() + e.getSize())) {
            return list.remove(i);
          }
        }
      }
    }
    return null;
  }

  // Adds the specified StructEntry object to the current resource structure
  private void addEntry(AbstractStruct struct, int offset, StructEntry entry)
  {
    if (struct != null && offset >= 0 && entry != null) {
      List<StructEntry> list = getList();
      for (int i = 0, size = list.size(); i < size; i++) {
        if (list.get(i).getOffset() > offset) {
          list.add(i, entry);
          return;
        }
      }
    }
  }

  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        ProResource pro = new ProResource(entry);
        ProSingleType single = (ProSingleType)pro.getAttribute(SearchOptions.getResourceName(SearchOptions.PRO_SingleTarget), false);
        ProAreaType area = (ProAreaType)pro.getAttribute(SearchOptions.getResourceName(SearchOptions.PRO_AreaOfEffect), false);
        boolean retVal = true;
        String key;
        Object o;

        String[] keyList = new String[]{SearchOptions.PRO_Type, SearchOptions.PRO_Speed,
                                        SearchOptions.PRO_TrapSize, SearchOptions.PRO_ExplosionSize,
                                        SearchOptions.PRO_ExplosionEffect};
        AbstractStruct[] structList = new AbstractStruct[]{pro, pro, area, area, area};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            if (structList[idx] != null) {
              StructEntry struct = structList[idx].getAttribute(SearchOptions.getResourceName(key), false);
              retVal &= SearchOptions.Utils.matchNumber(struct, o);
            } else {
              retVal &= (o == null);
            }
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.PRO_Behavior, SearchOptions.PRO_Flags,
                               SearchOptions.PRO_AreaFlags};
        structList = new AbstractStruct[]{pro, single, area};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            if (structList[idx] != null) {
              StructEntry struct = structList[idx].getAttribute(SearchOptions.getResourceName(key), false);
              retVal &= SearchOptions.Utils.matchFlags(struct, o);
            } else {
              retVal &= (o == null);
            }
          } else {
            break;
          }
        }

        if (retVal) {
          key = SearchOptions.PRO_Animation;
          o = searchOptions.getOption(key);
          if (single != null) {
            StructEntry struct = single.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchResourceRef(struct, o, false);
          } else {
            retVal &= (o == null);
          }
        }

        keyList = new String[]{SearchOptions.PRO_Custom1, SearchOptions.PRO_Custom2,
                               SearchOptions.PRO_Custom3, SearchOptions.PRO_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(pro, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}
