// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;

public final class Overlay extends AbstractStruct // implements AddRemovable, HasAddRemovable
{
  public Overlay(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Overlay", buffer, offset);
  }

  public void updateOffsets(int offset, int size)
  {
    HexNumber offset_tilemap = (HexNumber)getAttribute("Tilemap offset");
    if (offset_tilemap.getValue() >= offset)
      offset_tilemap.incValue(size);

    HexNumber offset_tilelookup = (HexNumber)getAttribute("Tilemap lookup offset");
    if (offset_tilelookup.getValue() >= offset)
      offset_tilelookup.incValue(size);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    DecNumber width = new DecNumber(buffer, offset, 2, "Width");
    list.add(width);
    DecNumber height = new DecNumber(buffer, offset + 2, 2, "Height");
    list.add(height);
    ResourceRef tileset = new ResourceRef(buffer, offset + 4, "Tileset", "TIS");
    list.add(tileset);
    list.add(new Unknown(buffer, offset + 12, 4));
    SectionOffset offset_tilemap = new SectionOffset(buffer, offset + 16, "Tilemap offset", null);
    list.add(offset_tilemap);
    SectionOffset offset_tilelookup = new SectionOffset(buffer, offset + 20, "Tilemap lookup offset", null);
    list.add(offset_tilelookup);
    int retoff = offset + 24;

    // readTilemap
    int lookuptablesize = 0;
    if (!tileset.toString().equalsIgnoreCase(".TIS")) {
      offset = offset_tilemap.getValue();
      int map_count = width.getValue() * height.getValue();
      for (int i = 0; i < map_count; i++) {
        Tilemap map = new Tilemap(this, buffer, offset, i);
        offset = map.getEndOffset();
        lookuptablesize += map.getTileCount();
        list.add(map);
      }
    }
    // readLookuptable
    offset = offset_tilelookup.getValue();
    for (int i = 0; i < lookuptablesize; i++)
      list.add(new DecNumber(buffer, offset + i * 2, 2, "Tilemap index " + i));
    return retoff;
  }
}

