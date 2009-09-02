// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.datatype.*;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractAbility extends AbstractStruct
{
  protected static final String[] s_type = {"Default", "Melee", "Ranged", "Magical", "Launcher"};
  protected static final String[] s_targettype = {"", "Living actor", "Inventory", "Dead actor",
                                                  "Any point within range", "Caster", "",
                                                  "Caster (keep spell, no animation)"};
  protected static final String[] s_dmgtype = {"None", "Piercing", "Crushing", "Slashing", "Missile",
                                               "Fist"};

  protected AbstractAbility(AbstractStruct superStruct, String name, byte buffer[], int offset)
          throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Writeable w = list.get(i);
      if (w instanceof Effect)
        return;
      w.write(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof Effect && getEffectsCount() >= 1) {
      SectionOffset effectOffset = (SectionOffset)getSuperStruct().getAttribute("Effects offset");
      int effectIndex = ((DecNumber)getAttribute("Effects index")).getValue() + getEffectsCount() - 1;
      datatype.setOffset(effectOffset.getValue() + effectIndex * 48);
    }
  }

  public int getEffectsCount()
  {
    return ((SectionCount)getAttribute("# effects")).getValue();
  }

  public void incEffectsIndex(int value)
  {
    ((DecNumber)getAttribute("Effects index")).incValue(value);
  }

  public int readEffects(byte buffer[], int off) throws Exception
  {
    int effect_count = ((SectionCount)getAttribute("# effects")).getValue();
    for (int i = 0; i < effect_count; i++) {
      Effect eff = new Effect(this, buffer, off);
      off = eff.getEndOffset();
      list.add(eff);
    }
    return off;
  }

  public void setEffectsIndex(int value)
  {
    ((DecNumber)getAttribute("Effects index")).setValue(value);
  }

  public void writeEffects(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Writeable w = list.get(i);
      if (w instanceof Effect)
        w.write(os);
    }
  }
}

