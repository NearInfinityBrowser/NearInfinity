// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.*;
import infinity.resource.*;

final class Door extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  private static final String[] s_yesno = {"No", "Yes"};

  Door() throws Exception
  {
    super(null, "Door", new byte[26], 0);
  }

  Door(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Door", buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new OpenPolygon(), new ClosedPolygon()};
  }

// --------------------- End Interface HasAddRemovable ---------------------

  protected void setAddRemovableOffset(AddRemovable datatype)
  {
    if (datatype instanceof RemovableDecNumber) {
      int offset = ((HexNumber)getSuperStruct().getAttribute("Door tilemap lookup offset")).getValue();
      int index = getTilemapIndex().getValue();
      datatype.setOffset(offset + index * 2);
    }
  }

  public DecNumber getTilemapIndex()
  {
    return (DecNumber)getAttribute("Tilemap lookup index");
  }

  public void readVertices(byte buffer[], int offset) throws Exception
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Polygon)
        ((Polygon)o).readVertices(buffer, offset);
    }
  }

  public void updatePolygonsOffset(int offset)
  {
    int polyOffset = Integer.MAX_VALUE;
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Polygon)
        polyOffset = Math.min(polyOffset, ((Polygon)o).getOffset());
    }
    if (polyOffset != Integer.MAX_VALUE)
      offset = polyOffset;
    ((SectionOffset)getAttribute("Polygons open offset")).setValue(offset);
    for (int i = 0; i < list.size(); i++)
      if (list.get(i) instanceof OpenPolygon)
        offset += 18;
    ((SectionOffset)getAttribute("Polygons closed offset")).setValue(offset);
    for (int i = 0; i < list.size(); i++)
      if (list.get(i) instanceof ClosedPolygon)
        offset += 18;
//    return offset;
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 8, "Name"));
    list.add(new Bitmap(buffer, offset + 8, 2, "Is door?", s_yesno));
    DecNumber indexTileCell = new DecNumber(buffer, offset + 10, 2, "Tilemap lookup index");
    list.add(indexTileCell);
    SectionCount countTileCell = new SectionCount(buffer, offset + 12, 2, "# tilemap indexes",
                                                  RemovableDecNumber.class);
    list.add(countTileCell);
    SectionCount countOpen = new SectionCount(buffer, offset + 14, 2, "# polygons open", OpenPolygon.class);
    list.add(countOpen);
    SectionCount countClosed = new SectionCount(buffer, offset + 16, 2, "# polygons closed",
                                                ClosedPolygon.class);
    list.add(countClosed);
    SectionOffset offsetOpen = new SectionOffset(buffer, offset + 18, "Polygons open offset",
                                                 OpenPolygon.class);
    list.add(offsetOpen);
    SectionOffset offsetClosed = new SectionOffset(buffer, offset + 22, "Polygons closed offset",
                                                   ClosedPolygon.class);
    list.add(offsetClosed);

    for (int i = 0; i < countOpen.getValue(); i++)
      list.add(new OpenPolygon(this, buffer, offsetOpen.getValue() + 18 * i, i));

    for (int i = 0; i < countClosed.getValue(); i++)
      list.add(new ClosedPolygon(this, buffer, offsetClosed.getValue() + 18 * i, i));

    if (getSuperStruct() != null) {
      HexNumber offsetTileCell = (HexNumber)getSuperStruct().getAttribute("Door tilemap lookup offset");
      for (int i = 0; i < countTileCell.getValue(); i++)
        list.add(new RemovableDecNumber(buffer, offsetTileCell.getValue() +
                                                2 * (indexTileCell.getValue() + i), 2,
                                        "Tilemap index " + i));
    }
    return offset + 26;
  }
}

