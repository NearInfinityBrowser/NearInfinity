// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.StreamUtils;

public final class SpellMemorization extends AbstractStruct implements AddRemovable, HasChildStructs
{
  // CRE/SpellMemorization-specific field labels
  public static final String CRE_MEMORIZATION                         = "Memorization info";
  public static final String CRE_MEMORIZATION_LEVEL                   = "Spell level";
  public static final String CRE_MEMORIZATION_NUM_MEMORIZABLE_TOTAL   = "# spells memorizable";
  public static final String CRE_MEMORIZATION_NUM_MEMORIZABLE_CURRENT = "# currently memorizable";
  public static final String CRE_MEMORIZATION_TYPE                    = "Type";
  public static final String CRE_MEMORIZATION_SPELL_TABLE_INDEX       = "Spell table index";
  public static final String CRE_MEMORIZATION_SPELL_COUNT             = "Spell count";

  SpellMemorization() throws Exception
  {
    super(null, CRE_MEMORIZATION, StreamUtils.getByteBuffer(16), 0);
  }

  SpellMemorization(CreResource cre, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(cre, CRE_MEMORIZATION + " " + nr, buffer, offset);
  }

  @Override
  public boolean canRemove()
  {
    return true;
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new MemorizedSpells()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof MemorizedSpells) {
      int index = ((IsNumeric)getAttribute(CRE_MEMORIZATION_SPELL_TABLE_INDEX)).getValue();
      index += ((IsNumeric)getAttribute(CRE_MEMORIZATION_SPELL_COUNT)).getValue();
      final CreResource cre = (CreResource)getParent();
      int offset = ((IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_MEMORIZED_SPELLS)).getValue() +
                   cre.getExtraOffset();
      datatype.setOffset(offset + 12 * index);
      ((AbstractStruct)datatype).realignStructOffsets();
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    addField(new DecNumber(buffer, offset, 2, CRE_MEMORIZATION_LEVEL));
    addField(new DecNumber(buffer, offset + 2, 2, CRE_MEMORIZATION_NUM_MEMORIZABLE_TOTAL));
    addField(new DecNumber(buffer, offset + 4, 2, CRE_MEMORIZATION_NUM_MEMORIZABLE_CURRENT));
    addField(new Bitmap(buffer, offset + 6, 2, CRE_MEMORIZATION_TYPE, KnownSpells.s_spelltype));
    addField(new DecNumber(buffer, offset + 8, 4, CRE_MEMORIZATION_SPELL_TABLE_INDEX));
    addField(new DecNumber(buffer, offset + 12, 4, CRE_MEMORIZATION_SPELL_COUNT));
    return offset + 16;
  }

  public void readMemorizedSpells(ByteBuffer buffer, int offset) throws Exception
  {
    IsNumeric firstSpell = (IsNumeric)getAttribute(CRE_MEMORIZATION_SPELL_TABLE_INDEX);
    IsNumeric numSpell = (IsNumeric)getAttribute(CRE_MEMORIZATION_SPELL_COUNT);
    for (int i = 0; i < numSpell.getValue(); i++) {
      addField(new MemorizedSpells(this, buffer, offset + 12 * (firstSpell.getValue() + i)));
    }
  }

  public int updateSpells(int offset, int startIndex)
  {
    ((DecNumber)getAttribute(CRE_MEMORIZATION_SPELL_TABLE_INDEX)).setValue(startIndex);
    int count = 0;
    for (final StructEntry entry : getFields()) {
      if (entry instanceof MemorizedSpells) {
        entry.setOffset(offset);
        ((AbstractStruct)entry).realignStructOffsets();
        offset += 12;
        count++;
      }
    }
    ((DecNumber)getAttribute(CRE_MEMORIZATION_SPELL_COUNT)).setValue(count);
    return count;
  }
}
