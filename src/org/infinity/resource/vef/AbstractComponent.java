// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.vef;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.StreamUtils;

public class AbstractComponent extends AbstractStruct implements AddRemovable
{
  // VEF/Component-specific field labels
  public static final String VEF_COMP_TICKS_START = "Ticks until start";
  public static final String VEF_COMP_TICKS_LOOP  = "Ticks until loop";
  public static final String VEF_COMP_CONTINUOUS  = "Continuous cycles?";

  protected AbstractComponent(String label) throws Exception
  {
    super(null, label, StreamUtils.getByteBuffer(224), 0);
  }

  protected AbstractComponent(AbstractStruct superStruct, ByteBuffer buffer, int offset, String label) throws Exception
  {
    super(superStruct, label, buffer, offset);
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
    addField(new DecNumber(buffer, offset, 4, VEF_COMP_TICKS_START));
    addField(new Unknown(buffer, offset + 4, 4));
    addField(new DecNumber(buffer, offset + 8, 4, VEF_COMP_TICKS_LOOP));
    VefType type = new VefType(buffer, offset + 12, 4);
    addField(type);

    List<StructEntry> list = new ArrayList<>();
    offset = type.readAttributes(buffer, offset + 16, list);
    addFields(getFields().size() - 1, list);

    addField(new Bitmap(buffer, offset, 4, VEF_COMP_CONTINUOUS, OPTION_NOYES));
    addField(new Unknown(buffer, offset + 4, 196));
    offset += 200;
    return offset;
  }
}
