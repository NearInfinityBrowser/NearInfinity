// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Profile;
import infinity.util.LongIntegerHashMap;

public final class JournalEntry extends AbstractStruct implements AddRemovable
{
  // GAM/JournalEntry-specific field labels
  public static final String GAM_JOURNAL          = "Journal entry";
  public static final String GAM_JOURNAL_TEXT     = "Text";
  public static final String GAM_JOURNAL_TIME     = "Time (ticks)";
  public static final String GAM_JOURNAL_CHAPTER  = "Chapter";
  public static final String GAM_JOURNAL_SECTION  = "Section";
  public static final String GAM_JOURNAL_SOURCE   = "Text source";

  private static final LongIntegerHashMap<String> m_source = new LongIntegerHashMap<String>();
  public static final String s_section[] = new String[]{"User notes", "Quests", "Done quests",
                                                        "Journal"};

  static {
    m_source.put(new Long(0x1f), "From talk override");
    m_source.put(new Long(0xff), "From dialog.tlk");
  }


  JournalEntry() throws Exception
  {
    super(null, GAM_JOURNAL, new byte[12], 0);
  }

  JournalEntry(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, GAM_JOURNAL + " " + number, buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, GAM_JOURNAL_TEXT));
    addField(new DecNumber(buffer, offset + 4, 4, GAM_JOURNAL_TIME));
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new UnsignDecNumber(buffer, offset + 8, 1, GAM_JOURNAL_CHAPTER));
      addField(new Unknown(buffer, offset + 9, 1));
      addField(new Flag(buffer, offset + 10, 1, GAM_JOURNAL_SECTION, s_section));
      addField(new HashBitmap(buffer, offset + 11, 1, GAM_JOURNAL_SOURCE, m_source));
    }
    else {
      addField(new DecNumber(buffer, offset + 8, 2, GAM_JOURNAL_CHAPTER));
      addField(new Unknown(buffer, offset + 10, 2));
    }
    return offset + 12;
  }
}

