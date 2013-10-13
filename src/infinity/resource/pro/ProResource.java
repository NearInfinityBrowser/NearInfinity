// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.HashBitmapEx;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UpdateEvent;
import infinity.datatype.UpdateListener;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.LongIntegerHashMap;

public final class ProResource extends AbstractStruct implements Resource, HasAddRemovable, UpdateListener
{
  private static final String[] s_color = {"", "Black", "Blue", "Chromatic", "Gold",
                                           "Green", "Purple", "Red", "White", "Ice",
                                           "Stone", "Magenta", "Orange"};
  private static final String[] s_behave = {"No flags set", "Show sparks", "Use height",
                                            "Loop fire sound", "Loop impact sound", "Ignore center",
                                            "Draw as background"};
  private static final LongIntegerHashMap<String> m_projtype = new LongIntegerHashMap<String>();
  static {
    m_projtype.put(1L, "No BAM");
    m_projtype.put(2L, "Single target");
    m_projtype.put(3L, "Area of effect");
  }


  public ProResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

//--------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return null;
  }

//--------------------- End Interface HasAddRemovable ---------------------

//--------------------- Begin Interface UpdateListener ---------------------

  @Override
  public boolean valueUpdated(UpdateEvent event)
  {
    if (event.getSource() instanceof HashBitmap) {
      HashBitmap proType = (HashBitmap)event.getSource();
      AbstractStruct struct = event.getStructure();
      // add/remove extended sections in the parent structure depending on the current value
      if (struct instanceof Resource && struct instanceof HasAddRemovable) {
        if (proType.getValue() == 3L) {         // area of effect
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
        } else if (proType.getValue() == 2L) {  // single target
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
        } else if (proType.getValue() == 1L) {  // no bam
          if (struct.getList().size() > 2) {
            StructEntry entry = struct.getList().get(struct.getList().size() - 1);
            if (entry instanceof ProAreaType)
              struct.removeDatatype((AddRemovable)entry, false);
            entry = struct.getList().get(struct.getList().size() - 1);
            if (entry instanceof ProSingleType)
              struct.removeDatatype((AddRemovable)entry, false);
          }
        } else {
          return false;
        }
        return true;
      }
    }
    return false;
  }

//--------------------- End Interface UpdateListener ---------------------

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    HashBitmapEx projtype = new HashBitmapEx(buffer, offset + 8, 2, "Projectile type", m_projtype);
    projtype.addUpdateListener(this);
    list.add(projtype);
    list.add(new DecNumber(buffer, offset + 10, 2, "Speed"));
    list.add(new Flag(buffer, offset + 12, 4, "Behavior", s_behave));
    list.add(new ResourceRef(buffer, offset + 16, "Fire sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 24, "Impact sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 32, "Source animation", new String[]{"VVC", "BAM"}));
    list.add(new Bitmap(buffer, offset + 40, 4, "Particle color", s_color));
    list.add(new Unknown(buffer, offset + 44, 212));
    offset += 256;

    if (projtype.getValue() > 1L) {
      ProSingleType single = new ProSingleType(this, buffer, offset);
      list.add(single);
      offset += single.getSize();
    }
    if (projtype.getValue() > 2L) {
      ProAreaType area = new ProAreaType(this, buffer, offset);
      list.add(area);
      offset += area.getSize();
    }

    return offset;
  }
}
