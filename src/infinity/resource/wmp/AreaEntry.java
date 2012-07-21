// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import infinity.datatype.*;
import infinity.resource.*;

import javax.swing.*;

final class AreaEntry extends AbstractStruct implements AddRemovable, HasDetailViewer, HasAddRemovable
{
  private static final String s_flag[] = {"No flags set", "Visible", "Reveal from linked area",
                                          "Can be visited", "Has been visited"};

  AreaEntry() throws Exception
  {
    super(null, "Area", new byte[240], 0);
  }

  AreaEntry(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Area " + nr, buffer, offset);
  }

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[] { new AreaLinkNorth(), new AreaLinkSouth(),
                                new AreaLinkEast(), new AreaLinkWest() };
  }

// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    return new ViewerArea(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new ResourceRef(buffer, offset, "Current area", "ARE"));
    list.add(new ResourceRef(buffer, offset + 8, "Original area", "ARE"));
    list.add(new TextString(buffer, offset + 16, 32, "Script name"));
    list.add(new Flag(buffer, offset + 48, 4, "Flags", s_flag));
    list.add(new DecNumber(buffer, offset + 52, 4, "Icon number"));
    list.add(new DecNumber(buffer, offset + 56, 4, "Coordinate: X"));
    list.add(new DecNumber(buffer, offset + 60, 4, "Coordinate: Y"));
    list.add(new StringRef(buffer, offset + 64, "Name"));
    list.add(new StringRef(buffer, offset + 68, "Tooltip"));
    list.add(new ResourceRef(buffer, offset + 72, "Loading image", "MOS"));
    list.add(new DecNumber(buffer, offset + 80, 4, "First link (north)"));
    list.add(new SectionCount(buffer, offset + 84, 4, "# links (north)", AreaLinkNorth.class));
    list.add(new DecNumber(buffer, offset + 88, 4, "First link (west)"));
    list.add(new SectionCount(buffer, offset + 92, 4, "# links (west)", AreaLinkWest.class));
    list.add(new DecNumber(buffer, offset + 96, 4, "First link (south)"));
    list.add(new SectionCount(buffer, offset + 100, 4, "# links (south)", AreaLinkSouth.class));
    list.add(new DecNumber(buffer, offset + 104, 4, "First link (east)"));
    list.add(new SectionCount(buffer, offset + 108, 4, "# links (east)", AreaLinkEast.class));
    list.add(new Unknown(buffer, offset + 112, 128));
    return offset + 240;
  }

  void readLinks(byte[] buffer, DecNumber linkOffset) throws Exception
  {
    DecNumber northStart = (DecNumber)getAttribute("First link (north)");
    DecNumber northCount = (DecNumber)getAttribute("# links (north)");
    int offset = linkOffset.getValue() + northStart.getValue() * 216;
    for (int i = 0; i < northCount.getValue(); i++)
      list.add(new AreaLinkNorth(this, buffer, offset + i * 216));

    DecNumber westStart = (DecNumber)getAttribute("First link (west)");
    DecNumber westCount = (DecNumber)getAttribute("# links (west)");
    offset = linkOffset.getValue() + westStart.getValue() * 216;
    for (int i = 0; i < westCount.getValue(); i++)
      list.add(new AreaLinkWest(this, buffer, offset + i * 216));

    DecNumber southStart = (DecNumber)getAttribute("First link (south)");
    DecNumber southCount = (DecNumber)getAttribute("# links (south)");
    offset = linkOffset.getValue() + southStart.getValue() * 216;
    for (int i = 0; i < southCount.getValue(); i++)
      list.add(new AreaLinkSouth(this, buffer, offset + i * 216));

    DecNumber eastStart = (DecNumber)getAttribute("First link (east)");
    DecNumber eastCount = (DecNumber)getAttribute("# links (east)");
    offset = linkOffset.getValue() + eastStart.getValue() * 216;
    for (int i = 0; i < eastCount.getValue(); i++)
      list.add(new AreaLinkEast(this, buffer, offset + i * 216));
  }
}

