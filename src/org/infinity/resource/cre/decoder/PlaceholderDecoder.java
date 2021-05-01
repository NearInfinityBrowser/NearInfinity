// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.infinity.resource.Profile;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;
import org.infinity.util.io.StreamUtils;

/**
 * General-purpose creature animation decoder for handling non-existing or unknown animation types.
 * Available ranges: [0000,ffff]
 */
public class PlaceholderDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.PLACEHOLDER;

  /** ResourceEntry for the placeholder animation BAM file. */
  private static final ResourceEntry BAM_PLACEHOLDER = loadPlaceholderBam();

  /** Loads the placeholder animation into memory as a virtual resource entry object. */
  private static ResourceEntry loadPlaceholderBam()
  {
    ResourceEntry retVal = null;
    // REMEMBER: BAM file path is relative to PlaceholderDecoder class path
    try (InputStream is = PlaceholderDecoder.class.getResourceAsStream("placeholder.bam")) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int nRead;
      byte[] buf = new byte[1024];
      while ((nRead = is.read(buf, 0, buf.length)) != -1) {
        baos.write(buf, 0, nRead);
      }
      baos.flush();
      retVal = new BufferedResourceEntry(StreamUtils.getByteBuffer(baos.toByteArray()), "placeholder.bam");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return retVal;
  }

  public PlaceholderDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public PlaceholderDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    String resref = getAnimationResref();
    ArrayList<String> retVal = new ArrayList<>();
    retVal.add(resref + ".BAM");
    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected IniMapSection getSpecificIniSection()
  {
    IniMapSection retVal = null;
    IniMap ini = getAnimationInfo();
    if (ini != null) {
      for (final Iterator<IniMapSection> iter = ini.iterator(); iter.hasNext(); ) {
        IniMapSection section = iter.next();
        switch (section.getName().toLowerCase()) {
          case "general":
          case "sounds":
            break;
          default:
            retVal = section;
        }
        if (retVal != null) {
          break;
        }
      }
    }

    if (retVal == null) {
      retVal = new IniMapSection(getAnimationSectionName(), 0, null);
    }

    return retVal;
  }

  @Override
  protected void init() throws Exception
  {
    // setting properties
    initDefaults(getAnimationInfo());

    // autodetecting animation attributes
    IniMapSection section = getSpecificIniSection();
    setAttribute(KEY_ANIMATION_SECTION, section.getName()); // override with animation-specific name
    for (final Iterator<IniMapEntry> iter = section.iterator(); iter.hasNext(); ) {
      IniMapEntry entry = iter.next();
      String key = entry.getKey();
      String value = entry.getValue();
      try {
        int n = Integer.parseInt(value);
        if (n == 0 || n == 1) {
          setAttribute(new DecoderAttribute(key, DecoderAttribute.DataType.BOOLEAN), Boolean.valueOf(n != 0));
        } else {
          setAttribute(new DecoderAttribute(key, DecoderAttribute.DataType.DECIMAL), Integer.valueOf(n));
        }
      } catch (NumberFormatException e) {
        setAttribute(new DecoderAttribute(key, DecoderAttribute.DataType.STRING), value);
      }
    }
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;

    switch (Profile.getGame()) {
      case PST:
      case PSTEE:
        if (seq == Sequence.PST_STAND) {
          retVal = SeqDef.createSequence(seq, new Direction[] {Direction.S}, false,
                                         BAM_PLACEHOLDER, 0, SegmentDef.SpriteType.AVATAR);
        }
        break;
      default:
        if (seq == Sequence.STAND) {
          retVal = SeqDef.createSequence(seq, new Direction[] {Direction.S}, false,
                                         BAM_PLACEHOLDER, 0, SegmentDef.SpriteType.AVATAR);
        }
    }
    return retVal;
  }
}
