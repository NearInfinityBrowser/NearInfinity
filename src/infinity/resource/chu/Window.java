// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.StructEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

final class Window extends AbstractStruct // implements AddRemovable
{
  public static final String FMT_NAME = "Panel %1$d";

  private static final String hasb[] = {"No", "Yes"};
  private static final String s_flag[] = {"No flags set", "Don't dim background"};

  Window() throws Exception
  {
    super(null, "Panel", new byte[36], 0);
  }

  Window(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, String.format(FMT_NAME, nr), buffer, offset);
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
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

  public ChuResource getChu()
  {
    if (getSuperStruct() instanceof ChuResource) {
      return (ChuResource)getSuperStruct();
    } else {
      return null;
    }
  }

  public int readControls(byte buffer[]) throws Exception
  {
    int numctrl = (int)((UnsignDecNumber)getAttribute("# controls")).getValue();
    int first = (int)((UnsignDecNumber)getAttribute("First control index")).getValue();
    int controlsoffset = getChu().getControlsOffset() + (first*8);
    int endoffset = controlsoffset;
    for (int i = 0; i < numctrl; i++) {
      int size = getChu().getControlOffset(first+i+1) - getChu().getControlOffset(first+i);
      Control control = new Control(this, buffer, controlsoffset, i, size);
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

  @Override
  protected int read(byte buffer[], int offset) throws Exception
  {
    if (getChu().getPanelSize() == 36) {
      list.add(0, new TextString(buffer, offset, 8, "Name"));
      offset += 8;
    }
    list.add(new DecNumber(buffer, offset, 2, "Panel ID"));
    list.add(new Unknown(buffer, offset + 2, 2));
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

