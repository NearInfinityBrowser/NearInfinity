// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sav;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import infinity.resource.Writeable;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.io.FileWriterNI;

/**
 * Specialized ResourceEntry class for compressed entries in SAV resources.
 */
public class SavResourceEntry extends ResourceEntry implements Writeable
{
  private final String filename;
  private int offset;
  private int comprLength;
  private int uncomprLength;
  private byte cdata[];

  public SavResourceEntry(byte buffer[], int offset)
  {
    this.offset = offset;
    int fileNameLength = DynamicArray.getInt(buffer, offset);
    filename = new String(buffer, offset + 4, fileNameLength - 1);
    offset += 4 + fileNameLength;
    uncomprLength = DynamicArray.getInt(buffer, offset);
    comprLength = DynamicArray.getInt(buffer, offset + 4);
    cdata = Arrays.copyOfRange(buffer, offset + 8, offset + 8 + comprLength);
  }

  public SavResourceEntry(ResourceEntry entry) throws Exception
  {
    comprLength = 0;
    uncomprLength = 0;
    filename = entry.toString();
    byte udata[] = entry.getResourceData(true);
    if (udata.length == 0) {
      uncomprLength = 0;
      comprLength = 8;;
      cdata = new byte[]{0x78, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01};
    }
    else {
      cdata = new byte[udata.length * 2];
      uncomprLength = udata.length;
      Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
      deflater.setInput(udata);
      deflater.finish();
      int clength = deflater.deflate(cdata);
      cdata = Arrays.copyOfRange(cdata, 0, clength);
      comprLength = clength;
    }
  }

  public int getEndOffset()
  {
    return offset + filename.length() + 13 + cdata.length;
  }

  @Override
  public String toString()
  {
    return filename;
  }

  @Override
  public String getResourceName()
  {
    return filename;
  }

  @Override
  public String getExtension()
  {
    return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH);
  }

  @Override
  public String getTreeFolder()
  {
    return null;
  }

  @Override
  public boolean hasOverride()
  {
    return false;
  }

  @Override
  public int[] getResourceInfo(boolean ignoreoverride) throws Exception
  {
    if (filename.toUpperCase(Locale.ENGLISH).endsWith(".TIS")) {
      try {
        byte data[] = decompress();
        if (!new String(data, 0, 4).equalsIgnoreCase("TIS ")) {
          int tilesize = 64 * 64 + 4 * 256;
          int tilecount = uncomprLength / tilesize;
          return new int[]{tilecount, tilesize};
        }
        return new int[]{DynamicArray.getInt(data, 8), DynamicArray.getInt(data, 12)};
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
    return new int[]{uncomprLength};
  }

  @Override
  public byte[] getResourceData(boolean ignoreoverride) throws Exception
  {
    return decompress();
  }

  @Override
  public InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
  {
    return new BufferedInputStream(new ByteArrayInputStream(decompress()));
  }

  @Override
  public File getActualFile(boolean ignoreoverride)
  {
    return null;
  }

  public byte[] decompress() throws Exception
  {
    Inflater inflater = new Inflater();
    byte udata[] = new byte[uncomprLength];
    inflater.setInput(cdata);
    inflater.inflate(udata);
    return udata;
  }

  @Override
  public void write(OutputStream os) throws IOException
  {
    FileWriterNI.writeInt(os, filename.length() + 1);
    FileWriterNI.writeString(os, filename, filename.length());
    FileWriterNI.writeByte(os, (byte)0);
    FileWriterNI.writeInt(os, uncomprLength);
    FileWriterNI.writeInt(os, comprLength);
    FileWriterNI.writeBytes(os, cdata);
  }
}
