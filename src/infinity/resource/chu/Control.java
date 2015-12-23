// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.Bitmap;
import infinity.datatype.ColorPicker;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HexNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.resource.AbstractStruct;
import infinity.resource.Profile;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.OutputStream;

final class Control extends AbstractStruct // implements AddRemovable
{
  public static final String FMT_NAME = "Control %1$d";

  private static final String[] s_type = {"Button", "", "Slider", "Text field", "",
                                          "Text area", "Label", "Scroll bar"};
  private static final String[] s_button = {"Center", "Left justify", "Right justify",
                                            "Top justify", "Bottom justify", "Anchor",
                                            "Reduce size", "Don't wrap"};
  private static final String[] s_label = {"Center", "Use color", "Truecolor", "Center justify",
                                           "Left justify", "Right justify", "Top justify",
                                           "Middle justify", "Bottom justify", "Word wrap"};
  private static final String[] s_case = {"Normal case", "Upper case only", "Lower case only"};

  private final int size;

  Control(AbstractStruct superStruct, byte buffer[], int offset, int number, int size) throws Exception
  {
    super(superStruct, String.format(FMT_NAME, number), buffer, offset);
    this.size = size;
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    getField(0).write(os);
    getField(1).write(os);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public int read(byte buffer[], int offset)
  {
    addField(new HexNumber(buffer, offset, 4, "Offset"));
    addField(new HexNumber(buffer, offset + 4, 4, "Length"));
    return offset + 8;
  }

  public int getControlSize()
  {
    return size;
  }

  /** Returns the control id. */
  public int getControlId()
  {
    return ((DecNumber)getAttribute("Control ID")).getValue();
  }

  /** Returns the x and y position of the control. */
  public Point getControlPosition()
  {
    return new Point(((DecNumber)getAttribute("Position: X")).getValue(),
                     ((DecNumber)getAttribute("Position: Y")).getValue());
  }

  /** Returns the width and height of the control. */
  public Dimension getControlDimensions()
  {
    return new Dimension(((DecNumber)getAttribute("Width")).getValue(),
                         ((DecNumber)getAttribute("Height")).getValue());
  }

  /** Returns the control type. */
  public int getControlType()
  {
    return ((Bitmap)getAttribute("Type")).getValue();
  }


  public int readControl(byte buffer[])
  {
    int offset = ((HexNumber)getAttribute("Offset")).getValue();
    int endOffset = offset + getControlSize();
    addField(new DecNumber(buffer, offset, 2, "Control ID"));
    addField(new DecNumber(buffer, offset + 2, 2, "Buffer length"));
    addField(new DecNumber(buffer, offset + 4, 2, "Position: X"));
    addField(new DecNumber(buffer, offset + 6, 2, "Position: Y"));
    addField(new DecNumber(buffer, offset + 8, 2, "Width"));
    addField(new DecNumber(buffer, offset + 10, 2, "Height"));
    Bitmap type = new Bitmap(buffer, offset + 12, 1, "Type", s_type);
    addField(type);
    addField(new Unknown(buffer, offset + 13, 1));

    switch (type.getValue()) {
      case 0: // Button
        addField(new ResourceRef(buffer, offset + 14, "Button", "BAM"));
        addField(new DecNumber(buffer, offset + 22, 1, "Animation number"));
        addField(new Flag(buffer, offset + 23, 1, "Text flags", s_button));
        addField(new DecNumber(buffer, offset + 24, 1, "Frame number: Unpressed"));
        addField(new DecNumber(buffer, offset + 25, 1, "Text anchor: Left"));
        addField(new DecNumber(buffer, offset + 26, 1, "Frame number: Pressed"));
        addField(new DecNumber(buffer, offset + 27, 1, "Text anchor: Right"));
        addField(new DecNumber(buffer, offset + 28, 1, "Frame number: Selected"));
        addField(new DecNumber(buffer, offset + 29, 1, "Text anchor: Top"));
        addField(new DecNumber(buffer, offset + 30, 1, "Frame number: Disabled"));
        addField(new DecNumber(buffer, offset + 31, 1, "Text anchor: Bottom"));
        offset += 32;
        break;
      case 2: // Slider
        addField(new ResourceRef(buffer, offset + 14, "Background image", "MOS"));
        addField(new ResourceRef(buffer, offset + 22, "Slider knob", "BAM"));
        addField(new DecNumber(buffer, offset + 30, 2, "Animation number"));
        addField(new DecNumber(buffer, offset + 32, 2, "Frame number: Ungrabbed"));
        addField(new DecNumber(buffer, offset + 34, 2, "Frame number: Grabbed"));
        addField(new DecNumber(buffer, offset + 36, 2, "Knob position: X"));
        addField(new DecNumber(buffer, offset + 38, 2, "Knob position: Y"));
        addField(new DecNumber(buffer, offset + 40, 2, "Step width"));
        addField(new DecNumber(buffer, offset + 42, 2, "Step count"));
        addField(new DecNumber(buffer, offset + 44, 2, "Slider region: Top"));
        addField(new DecNumber(buffer, offset + 46, 2, "Slider region: Bottom"));
        addField(new DecNumber(buffer, offset + 48, 2, "Slider region: Left"));
        addField(new DecNumber(buffer, offset + 50, 2, "Slider region: Right"));
        offset += 52;
        break;
      case 3: // Text field
        addField(new ResourceRef(buffer, offset + 14, "Background 1", "MOS"));
        addField(new ResourceRef(buffer, offset + 22, "Background 2", "MOS"));
        addField(new ResourceRef(buffer, offset + 30, "Background 3", "MOS"));
        addField(new ResourceRef(buffer, offset + 38, "Caret", "BAM"));
        addField(new DecNumber(buffer, offset + 46, 2, "Animation number"));
        addField(new DecNumber(buffer, offset + 48, 2, "Frame number"));
        addField(new DecNumber(buffer, offset + 50, 2, "Caret position: X"));
        addField(new DecNumber(buffer, offset + 52, 2, "Caret position: Y"));
        addField(new DecNumber(buffer, offset + 54, 4, "Text field ID"));
        if (Profile.isEnhancedEdition()) {
          addField(new ResourceRef(buffer, offset + 58, "Font", new String[]{"FNT", "BAM"}));
        } else {
          addField(new ResourceRef(buffer, offset + 58, "Font", "BAM"));
        }
        addField(new Unknown(buffer, offset + 66, 2));
        addField(new TextString(buffer, offset + 68, 32, "Initial text"));
        addField(new DecNumber(buffer, offset + 100, 2, "Field length"));
        addField(new Bitmap(buffer, offset + 102, 4, "Allowed case", s_case));
        offset += 106;
        break;
      case 5: // Text area
        if (Profile.isEnhancedEdition()) {
          addField(new ResourceRef(buffer, offset + 14, "Font (main text)", new String[]{"FNT", "BAM"}));
          addField(new ResourceRef(buffer, offset + 22, "Font (initials)", new String[]{"FNT", "BAM"}));
        } else {
          addField(new ResourceRef(buffer, offset + 14, "Font (main text)", "BAM"));
          addField(new ResourceRef(buffer, offset + 22, "Font (initials)", "BAM"));
        }
        addField(new ColorPicker(buffer, offset + 30, "Color 1", ColorPicker.Format.RGBX));
        addField(new ColorPicker(buffer, offset + 34, "Color 2", ColorPicker.Format.RGBX));
        addField(new ColorPicker(buffer, offset + 38, "Color 3", ColorPicker.Format.RGBX));
        addField(new DecNumber(buffer, offset + 42, 4, "Scroll bar ID"));
        offset += 46;
        break;
      case 6: // Label
        addField(new StringRef(buffer, offset + 14, "Initial text"));
        if (Profile.isEnhancedEdition()) {
          addField(new ResourceRef(buffer, offset + 18, "Font", new String[]{"FNT", "BAM"}));
        } else {
          addField(new ResourceRef(buffer, offset + 18, "Font", "BAM"));
        }
        addField(new ColorPicker(buffer, offset + 26, "Color 1", ColorPicker.Format.RGBX));
        addField(new ColorPicker(buffer, offset + 30, "Color 2", ColorPicker.Format.RGBX));
        addField(new Flag(buffer, offset + 34, 2, "Text flags", s_label));
        offset += 36;
        break;
      case 7: // Scroll bar
        addField(new ResourceRef(buffer, offset + 14, "Graphics", "BAM"));
        addField(new DecNumber(buffer, offset + 22, 2, "Animation number"));
        addField(new DecNumber(buffer, offset + 24, 2, "Frame number: Up-arrow, unpressed"));
        addField(new DecNumber(buffer, offset + 26, 2, "Frame number: Up-arrow, pressed"));
        addField(new DecNumber(buffer, offset + 28, 2, "Frame number: Down-arrow, unpressed"));
        addField(new DecNumber(buffer, offset + 30, 2, "Frame number: Down-arrow, pressed"));
        addField(new DecNumber(buffer, offset + 32, 2, "Frame number: Trough"));
        addField(new DecNumber(buffer, offset + 34, 2, "Frame number: Slider"));
        addField(new DecNumber(buffer, offset + 36, 4, "Text area ID"));
        offset += 40;
        break;
      default:
        HexNumber len = (HexNumber)getAttribute("Length");
        addField(new Unknown(buffer, offset + 14, len.getValue() - 14));
        offset += len.getValue();
        break;
    }

    // handling optional gap between controls
    if (offset < endOffset) {
      addField(new Unknown(buffer, offset, endOffset - offset, "Unused"));
      offset = endOffset;
    }

    return offset;
  }

  public void writeControl(OutputStream os) throws IOException
  {
    for (int i = 2; i < getFieldCount(); i++)
      getField(i).write(os);
  }
}

