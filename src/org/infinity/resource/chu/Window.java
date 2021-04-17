// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.chu;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.StreamUtils;

final class Window extends AbstractStruct // implements AddRemovable
{
  // CHU/Window-specific field labels
  public static final String CHU_WINDOW_PANEL               = "Panel";
  public static final String CHU_WINDOW_NAME                = "Name";
  public static final String CHU_WINDOW_ID                  = "Panel ID";
  public static final String CHU_WINDOW_POSITION_X          = "Position: X";
  public static final String CHU_WINDOW_POSITION_Y          = "Position: Y";
  public static final String CHU_WINDOW_WIDTH               = "Width";
  public static final String CHU_WINDOW_HEIGHT              = "Height";
  public static final String CHU_WINDOW_HAS_BACKGROUND      = "Has background?";
  public static final String CHU_WINDOW_NUM_CONTROLS        = "# controls";
  public static final String CHU_WINDOW_BACKGROUND          = "Background image";
  public static final String CHU_WINDOW_FIRST_CONTROL_INDEX = "First control index";
  public static final String CHU_WINDOW_FLAGS               = "Flags";

  private static final String s_flag[] = {"No flags set", "Don't dim background"};

  Window() throws Exception
  {
    super(null, CHU_WINDOW_PANEL, StreamUtils.getByteBuffer(36), 0);
  }

  Window(ChuResource chu, ByteBuffer buffer, int offset, int nr) throws Exception
  {
    super(chu, CHU_WINDOW_PANEL + " " + nr, buffer, offset);
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    Collections.sort(getFields());
    for (final StructEntry entry : getFields()) {
      if (entry instanceof Control) {
        break;
      }
      entry.write(os);
    }
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public ChuResource getParent()
  {
    return (ChuResource)super.getParent();
  }

  /** Returns the number of controls associated with this panel. */
  public int getControlCount()
  {
    return ((UnsignDecNumber)getAttribute(CHU_WINDOW_NUM_CONTROLS)).getValue();
  }

  /** Returns the given control. Index is relative to the controls associated with this panel. */
  public Control getControl(int index)
  {
    if (index >= 0 && index < getControlCount()) {
      return (Control)getAttribute(Control.CHU_CONTROL + " " + index);
    } else {
      return null;
    }
  }

  /** Returns the panel id. */
  public int getWindowId()
  {
    return ((IsNumeric)getAttribute(CHU_WINDOW_ID)).getValue();
  }

  /** Returns the x and y positions of the panel. */
  public Point getWindowPosition()
  {
    return new Point(((IsNumeric)getAttribute(CHU_WINDOW_POSITION_X)).getValue(),
                     ((IsNumeric)getAttribute(CHU_WINDOW_POSITION_Y)).getValue());
  }

  /** Returns width and height of the panel. */
  public Dimension getWindowDimension()
  {
    return new Dimension(((IsNumeric)getAttribute(CHU_WINDOW_WIDTH)).getValue(),
                         ((IsNumeric)getAttribute(CHU_WINDOW_HEIGHT)).getValue());
  }

  /** Returns whether the panel references a background MOS. */
  public boolean hasBackgroundImage()
  {
    return ((IsNumeric)getAttribute(CHU_WINDOW_HAS_BACKGROUND)).getValue() == 1;
  }

  /** Returns the background MOS for the panel. */
  public String getBackgroundImage()
  {
    return ((ResourceRef)getAttribute(CHU_WINDOW_BACKGROUND)).getResourceName();
  }

  public int readControls(ByteBuffer buffer) throws Exception
  {
    int numctrl = ((UnsignDecNumber)getAttribute(CHU_WINDOW_NUM_CONTROLS)).getValue();
    int first = ((UnsignDecNumber)getAttribute(CHU_WINDOW_FIRST_CONTROL_INDEX)).getValue();
    int controlsoffset = getParent().getControlsOffset() + (first*8);
    int endoffset = controlsoffset;
    for (int i = 0; i < numctrl; i++) {
      int size = getParent().getControlOffset(first+i+1) - getParent().getControlOffset(first+i);
      Control control = new Control(this, buffer, controlsoffset, i, size);
      controlsoffset = control.getEndOffset();
      endoffset = control.readControl(buffer);
      addField(control);
    }
    return endoffset;
  }

  public void writeControls(OutputStream os) throws IOException
  {
    for (final StructEntry o : getFields()) {
      if (o instanceof Control) {
        ((Control)o).writeControl(os);
      }
    }
  }

  public void writeControlsTable(OutputStream os) throws IOException
  {
    for (final StructEntry o : getFields()) {
      if (o instanceof Control) {
        ((Control)o).write(os);
      }
    }
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    if (getParent().getPanelSize() == 36) {
      addField(new TextString(buffer, offset, 8, CHU_WINDOW_NAME), 0);
      offset += 8;
    }
    addField(new DecNumber(buffer, offset, 2, CHU_WINDOW_ID));
    addField(new Unknown(buffer, offset + 2, 2));
    addField(new DecNumber(buffer, offset + 4, 2, CHU_WINDOW_POSITION_X));
    addField(new DecNumber(buffer, offset + 6, 2, CHU_WINDOW_POSITION_Y));
    addField(new DecNumber(buffer, offset + 8, 2, CHU_WINDOW_WIDTH));
    addField(new DecNumber(buffer, offset + 10, 2, CHU_WINDOW_HEIGHT));
    addField(new Bitmap(buffer, offset + 12, 2, CHU_WINDOW_HAS_BACKGROUND, OPTION_NOYES));
    addField(new UnsignDecNumber(buffer, offset + 14, 2, CHU_WINDOW_NUM_CONTROLS));
    addField(new ResourceRef(buffer, offset + 16, CHU_WINDOW_BACKGROUND, "MOS"));
    addField(new UnsignDecNumber(buffer, offset + 24, 2, CHU_WINDOW_FIRST_CONTROL_INDEX));
    addField(new Flag(buffer, offset + 26, 2, CHU_WINDOW_FLAGS, s_flag));
    return offset + 28;
  }
}
