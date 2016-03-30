// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are;

import java.nio.ByteBuffer;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public final class AutomapNotePST extends AbstractStruct implements AddRemovable
{
  // ARE/Automap Notes-specific field labels
  public static final String ARE_AUTOMAP            = "Automap note";
  public static final String ARE_AUTOMAP_LOCATION_X = "Coordinate: X";
  public static final String ARE_AUTOMAP_LOCATION_Y = "Coordinate: Y";
  public static final String ARE_AUTOMAP_TEXT       = "Text";
  public static final String ARE_AUTOMAP_READ_ONLY  = "Is read only?";

  public static final String[] s_noyes = { "No", "Yes" };

  AutomapNotePST() throws Exception
  {
    super(null, ARE_AUTOMAP, StreamUtils.getByteBuffer(532), 0);
  }

  AutomapNotePST(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, ARE_AUTOMAP + " " + number, buffer, offset);
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
    addField(new DecNumber(buffer, offset, 4, ARE_AUTOMAP_LOCATION_X));
    addField(new DecNumber(buffer, offset + 4, 4, ARE_AUTOMAP_LOCATION_Y));
    addField(new TextString(buffer, offset + 8, 500, ARE_AUTOMAP_TEXT));
    addField(new Bitmap(buffer, offset + 508, 4, ARE_AUTOMAP_READ_ONLY, s_noyes));
    addField(new Unknown(buffer, offset + 512, 20));
    return offset + 532;
  }
}

