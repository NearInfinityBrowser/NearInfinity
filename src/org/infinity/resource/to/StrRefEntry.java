// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;

import org.infinity.datatype.HexNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.io.StreamUtils;

public class StrRefEntry extends AbstractStruct
{
  // TOH/StrrefEntry-specific field labels
  public static final String TOH_STRREF                   = "StrRef entry";
  public static final String TOH_STRREF_OVERRIDDEN        = "Overridden strref";
  public static final String TOH_STRREF_SOUND             = "Associated sound";
  public static final String TOH_STRREF_OFFSET_TOT_STRING = "TOT string offset";

  public StrRefEntry() throws Exception
  {
    super(null, TOH_STRREF, StreamUtils.getByteBuffer(28), 0);
  }

  public StrRefEntry(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, TOH_STRREF + " " + nr, buffer, offset);
  }

  public StrRefEntry(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset) throws Exception
  {
    super(superStruct, name, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new StringRef(buffer, offset, TOH_STRREF_OVERRIDDEN));
    addField(new Unknown(buffer, offset + 4, 4));
    addField(new Unknown(buffer, offset + 8, 4));
    addField(new Unknown(buffer, offset + 12, 4));
    addField(new ResourceRef(buffer, offset + 16, TOH_STRREF_SOUND, "WAV"));
    addField(new HexNumber(buffer, offset + 24, 4, TOH_STRREF_OFFSET_TOT_STRING));
    return offset + 28;
  }
}
