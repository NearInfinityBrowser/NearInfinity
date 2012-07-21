// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

final class Window extends AbstractStruct // implements AddRemovable
{
  private static final String hasb[] = {"No", "Yes"};
  private static final String s_flag[] = {"No flags set", "Don't dim background"};

  Window() throws Exception
  {
    super(null, "Panel", new byte[28], 0);
  }

  Window(AbstractStruct superStruct, byte buffer[], int offset, int nr, int size) throws Exception
  {
    super(superStruct, "Panel " + nr, buffer, offset + (size - 28));
    if (size == 36) {
      list.add(0, new TextString(buffer, offset, 8, "Name"));
      setStartOffset(getOffset() - 8);
    }
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Collections.sort(list);
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry instanceof Control)
        break;
      else
        entry.write(os);
    }
  }

// --------------------- End Interface Writeable ---------------------


  public int readControls(byte buffer[], int controlsoffset) throws Exception
  {
    long numctrl = ((UnsignDecNumber)getAttribute("# controls")).getValue();
    long first = ((UnsignDecNumber)getAttribute("First control index")).getValue();
    controlsoffset += (int)(first * (long)8);
    int endoffset = controlsoffset;
    for (int i = 0; i < numctrl; i++) {
      Control control = new Control(this, buffer, controlsoffset);
      controlsoffset = control.getEndOffset();
      endoffset = control.readControl(buffer);
      list.add(control);
    }
    return endoffset;
  }

  public void writeControls(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Control)
        ((Control)o).writeControl(os);
    }
  }

  public void writeControlsTable(OutputStream os) throws IOException
  {
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Control)
        ((Control)o).write(os);
    }
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new DecNumber(buffer, offset, 4, "Panel ID"));
    list.add(new DecNumber(buffer, offset + 4, 2, "Position: X"));
    list.add(new DecNumber(buffer, offset + 6, 2, "Position: Y"));
    list.add(new DecNumber(buffer, offset + 8, 2, "Width"));
    list.add(new DecNumber(buffer, offset + 10, 2, "Height"));
    list.add(new Bitmap(buffer, offset + 12, 2, "Has background?", hasb));
    list.add(new UnsignDecNumber(buffer, offset + 14, 2, "# controls"));
    list.add(new ResourceRef(buffer, offset + 16, "Background image", "MOS"));
    list.add(new UnsignDecNumber(buffer, offset + 24, 2, "First control index"));
    list.add(new Flag(buffer, offset + 26, 2, "Flags", s_flag));
    return offset + 28;
  }
}

