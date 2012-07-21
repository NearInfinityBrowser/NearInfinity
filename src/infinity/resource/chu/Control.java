// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.chu;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;

import java.io.IOException;
import java.io.OutputStream;

final class Control extends AbstractStruct // implements AddRemovable
{
  private static final String s_type[] = {"Button", "", "Slider", "Text field", "", "Text area",
                                          "Label", "Scroll bar"};
  private static final String s_button[] = {"Center", "Left justify", "Right justify",
                                            "Top justify", "Bottom justify", "Anchor",
                                            "Reduce size", "Don't wrap"};
  private static final String s_label[] = {"Center", "RGB color", "", "", "Left justify",
                                           "Right justify"};
  private static final String s_case[] = {"Normal case", "Upper case only", "Lower case only"};

  Control(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Control", buffer, offset);
  }

// --------------------- Begin Interface StructEntry ---------------------

  public String getName()
  {
    return "Control";
  }

// --------------------- End Interface StructEntry ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    getStructEntryAt(0).write(os);
    getStructEntryAt(1).write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public int read(byte buffer[], int offset)
  {
    list.add(new HexNumber(buffer, offset, 4, "Offset"));
    list.add(new HexNumber(buffer, offset + 4, 4, "Length"));
    return offset + 8;
  }

  public int readControl(byte buffer[])
  {
    int offset = ((HexNumber)getAttribute("Offset")).getValue();
    list.add(new DecNumber(buffer, offset, 2, "Control ID"));
    list.add(new DecNumber(buffer, offset + 2, 2, "Buffer length"));
    list.add(new DecNumber(buffer, offset + 4, 2, "Position: X"));
    list.add(new DecNumber(buffer, offset + 6, 2, "Position: Y"));
    list.add(new DecNumber(buffer, offset + 8, 2, "Width"));
    list.add(new DecNumber(buffer, offset + 10, 2, "Height"));
    Bitmap type = new Bitmap(buffer, offset + 12, 1, "Type", s_type);
    list.add(type);
    list.add(new Unknown(buffer, offset + 13, 1));

    switch (type.getValue()) {
      case 0:
        list.add(new ResourceRef(buffer, offset + 14, "Button", "BAM"));
        list.add(new UnsignDecNumber(buffer, offset + 22, 1, "Animation number"));
        list.add(new Flag(buffer, offset + 23, 1, "Text flags", s_button));
        list.add(new DecNumber(buffer, offset + 24, 1, "Frame number: Unpressed"));
        list.add(new DecNumber(buffer, offset + 25, 1, "Text anchor: Left"));
        list.add(new DecNumber(buffer, offset + 26, 1, "Frame number: Pressed"));
        list.add(new DecNumber(buffer, offset + 27, 1, "Text anchor: Right"));
        list.add(new DecNumber(buffer, offset + 28, 1, "Frame number: Selected"));
        list.add(new DecNumber(buffer, offset + 29, 1, "Text anchor: Top"));
        list.add(new DecNumber(buffer, offset + 30, 1, "Frame number: Disabled"));
        list.add(new DecNumber(buffer, offset + 31, 1, "Text anchor: Bottom"));
        offset += 32;
        break;
      case 2:
        list.add(new ResourceRef(buffer, offset + 14, "Background image", "MOS"));
        list.add(new ResourceRef(buffer, offset + 22, "Slider knob", "BAM"));
        list.add(new DecNumber(buffer, offset + 30, 2, "Animation number"));
        list.add(new DecNumber(buffer, offset + 32, 2, "Frame number: Ungrabbed"));
        list.add(new DecNumber(buffer, offset + 34, 2, "Frame number: Grabbed"));
        list.add(new DecNumber(buffer, offset + 36, 2, "Knob position: X"));
        list.add(new DecNumber(buffer, offset + 38, 2, "Knob position: Y"));
        list.add(new DecNumber(buffer, offset + 40, 2, "Step width"));
        list.add(new DecNumber(buffer, offset + 42, 2, "Step count"));
        list.add(new DecNumber(buffer, offset + 44, 2, "Slider region: Top"));
        list.add(new DecNumber(buffer, offset + 46, 2, "Slider region: Bottom"));
        list.add(new DecNumber(buffer, offset + 48, 2, "Slider region: Left"));
        list.add(new DecNumber(buffer, offset + 50, 2, "Slider region: Right"));
        offset += 52;
        break;
      case 3:
        list.add(new ResourceRef(buffer, offset + 14, "Background 1", "MOS"));
        list.add(new ResourceRef(buffer, offset + 22, "Background 2", "MOS"));
        list.add(new ResourceRef(buffer, offset + 30, "Background 3", "MOS"));
        list.add(new ResourceRef(buffer, offset + 38, "Caret", "BAM"));
        list.add(new DecNumber(buffer, offset + 46, 2, "Animation number"));
        list.add(new DecNumber(buffer, offset + 48, 2, "Frame number"));
        list.add(new DecNumber(buffer, offset + 50, 2, "Caret position: X"));
        list.add(new DecNumber(buffer, offset + 52, 2, "Caret position: Y"));
        list.add(new DecNumber(buffer, offset + 54, 4, "Text field ID"));
        list.add(new ResourceRef(buffer, offset + 58, "Font", "BAM"));
        list.add(new Unknown(buffer, offset + 66, 2));
        list.add(new TextString(buffer, offset + 68, 32, "Initial text"));
        list.add(new DecNumber(buffer, offset + 100, 2, "Field length"));
        list.add(new Bitmap(buffer, offset + 102, 4, "Allowed case", s_case));
        offset += 106;
        break;
      case 5:
        list.add(new ResourceRef(buffer, offset + 14, "Font 1", "BAM"));
        list.add(new ResourceRef(buffer, offset + 22, "Font 2", "BAM"));
        list.add(new Unknown(buffer, offset + 30, 4, "Color 1"));
        list.add(new Unknown(buffer, offset + 34, 4, "Color 2"));
        list.add(new Unknown(buffer, offset + 38, 4, "Color 3"));
        list.add(new DecNumber(buffer, offset + 42, 4, "Scroll bar ID"));
        offset += 46;
        break;
      case 6:
        list.add(new StringRef(buffer, offset + 14, "Initial text"));
        list.add(new ResourceRef(buffer, offset + 18, "Font", "BAM"));
        list.add(new Unknown(buffer, offset + 26, 4, "Color 1"));
        list.add(new Unknown(buffer, offset + 30, 4, "Color 2"));
        list.add(new Flag(buffer, offset + 34, 2, "Text flags", s_label));
        offset += 36;
        break;
      case 7:
        list.add(new ResourceRef(buffer, offset + 14, "Graphics", "BAM"));
        list.add(new DecNumber(buffer, offset + 22, 2, "Animation number"));
        list.add(new DecNumber(buffer, offset + 24, 2, "Frame number: Up-arrow, unpressed"));
        list.add(new DecNumber(buffer, offset + 26, 2, "Frame number: Up-arrow, pressed"));
        list.add(new DecNumber(buffer, offset + 28, 2, "Frame number: Down-arrow, unpressed"));
        list.add(new DecNumber(buffer, offset + 30, 2, "Frame number: Down-arrow, pressed"));
        list.add(new DecNumber(buffer, offset + 32, 2, "Frame number: Trough"));
        list.add(new DecNumber(buffer, offset + 34, 2, "Frame number: Slider"));
        list.add(new DecNumber(buffer, offset + 36, 4, "Text area ID"));
        offset += 40;
        break;
      default:
        HexNumber len = (HexNumber)getAttribute("Length");
        list.add(new Unknown(buffer, offset + 14, len.getValue() - 14));
        offset += len.getValue();
        break;
    }
    return offset;
  }

  public void writeControl(OutputStream os) throws IOException
  {
    for (int i = 2; i < getRowCount(); i++)
      getStructEntryAt(i).write(os);
  }
}

