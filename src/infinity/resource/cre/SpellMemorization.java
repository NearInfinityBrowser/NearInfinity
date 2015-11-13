// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.StructEntry;

public final class SpellMemorization extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  private static final String[] s_spelltype = {"Priest", "Wizard", "Innate"};

  SpellMemorization() throws Exception
  {
    super(null, "Memorization info", new byte[16], 0);
  }

  SpellMemorization(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Memorization info " + nr, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new MemorizedSpells()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof MemorizedSpells) {
      int index = ((DecNumber)getAttribute("Spell table index")).getValue();
      index += ((DecNumber)getAttribute("Spell count")).getValue();
      CreResource cre = (CreResource)getSuperStruct();
      int offset = ((HexNumber)cre.getAttribute("Memorized spells offset")).getValue() +
                   cre.getExtraOffset();
      datatype.setOffset(offset + 12 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  @Override
  public int read(byte buffer[], int offset)
  {
    addField(new DecNumber(buffer, offset, 2, "Spell level"));
    addField(new DecNumber(buffer, offset + 2, 2, "# spells memorizable"));
    addField(new DecNumber(buffer, offset + 4, 2, "# currently memorizable"));
    addField(new Bitmap(buffer, offset + 6, 2, "Type", s_spelltype));
    addField(new DecNumber(buffer, offset + 8, 4, "Spell table index"));
    addField(new DecNumber(buffer, offset + 12, 4, "Spell count"));
    return offset + 16;
  }

  public void readMemorizedSpells(byte buffer[], int offset) throws Exception
  {
    DecNumber firstSpell = (DecNumber)getAttribute("Spell table index");
    DecNumber numSpell = (DecNumber)getAttribute("Spell count");
    for (int i = 0; i < numSpell.getValue(); i++) {
      addField(new MemorizedSpells(this, buffer, offset + 12 * (firstSpell.getValue() + i)));
    }
  }

  public int updateSpells(int offset, int startIndex)
  {
    ((DecNumber)getAttribute("Spell table index")).setValue(startIndex);
    int count = 0;
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
      if (entry instanceof MemorizedSpells) {
        entry.setOffset(offset);
        ((AbstractStruct)entry).realignStructOffsets();
        offset += 12;
        count++;
      }
    }
    ((DecNumber)getAttribute("Spell count")).setValue(count);
    return count;
  }
}

