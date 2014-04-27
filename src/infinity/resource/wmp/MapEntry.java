// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.HasDetailViewer;

import javax.swing.JComponent;

final class MapEntry extends AbstractStruct implements HasDetailViewer
{
  MapEntry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Map " + nr, buffer, offset);
  }

// --------------------- Begin Interface HasDetailViewer ---------------------

  @Override
  public JComponent getDetailViewer()
  {
    return new ViewerMap(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Map", "MOS"));
    list.add(new DecNumber(buffer, offset + 8, 4, "Width"));
    list.add(new DecNumber(buffer, offset + 12, 4, "Height"));
    list.add(new DecNumber(buffer, offset + 16, 4, "Map ID"));
    list.add(new StringRef(buffer, offset + 20, "Name"));
    list.add(new DecNumber(buffer, offset + 24, 4, "Center location: X"));
    list.add(new DecNumber(buffer, offset + 28, 4, "Center location: Y"));
    SectionCount area_count = new SectionCount(buffer, offset + 32, 4, "# areas", AreaEntry.class);
    list.add(area_count);
    SectionOffset area_offset = new SectionOffset(buffer, offset + 36, "Areas offset", AreaEntry.class);
    list.add(area_offset);
    SectionOffset link_offset = new SectionOffset(buffer, offset + 40, "Area links offset", AreaLink.class);
    list.add(link_offset);
    SectionCount link_count = new SectionCount(buffer, offset + 44, 4, "# area links", AreaLink.class);
    list.add(link_count);
    list.add(new ResourceRef(buffer, offset + 48, "Map icons", "BAM"));
    list.add(new Unknown(buffer, offset + 56, 128));

    int curOfs = area_offset.getValue();
    for (int i = 0; i < area_count.getValue(); i++) {
      AreaEntry areaEntry = new AreaEntry(this, buffer, curOfs, i);
      curOfs = areaEntry.getEndOffset();
      list.add(areaEntry);
      areaEntry.readLinks(buffer, link_offset);
    }

    return offset + 128 + 56;
  }
}

