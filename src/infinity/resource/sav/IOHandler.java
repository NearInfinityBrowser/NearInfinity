// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sav;

import infinity.datatype.DecNumber;
import infinity.datatype.TextString;
import infinity.resource.ResourceFactory;
import infinity.resource.Writeable;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.*;

import java.io.*;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class IOHandler implements Writeable
{
  private final ResourceEntry entry;
  private final TextString header;
  private File tempfolder;
  private List<FileEntry> fileentries;

  public IOHandler(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte buffer[] = entry.getResourceData(true); // ignoreOverride - no real effect
    header = new TextString(buffer, 0, 8, null);
    if (!header.toString().equalsIgnoreCase("SAV V1.0"))
      throw new Exception("Unsupported version: " + header);
    fileentries = new ArrayList<FileEntry>();
    int offset = 8;
    while (offset < buffer.length) {
      FileEntry fileentry = new FileEntry(buffer, offset);
      fileentries.add(fileentry);
      offset = fileentry.getEndOffset();
    }
    Collections.sort(fileentries);
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    header.write(os);
    for (int i = 0; i < fileentries.size(); i++)
      fileentries.get(i).write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public void close()
  {
    if (tempfolder == null)
      return;
    File files[] = tempfolder.listFiles();
    for (final File file : files)
      file.delete();
    tempfolder.delete();
    tempfolder = null;
  }

  public void compress(List entries) throws Exception
  {
    fileentries.clear();
    for (int i = 0; i < entries.size(); i++)
      fileentries.add(new FileEntry((ResourceEntry)entries.get(i)));
    close();
  }

  public List<FileResourceEntry> decompress() throws Exception
  {
    List<FileResourceEntry> entries = new ArrayList<FileResourceEntry>(fileentries.size());
    tempfolder = new File(ResourceFactory.getRootDir(), '_' + entry.getTreeFolder());
    tempfolder.mkdir();
    for (int i = 0; i < fileentries.size(); i++) {
      FileEntry fentry = fileentries.get(i);
      File file = new File(tempfolder, fentry.toString());
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      Filewriter.writeBytes(os, fentry.decompress());
      os.close();
      entries.add(new FileResourceEntry(file));
    }
    return entries;
  }

  public List getFileEntries()
  {
    return fileentries;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class FileEntry extends ResourceEntry implements Writeable
  {
    private final String filename;
    private final DecNumber comprLength;
    private final DecNumber uncomprLength;
    private byte cdata[];

    private FileEntry(byte buffer[], int offset)
    {
      DecNumber filenameLength = new DecNumber(buffer, offset, 4, null);
      filename = new String(buffer, offset + 4, filenameLength.getValue() - 1);
      offset += 4 + filenameLength.getValue();
      uncomprLength = new DecNumber(buffer, offset, 4, null);
      comprLength = new DecNumber(buffer, offset + 4, 4, null);
      cdata = ArrayUtil.getSubArray(buffer, offset + 8, comprLength.getValue());
    }

    private FileEntry(ResourceEntry rentry) throws Exception
    {
      comprLength = new DecNumber(new byte[4], 0, 4, null);
      uncomprLength = new DecNumber(new byte[4], 0, 4, null);
      filename = rentry.toString();
      byte udata[] = rentry.getResourceData(true);
      if (udata.length == 0) {
        uncomprLength.setValue(0);
        comprLength.setValue(8);
        cdata = new byte[]{0x78, 0x01, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01};
      }
      else {
        cdata = new byte[udata.length * 2];
        uncomprLength.setValue(udata.length);
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(udata);
        deflater.finish();
        int clength = deflater.deflate(cdata);
        cdata = ArrayUtil.getSubArray(cdata, 0, clength);
        comprLength.setValue(clength);
      }
    }

    private int getEndOffset()
    {
      return comprLength.getOffset() + 4 + cdata.length;
    }

    public String toString()
    {
      return filename;
    }

    public String getResourceName()
    {
      return filename;
    }

    public String getExtension()
    {
      return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
    }

    public String getTreeFolder()
    {
      return null;
    }

    public boolean hasOverride()
    {
      return false;
    }

    public int[] getResourceInfo(boolean ignoreoverride) throws Exception
    {
      if (filename.toUpperCase().endsWith(".TIS")) {
        try {
          byte data[] = decompress();
          if (!new String(data, 0, 4).equalsIgnoreCase("TIS ")) {
            int tilesize = 64 * 64 + 4 * 256;
            int tilecount = uncomprLength.getValue() / tilesize;
            return new int[]{tilecount, tilesize};
          }
          return new int[]{Byteconvert.convertInt(data, 8), Byteconvert.convertInt(data, 12)};
        } catch (IOException e) {
          e.printStackTrace();
        }
        return null;
      }
      return new int[]{uncomprLength.getValue()};
    }

    public byte[] getResourceData(boolean ignoreoverride) throws Exception
    {
      return decompress();
    }

    public InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
    {
      return new BufferedInputStream(new ByteArrayInputStream(decompress()));
    }

    public File getActualFile(boolean ignoreoverride)
    {
      return null;
    }

    public byte[] decompress() throws Exception
    {
      Inflater inflater = new Inflater();
      byte udata[] = new byte[uncomprLength.getValue()];
      inflater.setInput(cdata);
      inflater.inflate(udata);
      return udata;
    }

    public void write(OutputStream os) throws IOException
    {
      Filewriter.writeInt(os, filename.length() + 1);
      Filewriter.writeString(os, filename, filename.length());
      Filewriter.writeByte(os, (byte)0);
      uncomprLength.write(os);
      comprLength.write(os);
      Filewriter.writeBytes(os, cdata);
    }
  }
}

