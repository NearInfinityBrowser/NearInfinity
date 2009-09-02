// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.DecNumber;
import infinity.resource.*;

public final class Iwd2Struct extends AbstractStruct implements HasAddRemovable
{
  public static final int TYPE_SPELL = 0;
  public static final int TYPE_ABILITY = 1;
  public static final int TYPE_SHAPE = 2;
  public static final int TYPE_SONG = 3;
  private final DecNumber count;
  private final int type;

  public Iwd2Struct(AbstractStruct superStruct, byte buffer[], int offset, DecNumber count, String name,
                    int type) throws Exception
  {
    super(superStruct, name, offset, count.getValue() + 2);
    this.count = count;
    this.type = type;
    if (type == TYPE_SPELL)
      for (int i = 0; i < count.getValue(); i++)
        list.add(new Iwd2Spell(this, buffer, offset + 16 * i));
    else if (type == TYPE_ABILITY)
      for (int i = 0; i < count.getValue(); i++)
        list.add(new Iwd2Ability(this, buffer, offset + 16 * i));
    else if (type == TYPE_SHAPE)
      for (int i = 0; i < count.getValue(); i++)
        list.add(new Iwd2Shape(this, buffer, offset + 16 * i));
    else if (type == TYPE_SONG)
      for (int i = 0; i < count.getValue(); i++)
        list.add(new Iwd2Song(this, buffer, offset + 16 * i));
    list.add(new DecNumber(buffer, offset + 16 * count.getValue(), 4, "# memorizable (total)"));
    list.add(new DecNumber(buffer, offset + 16 * count.getValue() + 4, 4, "# free uses remaining"));
    setOffset(offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
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

// --------------------- End Interface HasAddRemovable ---------------------

  protected void datatypeAdded(AddRemovable datatype)
  {
    count.incValue(1);
  }

  protected void datatypeRemoved(AddRemovable datatype)
  {
    count.incValue(-1);
  }

  protected int getAddedPosition()
  {
    return count.getValue();
  }

  protected int read(byte buffer[], int offset)
  {
    return -1;
  }
}

