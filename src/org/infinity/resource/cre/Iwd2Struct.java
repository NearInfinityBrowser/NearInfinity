// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.HasChildStructs;

public final class Iwd2Struct extends AbstractStruct implements HasChildStructs
{
  // CRE/Iwd2Struct-specific field labels
  public static final String CRE_STRUCT_NUM_MEMORIZABLE = "# memorizable (total)";
  public static final String CRE_STRUCT_NUM_REMAINING = "# free uses remaining";

  public static final int TYPE_SPELL = 0;
  public static final int TYPE_ABILITY = 1;
  public static final int TYPE_SHAPE = 2;
  public static final int TYPE_SONG = 3;
  private final DecNumber count;
  private final int type;

  public Iwd2Struct(AbstractStruct superStruct, ByteBuffer buffer, int offset, DecNumber count, String name,
                    int type) throws Exception
  {
    super(superStruct, name, offset, count.getValue() + 2);
    this.count = count;
    this.type = type;
    if (type == TYPE_SPELL) {
      for (int i = 0; i < count.getValue(); i++) {
        addField(new Iwd2Spell(this, buffer, offset + 16 * i));
      }
    } else if (type == TYPE_ABILITY) {
      for (int i = 0; i < count.getValue(); i++) {
        addField(new Iwd2Ability(this, buffer, offset + 16 * i));
      }
    } else if (type == TYPE_SHAPE) {
      for (int i = 0; i < count.getValue(); i++) {
        addField(new Iwd2Shape(this, buffer, offset + 16 * i));
      }
    } else if (type == TYPE_SONG) {
      for (int i = 0; i < count.getValue(); i++) {
        addField(new Iwd2Song(this, buffer, offset + 16 * i));
      }
    }
    addField(new DecNumber(buffer, offset + 16 * count.getValue(), 4, CRE_STRUCT_NUM_MEMORIZABLE));
    addField(new DecNumber(buffer, offset + 16 * count.getValue() + 4, 4, CRE_STRUCT_NUM_REMAINING));
    setOffset(offset);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    switch (type) {
      case TYPE_SPELL:
        return new AddRemovable[]{new Iwd2Spell()};
      case TYPE_ABILITY:
        return new AddRemovable[]{new Iwd2Ability()};
      case TYPE_SHAPE:
        return new AddRemovable[]{new Iwd2Shape()};
      case TYPE_SONG:
        return new AddRemovable[]{new Iwd2Song()};
      default:
        return new AddRemovable[]{};
    }
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    count.incValue(1);
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    count.incValue(-1);
  }

  @Override
  protected int getInsertPosition()
  {
    return count.getValue();
  }

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    return -1;
  }
}
