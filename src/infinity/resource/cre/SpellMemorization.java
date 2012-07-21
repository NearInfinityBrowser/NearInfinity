// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.*;
import infinity.resource.*;

final class SpellMemorization extends AbstractStruct implements AddRemovable, HasAddRemovable
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

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new MemorizedSpells()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

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

  public int read(byte buffer[], int offset)
  {
    list.add(new DecNumber(buffer, offset, 2, "Spell level"));
    list.add(new DecNumber(buffer, offset + 2, 2, "# spells memorizable"));
    list.add(new DecNumber(buffer, offset + 4, 2, "# currently memorizable"));
    list.add(new Bitmap(buffer, offset + 6, 2, "Type", s_spelltype));
    list.add(new DecNumber(buffer, offset + 8, 4, "Spell table index"));
    list.add(new DecNumber(buffer, offset + 12, 4, "Spell count"));
    return offset + 16;
  }

  public void readMemorizedSpells(byte buffer[], int offset) throws Exception
  {
    DecNumber firstSpell = (DecNumber)getAttribute("Spell table index");
    DecNumber numSpell = (DecNumber)getAttribute("Spell count");
    for (int i = 0; i < numSpell.getValue(); i++)
      list.add(new MemorizedSpells(this, buffer, offset + 12 * (firstSpell.getValue() + i)));
  }

  public int updateSpells(int offset, int startIndex)
  {
    ((DecNumber)getAttribute("Spell table index")).setValue(startIndex);
    int count = 0;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
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

