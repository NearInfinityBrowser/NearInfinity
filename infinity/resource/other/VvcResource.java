// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class VvcResource extends AbstractStruct implements Resource
{
  private static final String s_transparency[] = {"No flags set", "", "Transparent", "", "Translucent"};
  private static final String s_tint[] = {"No flags set", "", "Blend", "", "Grayscale", "", "Brighten",
                                          "", "", "", "Sepia"};
  private static final String s_seq[] = {"No flags set", "Looping", "", "", "Draw animation", "", "",
                                         "Not covered by wall"};

  public VvcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new ResourceRef(buffer, offset + 8, "Animation", "BAM"));
    list.add(new Unknown(buffer, offset + 16, 8));
    list.add(new Flag(buffer, offset + 24, 2, "Drawing", s_transparency));
    list.add(new Flag(buffer, offset + 26, 2, "Color adjustment", s_tint));
    list.add(new Unknown(buffer, offset + 28, 4));
    list.add(new Flag(buffer, offset + 32, 4, "Sequencing", s_seq));
    list.add(new Unknown(buffer, offset + 36, 4));
    list.add(new DecNumber(buffer, offset + 40, 4, "Position: X"));
    list.add(new DecNumber(buffer, offset + 44, 4, "Position: Y"));
    list.add(new Unknown(buffer, offset + 48, 4));
    list.add(new DecNumber(buffer, offset + 52, 4, "Frame rate"));
    list.add(new HexNumber(buffer, offset + 56, 4, "Face target?"));
    list.add(new Unknown(buffer, offset + 60, 4));
    list.add(new HexNumber(buffer, offset + 64, 4, "Positioning"));
    list.add(new Unknown(buffer, offset + 68, 4));
    list.add(new Unknown(buffer, offset + 72, 4));
    list.add(new DecNumber(buffer, offset + 76, 4, "Position: Z"));
    list.add(new Unknown(buffer, offset + 80, 4));
    list.add(new Unknown(buffer, offset + 84, 4));
    list.add(new Unknown(buffer, offset + 88, 4));
    list.add(new DecNumber(buffer, offset + 92, 4, "Duration (frames)"));
    list.add(new Unknown(buffer, offset + 96, 4));
    list.add(new Unknown(buffer, offset + 100, 4));
    list.add(new DecNumber(buffer, offset + 104, 4, "First BAM animation number"));
    list.add(new DecNumber(buffer, offset + 108, 4, "Second BAM animation number"));
    list.add(new Unknown(buffer, offset + 112, 4));
    list.add(new Unknown(buffer, offset + 116, 4));
    list.add(new ResourceRef(buffer, offset + 120, "Starting sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 128, "Duration sound", "WAV"));
    list.add(new Unknown(buffer, offset + 136, 4));
    list.add(new Unknown(buffer, offset + 140, 4));
    list.add(new DecNumber(buffer, offset + 144, 4, "Third BAM animation number"));
    list.add(new ResourceRef(buffer, offset + 148, "Ending sound", "WAV"));
    list.add(new Unknown(buffer, offset + 156, 336));
    return offset + 492;
  }
}

