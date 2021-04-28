// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;
import java.util.TreeMap;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.HashBitmap;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.util.io.StreamUtils;

public final class JournalEntry extends AbstractStruct implements AddRemovable
{
  // GAM/JournalEntry-specific field labels
  public static final String GAM_JOURNAL          = "Journal entry";
  public static final String GAM_JOURNAL_TEXT     = "Text";
  public static final String GAM_JOURNAL_TIME     = "Time (ticks)";
  public static final String GAM_JOURNAL_CHAPTER  = "Chapter";
  public static final String GAM_JOURNAL_SECTION  = "Section";
  public static final String GAM_JOURNAL_SOURCE   = "Text source";

  private static final TreeMap<Long, String> m_source = new TreeMap<>();
  public static final String s_section[] = new String[]{"User notes", "Quests", "Done quests",
                                                        "Journal"};

  static {
    m_source.put(0x1fL, "From talk override");
    m_source.put(0xffL, "From dialog.tlk");
  }


  JournalEntry() throws Exception
  {
    super(null, GAM_JOURNAL, StreamUtils.getByteBuffer(12), 0);
  }

  JournalEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
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
  public int read(ByteBuffer buffer, int offset) throws Exception
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

