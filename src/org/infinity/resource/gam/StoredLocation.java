// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.util.io.StreamUtils;

public class StoredLocation extends AbstractStruct implements AddRemovable
{
  // GAM/StoredLocation-specific field labels
  public static final String GAM_LOC            = "Stored location";
  public static final String GAM_LOC_AREA       = "Area";
  public static final String GAM_LOC_LOCATION_X = "Saved location: X";
  public static final String GAM_LOC_LOCATION_Y = "Saved location: Y";

  StoredLocation() throws Exception
  {
    super(null, GAM_LOC, StreamUtils.getByteBuffer(12), 0);
  }

  StoredLocation(AbstractStruct superStruct, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    this(superStruct, GAM_LOC, buffer, offset, nr);
  }

  StoredLocation(AbstractStruct superStruct, String name, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(superStruct, name + " " + nr, buffer, offset);
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
    addField(new ResourceRef(buffer, offset, GAM_LOC_AREA, "ARE"));
    addField(new DecNumber(buffer, offset + 8, 2, GAM_LOC_LOCATION_X));
    addField(new DecNumber(buffer, offset + 10, 2, GAM_LOC_LOCATION_Y));
    return offset + 12;
  }
}

