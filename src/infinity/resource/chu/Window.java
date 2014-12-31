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

import java.awt.Dimension;
import java.awt.Point;
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
    Collections.sort(getList());
    for (int i = 0; i < getFieldCount(); i++) {
      StructEntry entry = getField(i);
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

  /** Returns the number of controls associated with this panel. */
  public int getControlCount()
  {
    return (int)((UnsignDecNumber)getAttribute("# controls")).getValue();
  }

  /** Returns the given control. Index is relative to the controls associated with this panel. */
  public Control getControl(int index)
  {
    if (index >= 0 && index < getControlCount()) {
      return (Control)getAttribute(String.format(Control.FMT_NAME, index));
    } else {
      return null;
    }
  }

  /** Returns the panel id. */
  public int getWindowId()
  {
    return ((DecNumber)getAttribute("Panel ID")).getValue();
  }

  /** Returns the x and y positions of the panel. */
  public Point getWindowPosition()
  {
    return new Point(((DecNumber)getAttribute("Position: X")).getValue(),
                     ((DecNumber)getAttribute("Position: Y")).getValue());
  }

  /** Returns width and height of the panel. */
  public Dimension getWindowDimension()
  {
    return new Dimension(((DecNumber)getAttribute("Width")).getValue(),
                         ((DecNumber)getAttribute("Height")).getValue());
  }

  /** Returns whether the panel references a background MOS. */
  public boolean hasBackgroundImage()
  {
    return ((Bitmap)getAttribute("Has background?")).getValue() == 1;
  }

  /** Returns the background MOS for the panel. */
  public String getBackgroundImage()
  {
    return ((ResourceRef)getAttribute("Background image")).getResourceName();
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
      addField(control);
    }
    return endoffset;
  }

  public void writeControls(OutputStream os) throws IOException
  {
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Control) {
        ((Control)o).writeControl(os);
      }
    }
  }

  public void writeControlsTable(OutputStream os) throws IOException
  {
    for (int i = 0; i < getFieldCount(); i++) {
      Object o = getField(i);
      if (o instanceof Control) {
        ((Control)o).write(os);
      }
    }
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    if (getChu().getPanelSize() == 36) {
      addField(new TextString(buffer, offset, 8, "Name"), 0);
      offset += 8;
    }
    addField(new DecNumber(buffer, offset, 2, "Panel ID"));
    addField(new Unknown(buffer, offset + 2, 2));
    addField(new DecNumber(buffer, offset + 4, 2, "Position: X"));
    addField(new DecNumber(buffer, offset + 6, 2, "Position: Y"));
    addField(new DecNumber(buffer, offset + 8, 2, "Width"));
    addField(new DecNumber(buffer, offset + 10, 2, "Height"));
    addField(new Bitmap(buffer, offset + 12, 2, "Has background?", hasb));
    addField(new UnsignDecNumber(buffer, offset + 14, 2, "# controls"));
    addField(new ResourceRef(buffer, offset + 16, "Background image", "MOS"));
    addField(new UnsignDecNumber(buffer, offset + 24, 2, "First control index"));
    addField(new Flag(buffer, offset + 26, 2, "Flags", s_flag));
    return offset + 28;
  }
}

