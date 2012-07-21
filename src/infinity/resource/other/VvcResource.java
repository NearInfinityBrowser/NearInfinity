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
  private static final String s_transparency[] = {"No flags set", "Transparent", "Translucent", "Translucent shadow", "Blended",
                                                  "Mirror X axis", "Mirror Y axis", "Clipped", "Copy from back", "Clear fill",
                                                  "3D blend", "Not covered by wall", "Persist through time stop", "Ignore dream palette",
                                                  "2D blend"};
  private static final String s_tint[] = {"No flags set", "Not light source", "Light source", "Internal brightness", "Time stopped", "",
                                          "Internal gamma", "Non-reserved palette", "Full palette", "", "Dream palette"};
  private static final String s_seq[] = {"No flags set", "Looping", "Special lighting", "Modify for height", "Draw animation", "Custom palette",
                                         "Purgeable", "Not covered by wall", "Mid-level brighten", "High-level brighten"};
  private static final String s_face[] = {"Use current", "Face target", "Follow target", "Follow path", "Lock orientation"};
  private static final String s_noyes[] = {"No", "Yes"};

  public VvcResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new ResourceRef(buffer, offset + 8, "Animation", "BAM"));
    list.add(new ResourceRef(buffer, offset + 16, "Shadow", "BAM"));
    list.add(new Flag(buffer, offset + 24, 2, "Drawing", s_transparency));
    list.add(new Flag(buffer, offset + 26, 2, "Color adjustment", s_tint));
    list.add(new Unknown(buffer, offset + 28, 4));
    list.add(new Flag(buffer, offset + 32, 4, "Sequencing", s_seq));
    list.add(new Unknown(buffer, offset + 36, 4));
    list.add(new DecNumber(buffer, offset + 40, 4, "Position: X"));
    list.add(new DecNumber(buffer, offset + 44, 4, "Position: Y"));
    list.add(new Bitmap(buffer, offset + 48, 4, "Draw oriented", s_noyes));
    list.add(new DecNumber(buffer, offset + 52, 4, "Frame rate"));
    list.add(new DecNumber(buffer, offset + 56, 4, "# orientations"));
    list.add(new DecNumber(buffer, offset + 60, 4, "Primary orientation"));
    list.add(new Flag(buffer, offset + 64, 4, "Travel orientation", s_face));
    list.add(new ResourceRef(buffer, offset + 68, "Palette", "BMP"));
//    list.add(new Unknown(buffer, offset + 72, 4));
    list.add(new DecNumber(buffer, offset + 76, 4, "Position: Z"));
    list.add(new DecNumber(buffer, offset + 80, 4, "Light spot width"));
    list.add(new DecNumber(buffer, offset + 84, 4, "Light spot height"));
    list.add(new DecNumber(buffer, offset + 88, 4, "Light spot brightness"));
    list.add(new DecNumber(buffer, offset + 92, 4, "Duration (frames)"));
    list.add(new ResourceRef(buffer, offset + 96, "Resource", "VVC"));
//    list.add(new Unknown(buffer, offset + 100, 4));
    list.add(new DecNumber(buffer, offset + 104, 4, "First animation number"));
    list.add(new DecNumber(buffer, offset + 108, 4, "Second animation number"));
    list.add(new DecNumber(buffer, offset + 112, 4, "Current animation number"));
    list.add(new Bitmap(buffer, offset + 116, 4, "Continuous playback", s_noyes));
    list.add(new ResourceRef(buffer, offset + 120, "Starting sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 128, "Duration sound", "WAV"));
    list.add(new ResourceRef(buffer, offset + 136, "Alpha mask", "BAM"));
//    list.add(new Unknown(buffer, offset + 136, 4));
    list.add(new DecNumber(buffer, offset + 144, 4, "Third animation number"));
    list.add(new ResourceRef(buffer, offset + 148, "Ending sound", "WAV"));
    list.add(new Unknown(buffer, offset + 156, 336));
    return offset + 492;
  }
}

