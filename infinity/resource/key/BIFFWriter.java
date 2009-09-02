// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.gui.BIFFEditor;
import infinity.resource.ResourceFactory;
import infinity.util.ArrayUtil;
import infinity.util.Filewriter;

import java.io.*;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class BIFFWriter
{
  private final BIFFEntry bifentry;
  private final Map<ResourceEntry, Boolean> resources = new HashMap<ResourceEntry, Boolean>();
  private final Map<ResourceEntry, Boolean> tileresources = new HashMap<ResourceEntry, Boolean>();
  private final int format;

  private static byte[] compress(byte data[])
  {
    Deflater deflater = new Deflater();
    byte compr[] = new byte[data.length * 2];
    deflater.setInput(data);
    deflater.finish();
    int clength = deflater.deflate(compr);
    return ArrayUtil.getSubArray(compr, 0, clength);
  }

  private static void compressBIF(File biff, File compr, String uncrfilename) throws IOException
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(compr));
    Filewriter.writeString(os, "BIF ", 4);
    Filewriter.writeString(os, "V1.0", 4);
    Filewriter.writeInt(os, uncrfilename.length());
    Filewriter.writeString(os, uncrfilename, uncrfilename.length());
    Filewriter.writeInt(os, (int)biff.length()); // Uncompressed length
    Filewriter.writeInt(os, 0); // Compressed length
    OutputStream dos = new DeflaterOutputStream(os);
    InputStream is = new BufferedInputStream(new FileInputStream(biff));
    byte buffer[] = new byte[32765];
    int bytesread = is.read(buffer, 0, buffer.length);
    while (bytesread != -1) {
      dos.write(buffer, 0, bytesread);
      bytesread = is.read(buffer, 0, buffer.length);
    }
    is.close();
    dos.close();
    os.close();
    int comprsize = (int)compr.length() - (0x20 + uncrfilename.length());
    RandomAccessFile ranfile = new RandomAccessFile(compr, "rw");
    ranfile.seek((long)(0x10 + uncrfilename.length()));
    Filewriter.writeInt(ranfile, comprsize);
    ranfile.close();
  }

  private static void compressBIFC(File biff, File compr) throws Exception
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(compr));
    Filewriter.writeString(os, "BIFC", 4);
    Filewriter.writeString(os, "V1.0", 4);
    Filewriter.writeInt(os, (int)biff.length());
    InputStream is = new BufferedInputStream(new FileInputStream(biff));
    byte block[] = readBytes(is, 8192);
    while (block.length != 0) {
      byte compressed[] = compress(block);
      Filewriter.writeInt(os, block.length);
      Filewriter.writeInt(os, compressed.length);
      Filewriter.writeBytes(os, compressed);
      block = readBytes(is, 8192);
    }
    is.close();
    os.close();
  }

  private static byte[] readBytes(InputStream is, int length) throws Exception
  {
    byte buffer[] = new byte[length];
    int bytesread = 0;
    while (bytesread < length) {
      int newread = is.read(buffer, bytesread, length - bytesread);
      if (newread == -1)
        break;
      bytesread += newread;
    }
    return ArrayUtil.getSubArray(buffer, 0, bytesread);
  }

  public BIFFWriter(BIFFEntry bifentry, int format)
  {
    this.bifentry = bifentry;
    this.format = format;
    if (bifentry.getIndex() == -1)  // new biff-file
      ResourceFactory.getKeyfile().addBIFFEntry(bifentry);
  }

  public void addResource(ResourceEntry resourceentry, boolean ignoreoverride)
  {
    if (resourceentry.getExtension().equalsIgnoreCase("TIS"))
      tileresources.put(resourceentry, Boolean.valueOf(ignoreoverride));
    else
      resources.put(resourceentry, Boolean.valueOf(ignoreoverride));
  }

  public void write() throws Exception
  {
    File dummyfile = new File(ResourceFactory.getRootDir(),
                              "data" + File.separatorChar + "_dummy.bif");
    writeBIFF(dummyfile);
    ResourceFactory.getKeyfile().closeBIFFFile();
    bifentry.setFileLength((int)dummyfile.length()); // Uncompressed length
    if (format == BIFFEditor.BIFF) {
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = new File(ResourceFactory.getRootDir(), bifentry.toString());
      else
        realfile.delete();
      dummyfile.renameTo(realfile);
    }
    else if (format == BIFFEditor.BIF) {
      File compressedfile = new File(ResourceFactory.getRootDir(),
                                     "data" + File.separatorChar + "_dummy.cbf");
      compressBIF(dummyfile, compressedfile, bifentry.toString());
      dummyfile.delete();
      // Delete both BIFF version if this exist
      String filename = ResourceFactory.getRootDir().toString() + bifentry.toString();
      new File(filename).delete();
      filename = filename.substring(0, filename.lastIndexOf(".")) + ".cbf";
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = new File(filename);
      else
        realfile.delete();
      compressedfile.renameTo(realfile);
    }
    else if (format == BIFFEditor.BIFC) {
      File compressedfile = new File(ResourceFactory.getRootDir(),
                                     "data" + File.separatorChar + "_dummy2.bif");
      compressBIFC(dummyfile, compressedfile);
      dummyfile.delete();
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = new File(ResourceFactory.getRootDir(), bifentry.toString());
      else
        realfile.delete();
      compressedfile.renameTo(realfile);
    }
  }

  private BIFFResourceEntry reloadNode(ResourceEntry entry, int newoffset)
  {
    ResourceFactory.getInstance().getResources().removeResourceEntry(entry);
    BIFFResourceEntry newentry = new BIFFResourceEntry(bifentry, entry.toString(), newoffset);
    ResourceFactory.getInstance().getResources().addResourceEntry(newentry, newentry.getTreeFolder());
    return newentry;
  }

  private void writeBIFF(File file) throws Exception
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    Filewriter.writeString(os, "BIFF", 4);
    Filewriter.writeString(os, "V1  ", 4);
    Filewriter.writeInt(os, resources.size());
    Filewriter.writeInt(os, tileresources.size());
    Filewriter.writeInt(os, 0x14);
    int offset = 20 + 16 * resources.size() + 20 * tileresources.size();
    int index = 0; // Non-tileset index starts at 0
    for (final ResourceEntry resourceEntry : resources.keySet()) {
      BIFFResourceEntry newentry = reloadNode(resourceEntry, index);
      Filewriter.writeInt(os, newentry.getLocator());
      Filewriter.writeInt(os, offset); // Offset
      int info[] = resourceEntry.getResourceInfo(resources.get(resourceEntry).booleanValue());
      offset += info[0];
      Filewriter.writeInt(os, info[0]); // Size
      Filewriter.writeShort(os, (short)ResourceFactory.getKeyfile().getExtensionType(resourceEntry.getExtension()));
      Filewriter.writeShort(os, (short)0); // Unknown
      index++;
    }
    index = 1; // Tileset index starts at 1
    for (final ResourceEntry resourceEntry : tileresources.keySet()) {
      BIFFResourceEntry newentry = reloadNode(resourceEntry, index);
      Filewriter.writeInt(os, newentry.getLocator());
      Filewriter.writeInt(os, offset); // Offset
      int info[] = resourceEntry.getResourceInfo(tileresources.get(resourceEntry).booleanValue());
      Filewriter.writeInt(os, info[0]); // Number of tiles
      Filewriter.writeInt(os, info[1]); // Size of each tile (in bytes)
      offset += info[0] * info[1];
      Filewriter.writeShort(os, (short)ResourceFactory.getKeyfile().getExtensionType(resourceEntry.getExtension()));
      Filewriter.writeShort(os, (short)0); // Unknown
      index++;
    }
    for (final ResourceEntry resourceEntry : resources.keySet())
      Filewriter.writeBytes(os, resourceEntry.getResourceData(resources.get(resourceEntry).booleanValue()));
    for (final ResourceEntry resourceEntry : tileresources.keySet()) 
      Filewriter.writeBytes(os, resourceEntry.getResourceData(tileresources.get(resourceEntry).booleanValue()));
    os.close();
  }
}

