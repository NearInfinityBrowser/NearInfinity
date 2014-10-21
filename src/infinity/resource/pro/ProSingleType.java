// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.pro;

import infinity.datatype.ColorValue;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.util.LongIntegerHashMap;

public final class ProSingleType extends AbstractStruct implements AddRemovable
{
  public static final LongIntegerHashMap<String> s_facetarget = new LongIntegerHashMap<String>();
  public static final String[] s_flags = {"No flags set", "Colored BAM", "Creates smoke", "Colored smoke",
                                          "Not light source", "Modify for height", "Casts shadow",
                                          "Light spot enabled", "Translucent", "Mid-level brighten", "Blended"};
  public static final String[] s_trail = {"No flags set", "Draw at target", "Draw at source"};

  static {
    s_facetarget.put(new Long(1), "Do not face target");
    s_facetarget.put(new Long(5), "Mirrored east (reduced)");
    s_facetarget.put(new Long(9), "Mirrored east (full)");
    s_facetarget.put(new Long(16), "Not mirrored (full)");
  }


  public ProSingleType() throws Exception
  {
    super(null, "Projectile info", new byte[256], 0);
    setOffset(256);
  }

  public ProSingleType(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, "Projectile info", buffer, offset);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return false;   // can not be removed manually
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
  public int read(byte[] buffer, int offset) throws Exception
  {
    list.add(new Flag(buffer, offset, 4, "Flags", s_flags));
    list.add(new ResourceRef(buffer, offset + 4, "Projectile animation", "BAM"));
    list.add(new ResourceRef(buffer, offset + 12, "Shadow animation", "BAM"));
    list.add(new DecNumber(buffer, offset + 20, 1, "Projectile animation number"));
    list.add(new DecNumber(buffer, offset + 21, 1, "Shadow animation number"));
    list.add(new DecNumber(buffer, offset + 22, 2, "Light spot intensity"));
    list.add(new DecNumber(buffer, offset + 24, 2, "Light spot width"));
    list.add(new DecNumber(buffer, offset + 26, 2, "Light spot height"));
    list.add(new ResourceRef(buffer, offset + 28, "Palette", "BMP"));
    list.add(new ColorValue(buffer, offset + 36, 1, "Projectile color 1"));
    list.add(new ColorValue(buffer, offset + 37, 1, "Projectile color 2"));
    list.add(new ColorValue(buffer, offset + 38, 1, "Projectile color 3"));
    list.add(new ColorValue(buffer, offset + 39, 1, "Projectile color 4"));
    list.add(new ColorValue(buffer, offset + 40, 1, "Projectile color 5"));
    list.add(new ColorValue(buffer, offset + 41, 1, "Projectile color 6"));
    list.add(new ColorValue(buffer, offset + 42, 1, "Projectile color 7"));
    list.add(new DecNumber(buffer, offset + 43, 1, "Smoke puff delay"));
    list.add(new ColorValue(buffer, offset + 44, 1, "Smoke color 1"));
    list.add(new ColorValue(buffer, offset + 45, 1, "Smoke color 2"));
    list.add(new ColorValue(buffer, offset + 46, 1, "Smoke color 3"));
    list.add(new ColorValue(buffer, offset + 47, 1, "Smoke color 4"));
    list.add(new ColorValue(buffer, offset + 48, 1, "Smoke color 5"));
    list.add(new ColorValue(buffer, offset + 49, 1, "Smoke color 6"));
    list.add(new ColorValue(buffer, offset + 50, 1, "Smoke color 7"));
    list.add(new HashBitmap(buffer, offset + 51, 1, "Face target granularity", s_facetarget));
    list.add(new IdsBitmap(buffer, offset + 52, 2, "Smoke animation", "ANIMATE.IDS"));
    list.add(new ResourceRef(buffer, offset + 54, "Trailing animation 1", "BAM"));
    list.add(new ResourceRef(buffer, offset + 62, "Trailing animation 2", "BAM"));
    list.add(new ResourceRef(buffer, offset + 70, "Trailing animation 3", "BAM"));
    list.add(new DecNumber(buffer, offset + 78, 2, "Tailing animation delay 1"));
    list.add(new DecNumber(buffer, offset + 80, 2, "Tailing animation delay 2"));
    list.add(new DecNumber(buffer, offset + 82, 2, "Tailing animation delay 3"));
    list.add(new Flag(buffer, offset + 84, 4, "Trail flags", s_trail));
    list.add(new Unknown(buffer, offset + 88, 168));

    return offset + 256;
  }
}
