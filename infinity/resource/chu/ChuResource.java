// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

public final class ChuResource extends AbstractStruct implements Resource //, HasAddRemovable
{
  public ChuResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Window)
        ((Window)o).writeControlsTable(os);
    }
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Window)
        ((Window)o).writeControls(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    SectionCount numwindows = new SectionCount(buffer, offset + 8, 4, "# panels",
                                               Window.class);
    list.add(numwindows);
    SectionOffset controlsoffset = new SectionOffset(buffer, offset + 12, "Controls offset",
                                                     Control.class);
    list.add(controlsoffset);
    SectionOffset windowsoffset = new SectionOffset(buffer, offset + 16, "Panels offset",
                                                    Window.class);
    list.add(windowsoffset);

    offset = windowsoffset.getValue();
    int endoffset = offset;
    int windowsize = (controlsoffset.getValue() - windowsoffset.getValue()) / numwindows.getValue();
    for (int i = 0; i < numwindows.getValue(); i++) {
      Window window = new Window(this, buffer, offset, i, windowsize);
      offset = window.getEndOffset();
      endoffset = Math.max(endoffset, window.readControls(buffer, controlsoffset.getValue()));
      list.add(window);
    }
    return Math.max(offset, endoffset);
  }
}

