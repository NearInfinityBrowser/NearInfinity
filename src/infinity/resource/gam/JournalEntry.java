// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.ResourceFactory;
import infinity.util.LongIntegerHashMap;

final class JournalEntry extends AbstractStruct implements AddRemovable
{
  private static final LongIntegerHashMap<String> chapter = new LongIntegerHashMap<String>();
  private static final String s_section[] = new String[]{"User notes", "Quests", "Done quests",
                                                         "Journal"};

  static {
    chapter.put(0x1f, "From talk override");
    chapter.put(0xff, "From dialog.tlk");
  }


  JournalEntry() throws Exception
  {
    super(null, "Journal entry", new byte[12], 0);
  }

  JournalEntry(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Journal entry", buffer, offset);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new StringRef(buffer, offset, "Text"));
    list.add(new DecNumber(buffer, offset + 4, 4, "Time (ticks)"));
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_TUTU) {
      list.add(new UnsignDecNumber(buffer, offset + 8, 1, "Chapter"));
      list.add(new Unknown(buffer, offset + 9, 1));
      list.add(new Flag(buffer, offset + 10, 1, "Section", s_section));
      list.add(new HashBitmap(buffer, offset + 11, 1, "Text source", chapter));
    }
    else {
      list.add(new DecNumber(buffer, offset + 8, 2, "Chapter"));
      list.add(new Unknown(buffer, offset + 10, 2));
    }
    return offset + 12;
  }
}

