// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.*;
import infinity.resource.EffectFactory.EffectEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class EffectType extends Bitmap implements UpdateListener
{
  public static final String s_dispel[] = {"No dispel/bypass resistance", "Dispel/Not bypass resistance",
                                           "Not dispel/bypass resistance", "Dispel/Bypass resistance"};
  private static final String s_target[] = {"None", "Self", "Preset target",
                                            "Party", "Everyone", "Everyone except party",
                                            "Caster group", "Target group", "Everyone except self", "Original caster"};
  private int attr_length;

  public EffectType(byte buffer[], int offset, int length)
  {
    super(buffer, offset, length, "Type", EffectFactory.getFactory().getEffectNameArray());
  }

// --------------------- Begin Interface Editable ---------------------

  public boolean updateValue(AbstractStruct struct)
  {
    super.updateValue(struct);
    try {
      List<StructEntry> list = new ArrayList<StructEntry>();
      readAttributes(struct.removeFromList(this, attr_length), 0, list);
      for (int i = 0; i < list.size(); i++) {
        StructEntry entry = list.get(i);
        entry.setOffset(entry.getOffset() + getOffset() + getSize());
      }
      struct.addToList(this, list);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface Editable ---------------------

// --------------------- Begin Interface UpdateListener ---------------------

  public boolean valueUpdated(UpdateEvent event)
  {
    boolean result = false;
    try {
      AbstractStruct struct = event.getStructure();
      EffectFactory factory = EffectFactory.getFactory();
      EnumMap<EffectEntry, Integer> map = factory.getEffectStructure(struct);
      if (map.containsKey(EffectEntry.IDX_OPCODE)) {
        int opcode = ((EffectType)factory.getEntry(struct, map.get(EffectEntry.IDX_OPCODE))).getValue();

        if (opcode == 319) {    // effect type "Item Usability" (319/0x13F)
          long param2 = ((HashBitmapEx)event.getSource()).getValue();
          if (param2 == 10L) {
            // Param1 = Actor's name as Strref
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_PARAM1), map.get(EffectEntry.OFS_PARAM1),
                                 new StringRef(factory.getEntryData(struct, map.get(EffectEntry.IDX_PARAM1)),
                                               0, "Actor name"));
          } else if (param2 >= 2 && param2 < 10) {
            // Param1 = IDS entry
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_PARAM1), map.get(EffectEntry.OFS_PARAM1),
                                 new IdsBitmap(factory.getEntryData(struct, map.get(EffectEntry.IDX_PARAM1)),
                                               0, 4, "IDS entry", EffectFactory.m_itemids.get(param2)));
          } else {
            // Param1 = Unused
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_PARAM1), map.get(EffectEntry.OFS_PARAM1),
                                 new DecNumber(factory.getEntryData(struct, map.get(EffectEntry.IDX_PARAM1)),
                                               0, 4, "Unused"));
          }

          if (param2 == 11L) {
            // Resource = Actor's script name
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_RESOURCE), map.get(EffectEntry.OFS_RESOURCE),
                                 new TextString(factory.getEntryData(struct, map.get(EffectEntry.IDX_RESOURCE)),
                                                0, 8, "Script name"));
          } else {
            // Resource = Unused
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_RESOURCE), map.get(EffectEntry.OFS_RESOURCE),
                                 new Unknown(factory.getEntryData(struct, map.get(EffectEntry.IDX_RESOURCE)),
                                             0, 8, "Unused"));
          }
          result = true;
        } else if (opcode == 232) {   // effect type "Cast spell on condition" (232/0xE8)
          int param2 = ((BitmapEx)event.getSource()).getValue();
          if (param2 == 13) {
            // Special = Time of day
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_SPECIAL), map.get(EffectEntry.OFS_SPECIAL),
                                 new IdsBitmap(factory.getEntryData(struct, map.get(EffectEntry.IDX_SPECIAL)),
                                               0, 4, "Time of day", "TIMEODAY.IDS"));
          } else {
            // Special = Unused
            factory.replaceEntry(struct, map.get(EffectEntry.IDX_SPECIAL), map.get(EffectEntry.OFS_SPECIAL),
                                 new DecNumber(factory.getEntryData(struct, map.get(EffectEntry.IDX_SPECIAL)),
                                               0, 4, "Unused"));
          }
          result = true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

// --------------------- End Interface UpdateListener ---------------------

  public int readAttributes(byte buffer[], int off, List<StructEntry> list)
  {
//    String restype;
    attr_length = off;
    boolean isV1 = (getSize() == 2);
    if (isV1) {
      // EFF V1.0
      list.add(new Bitmap(buffer, off, 1, "Target", s_target));
      list.add(new DecNumber(buffer, off + 1, 1, "Power"));
      off += 2;
    }
    else {
      // EFF V2.0
      list.add(new Bitmap(buffer, off, 4, "Target", s_target));
      list.add(new DecNumber(buffer, off + 4, 4, "Power"));
      off += 8;
    }
    off = EffectFactory.getFactory().makeEffectStruct(this, buffer, off, list, getValue(), isV1);
    attr_length = off - attr_length;
    return off;
  }
}

