// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;
import infinity.util.LongIntegerHashMap;

public final class ProResource extends AbstractStruct implements Resource
{
  private static final String[] s_color = {"", "Black", "Blue", "Chromatic", "Gold",
                                           "Green", "Purple", "Red", "White", "Ice",
                                           "Stone", "Magenta", "Orange"};
  private static final LongIntegerHashMap<String> s_proj = new LongIntegerHashMap<String>();
//  private static final String[] s_proj = {"Fireball", "Stinking cloud", "Cloudkill", "Ice storm",
//                                          "Grease", "Web", "Meteor", "Abi-Dalzim's Horrid Wilting",
//                                          "Teleport field", "Entangle", "Color spray",
//                                          "Cone of cold", "Holy smite", "Unholy blight",
//                                          "Prismatic spray", "Big flame", "Storm of vengence",
//                                          "Purple fireball", "Green dragon blast"};
  private static final String[] s_areaflags = {
    "Trap not visible", "Trap visible", "Triggered by inanimates", "Triggered by condition",
    "No overlapping effects", "Secondary projectile", "Fragments", "Not affecting allies",
    "Not affecting enemies"};
  private static final String[] s_flags = {
    "No flags set", "Colored BAM", "Creates smoke", "", "Darken", "", "Casts shadow",
    "Light spot enabled", "Translucent"
  };
  private static final String[] s_behave = {"No flags set", "Show sparks", "3D traveling",
                                            "Loop sound 1", "Loop sound 2", "Ignore center"};
  private static final String[] s_areatype = {"Round", "", "", "Draw animation", "Cone-shaped"};

  static {
    s_proj.put(0L, "Fireball");
    s_proj.put(1L, "Stinking cloud");
    s_proj.put(2L, "Cloudkill");
    s_proj.put(3L, "Ice storm");
    s_proj.put(4L, "Grease");
    s_proj.put(5L, "Web");
    s_proj.put(6L, "Meteor");
    s_proj.put(7L, "Horrid wilting");
    s_proj.put(8L, "Teleport field");
    s_proj.put(9L, "Entangle");
    s_proj.put(10L, "Color spray");
    s_proj.put(11L, "Cone of cold");
    s_proj.put(12L, "Holy smite");
    s_proj.put(13L, "Unholy blight");
    s_proj.put(14L, "Prismatic spray");
    s_proj.put(15L, "Red dragon blast");
    s_proj.put(16L, "Storm of vengeance");
    s_proj.put(17L, "Purple fireball");
    s_proj.put(18L, "Green dragon blast");
    s_proj.put(255L, "None");
  }

  public ProResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    HexNumber projtype = new HexNumber(buffer, offset + 8, 2, "Type");
    list.add(projtype);
    list.add(new DecNumber(buffer, offset + 10, 2, "Speed"));
    list.add(new Flag(buffer, offset + 12, 4, "Behavior", s_behave));
//    list.add(new Unknown(buffer, offset + 14, 2));
    list.add(new ResourceRef(buffer, offset + 16, "Sound 1", "WAV"));
    list.add(new ResourceRef(buffer, offset + 24, "Sound 2", "WAV"));
    list.add(new ResourceRef(buffer, offset + 32, "Sound 3", "WAV"));
    list.add(new Bitmap(buffer, offset + 40, 4, "Particle color", s_color));
    list.add(new Unknown(buffer, offset + 44, 212));
    list.add(new Flag(buffer, offset + 256, 4, "Flags", s_flags));
    list.add(new ResourceRef(buffer, offset + 260, "Projectile animation", "BAM"));
    list.add(new ResourceRef(buffer, offset + 268, "Shadow animation", "BAM"));
    list.add(new DecNumber(buffer, offset + 276, 1, "Projectile animation number"));
    list.add(new DecNumber(buffer, offset + 277, 1, "Shadow animation number"));
    list.add(new DecNumber(buffer, offset + 278, 2, "Light spot intensity"));
    list.add(new DecNumber(buffer, offset + 280, 2, "Light spot width"));
    list.add(new DecNumber(buffer, offset + 282, 2, "Light spot height"));
    list.add(new ResourceRef(buffer, offset + 284, "Palette", "BMP"));
    list.add(new ColorValue(buffer, offset + 292, 1, "Projectile color 1"));
    list.add(new ColorValue(buffer, offset + 293, 1, "Projectile color 2"));
    list.add(new ColorValue(buffer, offset + 294, 1, "Projectile color 3"));
    list.add(new ColorValue(buffer, offset + 295, 1, "Projectile color 4"));
    list.add(new ColorValue(buffer, offset + 296, 1, "Projectile color 5"));
    list.add(new ColorValue(buffer, offset + 297, 1, "Projectile color 6"));
    list.add(new ColorValue(buffer, offset + 298, 1, "Projectile color 7"));
    list.add(new DecNumber(buffer, offset + 299, 1, "Smoke puff delay"));
    list.add(new ColorValue(buffer, offset + 300, 1, "Smoke color 1"));
    list.add(new ColorValue(buffer, offset + 301, 1, "Smoke color 2"));
    list.add(new ColorValue(buffer, offset + 302, 1, "Smoke color 3"));
    list.add(new ColorValue(buffer, offset + 303, 1, "Smoke color 4"));
    list.add(new ColorValue(buffer, offset + 304, 1, "Smoke color 5"));
    list.add(new ColorValue(buffer, offset + 305, 1, "Smoke color 6"));
    list.add(new ColorValue(buffer, offset + 306, 1, "Smoke color 7"));
    list.add(new HexNumber(buffer, offset + 307, 1, "Face target?"));
//            new Flag(buffer, offset + 307, 1, "Aim to target",
//                     new String[]{"No flags set", "", "", "Face target"}));
    list.add(new IdsBitmap(buffer, offset + 308, 2, "Smoke animation", "ANIMATE.IDS"));
    list.add(new ResourceRef(buffer, offset + 310, "Trailing animation 1", "BAM"));
    list.add(new ResourceRef(buffer, offset + 318, "Trailing animation 2", "BAM"));
    list.add(new ResourceRef(buffer, offset + 326, "Trailing animation 3", "BAM"));
    list.add(new DecNumber(buffer, offset + 334, 2, "Tailing animation number 1"));
    list.add(new DecNumber(buffer, offset + 336, 2, "Tailing animation number 2"));
    list.add(new DecNumber(buffer, offset + 338, 2, "Tailing animation number 3"));
    list.add(new Unknown(buffer, offset + 340, 172));
    if (projtype.getValue() != 3)
      return offset + 512;
    // Area of effect
    list.add(new Flag(buffer, offset + 512, 1, "Area of effect target", s_areaflags));
    list.add(new Flag(buffer, offset + 513, 1, "Area of effect type", s_areatype));
    list.add(new Unknown(buffer, offset + 514, 2));
    list.add(new DecNumber(buffer, offset + 516, 2, "Trap size"));
    list.add(new DecNumber(buffer, offset + 518, 2, "Explosion size"));
    list.add(new ResourceRef(buffer, offset + 520, "Explosion sound", "WAV"));
    list.add(new DecNumber(buffer, offset + 528, 2, "Explosion frequency (frames)"));
    list.add(new IdsBitmap(buffer, offset + 530, 2, "Fragment animation", "ANIMATE.IDS"));
    list.add(new ProRef(buffer, offset + 532, "Secondary projectile"));
    list.add(new DecNumber(buffer, offset + 534, 1, "Duration"));
    list.add(new HashBitmap(buffer, offset + 535, 1, "Explosion effect", s_proj));
    list.add(new ColorValue(buffer, offset + 536, 2, "Explosion color"));
//    list.add(new Unknown(buffer, offset + 537, 1));
    list.add(new ProRef(buffer, offset + 538, "Explosion projectile"));
    list.add(new ResourceRef(buffer, offset + 540, "Explosion animation", "VVC"));
    list.add(new DecNumber(buffer, offset + 548, 2, "Cone width"));
    list.add(new Unknown(buffer, offset + 550, 218));
    return offset + 768;
  }
}

