// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import infinity.datatype.HashBitmap;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.util.LongIntegerHashMap;

public final class ProType extends HashBitmap
{
  private static final LongIntegerHashMap<String> s_projtype = new LongIntegerHashMap<String>();

  static {
    s_projtype.put(new Long(1), "No BAM");
    s_projtype.put(new Long(2), "Single target");
    s_projtype.put(new Long(3), "Area of effect");
  }


  public ProType(byte buffer[], int offset)
  {
    super(buffer, offset, 2, "Projectile type", s_projtype);
  }

//--------------------- Begin Interface Editable ---------------------

  public boolean updateValue(AbstractStruct struct)
  {
    super.updateValue(struct);

    // add/remove extended sections in the parent structure depending on the current value
    if (struct instanceof Resource && struct instanceof HasAddRemovable) {
      if (getValue() == 3L) {         // area of effect
        StructEntry entry = struct.getList().get(struct.getList().size() - 1);
        try {
          if (!(entry instanceof ProSingleType) && !(entry instanceof ProAreaType))
            struct.addDatatype(new ProSingleType(), struct.getList().size());
          entry = struct.getList().get(struct.getList().size() - 1);
          if (!(entry instanceof ProAreaType))
            struct.addDatatype(new ProAreaType(), struct.getList().size());
        } catch (Exception e) {
          e.printStackTrace();
          return false;
        }
      } else if (getValue() == 2L) {  // single target
        StructEntry entry = struct.getList().get(struct.getList().size() - 1);
        if (entry instanceof ProAreaType)
          struct.removeDatatype((AddRemovable)entry, false);
        entry = struct.getList().get(struct.getList().size() - 1);
        if (!(entry instanceof ProSingleType)) {
          try {
            struct.addDatatype(new ProSingleType(), struct.getList().size());
          } catch (Exception e) {
            e.printStackTrace();
            return false;
          }
        }
      } else if (getValue() == 1L) {  // no bam
        if (struct.getList().size() > 2) {
          StructEntry entry = struct.getList().get(struct.getList().size() - 1);
          if (entry instanceof ProAreaType)
            struct.removeDatatype((AddRemovable)entry, false);
          entry = struct.getList().get(struct.getList().size() - 1);
          if (entry instanceof ProSingleType)
            struct.removeDatatype((AddRemovable)entry, false);
        }
      } else
        return false;
      return true;
    }
    return false;
  }

//--------------------- End Interface Editable ---------------------
}
