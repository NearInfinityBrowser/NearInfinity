// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn;

import infinity.datatype.*;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import java.io.IOException;
import java.io.OutputStream;

public final class SsfResource extends AbstractStruct implements Resource
{
  public SsfResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeFlatList(os);
  }

// --------------------- End Interface Writeable ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "FileType"));
    TextString fileVersion = new TextString(buffer, offset + 4, 4, "FileVersion");
    list.add(fileVersion);
    SectionCount entryCount = new SectionCount(buffer, offset + 8, 4, "EntryCount", null);
    list.add(entryCount);

    if (fileVersion.toString().equalsIgnoreCase("V1.0")) {
      SectionOffset tableOffset = new SectionOffset(buffer, offset + 12, "TableOffset", null);
      list.add(tableOffset);
      list.add(new Unknown(buffer, offset + 16, 24, "Padding"));
      offset = tableOffset.getValue();
      for (int i = 0; i < entryCount.getValue(); i++)
        list.add(new SsfEntry(this, i, buffer, offset + 4 * i));
      return offset + entryCount.getValue() * (4 + 24);
    }
    else if (fileVersion.toString().equalsIgnoreCase("V1.1")) {
      for (int i = 0; i < entryCount.getValue(); i++)
        for (int j = 0; j < 3; j++)
          list.add(new StringRef(buffer, 12 + i * 12 + j * 4, "StringRef " + i + ',' + j));
      list.add(new Unknown(buffer, 12 + entryCount.getValue() * 3 * 4,
                           buffer.length - 12 - entryCount.getValue() * 3 * 4));
      return buffer.length;
    }
    else
      throw new Exception("Version not supported " + fileVersion);
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class SsfEntry extends AbstractStruct
  {
    private SsfEntry(SsfResource ssf, int num, byte buffer[], int offset) throws Exception
    {
      super(ssf, "Entry " + num, buffer, offset);
    }

    protected int read(byte buffer[], int startoffset) throws Exception
    {
      HexNumber offset = new HexNumber(buffer, startoffset, 4, "Entry offset");
      list.add(offset);
      list.add(new ResourceRef(buffer, offset.getValue(), 16, "ResRef", "WAV"));
      list.add(new StringRef(buffer, offset.getValue() + 16, "StringRef"));
      return startoffset + 4;
    }
  }
}

