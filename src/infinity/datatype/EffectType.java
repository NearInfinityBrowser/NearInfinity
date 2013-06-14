// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class EffectType extends Bitmap
{
  public static final String s_dispel[] = {"No dispel/bypass resistance", "Dispel/Not bypass resistance",
                                           "Not dispel/bypass resistance", "Dispel/Bypass resistance"};
  private static final String s_target[] = {"None", "Self", "Preset target",
                                            "Party", "Everyone", "Everyone except party",
                                            "Caster group", "Target group", "Everyone except self", "Original caster"};
  private static final String s_duration[] = {"Instant/Limited", "Instant/Permanent until death",
                                              "Instant/While equipped", "Delay/Limited", "Delay/Permanent",
                                              "Delay/While equipped", "Limited after duration",
                                              "Permanent after duration", "Equipped after duration",
                                              "Instant/Permanent", "Instant/Limited (ticks)"};
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

  public int readAttributes(byte buffer[], int off, List<StructEntry> list)
  {
    String restype;
    attr_length = off;
    if (getSize() == 2) {
      // EFF V1.0
      list.add(new Bitmap(buffer, off, 1, "Target", s_target));
      list.add(new DecNumber(buffer, off + 1, 1, "Power"));
      restype = EffectFactory.getFactory().makeEffectStruct(buffer, off + 2, list, getValue());
      list.add(new Bitmap(buffer, off + 10, 1, "Timing mode", s_duration));
      list.add(new Bitmap(buffer, off + 11, 1, "Dispel/Resistance", s_dispel));
      list.add(new DecNumber(buffer, off + 12, 4, "Duration"));
      list.add(new DecNumber(buffer, off + 16, 1, "Probability 1"));
      list.add(new DecNumber(buffer, off + 17, 1, "Probability 2"));
      off += 18;
    }
    else {
      // EFF V2.0
      list.add(new Bitmap(buffer, off, 4, "Target", s_target));
      list.add(new DecNumber(buffer, off + 4, 4, "Power"));
      restype = EffectFactory.getFactory().makeEffectStruct(buffer, off + 8, list, getValue());
      list.add(new Bitmap(buffer, off + 16, 4, "Timing mode", s_duration));
      list.add(new DecNumber(buffer, off + 20, 4, "Duration"));
      list.add(new DecNumber(buffer, off + 24, 2, "Probability 1"));
      list.add(new DecNumber(buffer, off + 26, 2, "Probability 2"));
      off += 28;
    }
    if (restype == null)
      list.add(new Unknown(buffer, off, 8));
    else if (restype.equalsIgnoreCase("String"))
      list.add(new TextString(buffer, off, 8, "String"));
    else
      list.add(new ResourceRef(buffer, off, "Resource", restype.split(":")));
    off += 8;
    attr_length = off - attr_length;
    return off;
  }
}

