// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.*;

import java.io.IOException;
import java.util.ArrayList;
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
    try {
      return EffectFactory.updateOpcode(event.getStructure());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface UpdateListener ---------------------

  public int readAttributes(byte buffer[], int off, List<StructEntry> list)
  {
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
    try {
      off = EffectFactory.getFactory().makeEffectStruct(this, buffer, off, list, getValue(), isV1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    attr_length = off - attr_length;
    return off;
  }
}

