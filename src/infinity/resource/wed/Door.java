// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wed;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.RemovableDecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.HasAddRemovable;

public final class Door extends AbstractStruct implements AddRemovable, HasAddRemovable
{
  private static final String[] s_noyes = {"No", "Yes"};

  public Door() throws Exception
  {
    super(null, "Door", new byte[26], 0);
  }

  public Door(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Door " + number, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new OpenPolygon(), new ClosedPolygon()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------

  @Override
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
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Polygon)
        ((Polygon)o).readVertices(buffer, offset);
    }
  }

  public void updatePolygonsOffset(int offset)
  {
    int polyOffset = Integer.MAX_VALUE;
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Polygon) {
        polyOffset = Math.min(polyOffset, ((Polygon)o).getOffset());
      }
    }
    if (polyOffset != Integer.MAX_VALUE) {
      offset = polyOffset;
    }
    ((SectionOffset)getAttribute("Polygons open offset")).setValue(offset);
    for (int i = 0; i < getFieldCount(); i++) {
      if (getField(i) instanceof OpenPolygon) {
        offset += 18;
      }
    }
    ((SectionOffset)getAttribute("Polygons closed offset")).setValue(offset);
    for (int i = 0; i < getFieldCount(); i++) {
      if (getField(i) instanceof ClosedPolygon) {
        offset += 18;
      }
    }
//    return offset;
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 8, "Name"));
    addField(new Bitmap(buffer, offset + 8, 2, "Is door?", s_noyes));
    DecNumber indexTileCell = new DecNumber(buffer, offset + 10, 2, "Tilemap lookup index");
    addField(indexTileCell);
    SectionCount countTileCell = new SectionCount(buffer, offset + 12, 2, "# tilemap indexes",
                                                  RemovableDecNumber.class);
    addField(countTileCell);
    SectionCount countOpen = new SectionCount(buffer, offset + 14, 2, "# polygons open", OpenPolygon.class);
    addField(countOpen);
    SectionCount countClosed = new SectionCount(buffer, offset + 16, 2, "# polygons closed",
                                                ClosedPolygon.class);
    addField(countClosed);
    SectionOffset offsetOpen = new SectionOffset(buffer, offset + 18, "Polygons open offset",
                                                 OpenPolygon.class);
    addField(offsetOpen);
    SectionOffset offsetClosed = new SectionOffset(buffer, offset + 22, "Polygons closed offset",
                                                   ClosedPolygon.class);
    addField(offsetClosed);

    for (int i = 0; i < countOpen.getValue(); i++) {
      addField(new OpenPolygon(this, buffer, offsetOpen.getValue() + 18 * i, i));
    }

    for (int i = 0; i < countClosed.getValue(); i++) {
      addField(new ClosedPolygon(this, buffer, offsetClosed.getValue() + 18 * i, i));
    }

    if (getSuperStruct() != null) {
      HexNumber offsetTileCell = (HexNumber)getSuperStruct().getAttribute("Door tilemap lookup offset");
      for (int i = 0; i < countTileCell.getValue(); i++) {
        addField(new RemovableDecNumber(buffer, offsetTileCell.getValue() +
                                                2 * (indexTileCell.getValue() + i), 2,
                                        "Tilemap index " + i));
      }
    }
    return offset + 26;
  }
}

