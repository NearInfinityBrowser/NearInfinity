// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.infinity.gui.BIFFEditor;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.io.FileInputStreamNI;
import org.infinity.util.io.FileNI;
import org.infinity.util.io.FileOutputStreamNI;
import org.infinity.util.io.FileWriterNI;
import org.infinity.util.io.RandomAccessFileNI;

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
    return Arrays.copyOfRange(compr, 0, clength);
  }

  private static void compressBIF(File biff, File compr, String uncrfilename) throws IOException
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStreamNI(compr));
    FileWriterNI.writeString(os, "BIF ", 4);
    FileWriterNI.writeString(os, "V1.0", 4);
    FileWriterNI.writeInt(os, uncrfilename.length());
    FileWriterNI.writeString(os, uncrfilename, uncrfilename.length());
    FileWriterNI.writeInt(os, (int)biff.length()); // Uncompressed length
    FileWriterNI.writeInt(os, 0); // Compressed length
    OutputStream dos = new DeflaterOutputStream(os);
    InputStream is = new BufferedInputStream(new FileInputStreamNI(biff));
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
    RandomAccessFile ranfile = new RandomAccessFileNI(compr, "rw");
    ranfile.seek((long)(0x10 + uncrfilename.length()));
    FileWriterNI.writeInt(ranfile, comprsize);
    ranfile.close();
  }

  private static void compressBIFC(File biff, File compr) throws Exception
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStreamNI(compr));
    FileWriterNI.writeString(os, "BIFC", 4);
    FileWriterNI.writeString(os, "V1.0", 4);
    FileWriterNI.writeInt(os, (int)biff.length());
    InputStream is = new BufferedInputStream(new FileInputStreamNI(biff));
    byte block[] = readBytes(is, 8192);
    while (block.length != 0) {
      byte compressed[] = compress(block);
      FileWriterNI.writeInt(os, block.length);
      FileWriterNI.writeInt(os, compressed.length);
      FileWriterNI.writeBytes(os, compressed);
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
    return Arrays.copyOfRange(buffer, 0, bytesread);
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
    File dummyfile = FileNI.getFile(Profile.getRootFolders(),
                              "data" + File.separatorChar + "_dummy.bif");
    writeBIFF(dummyfile);
    ResourceFactory.getKeyfile().closeBIFFFile();
    bifentry.setFileLength((int)dummyfile.length()); // Uncompressed length
    if (format == BIFFEditor.BIFF) {
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = FileNI.getFile(Profile.getRootFolders(), bifentry.toString());
      else
        realfile.delete();
      dummyfile.renameTo(realfile);
    }
    else if (format == BIFFEditor.BIF) {
      File compressedfile = FileNI.getFile(Profile.getRootFolders(),
                                     "data" + File.separatorChar + "_dummy.cbf");
      compressBIF(dummyfile, compressedfile, bifentry.toString());
      dummyfile.delete();
      // Delete both BIFF version if this exist
      String filename = FileNI.getFile(Profile.getRootFolders(), bifentry.toString()).toString();
      new FileNI(filename).delete();
      filename = filename.substring(0, filename.lastIndexOf(".")) + ".cbf";
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = new FileNI(filename);
      else
        realfile.delete();
      compressedfile.renameTo(realfile);
    }
    else if (format == BIFFEditor.BIFC) {
      File compressedfile = FileNI.getFile(Profile.getRootFolders(),
                                     "data" + File.separatorChar + "_dummy2.bif");
      compressBIFC(dummyfile, compressedfile);
      dummyfile.delete();
      // Delete old BIF, rename this to real name
      File realfile = bifentry.getFile();
      if (realfile == null)
        realfile = FileNI.getFile(Profile.getRootFolders(), bifentry.toString());
      else
        realfile.delete();
      compressedfile.renameTo(realfile);
    }
  }

  private BIFFResourceEntry reloadNode(ResourceEntry entry, int newoffset)
  {
    ResourceFactory.getResources().removeResourceEntry(entry);
    BIFFResourceEntry newentry = new BIFFResourceEntry(bifentry, entry.toString(), newoffset);
    ResourceFactory.getResources().addResourceEntry(newentry, newentry.getTreeFolder());
    return newentry;
  }

  private void writeBIFF(File file) throws Exception
  {
    OutputStream os = new BufferedOutputStream(new FileOutputStreamNI(file));
    FileWriterNI.writeString(os, "BIFF", 4);
    FileWriterNI.writeString(os, "V1  ", 4);
    FileWriterNI.writeInt(os, resources.size());
    FileWriterNI.writeInt(os, tileresources.size());
    FileWriterNI.writeInt(os, 0x14);
    int offset = 20 + 16 * resources.size() + 20 * tileresources.size();
    int index = 0; // Non-tileset index starts at 0
    for (final ResourceEntry resourceEntry : resources.keySet()) {
      BIFFResourceEntry newentry = reloadNode(resourceEntry, index);
      FileWriterNI.writeInt(os, newentry.getLocator());
      FileWriterNI.writeInt(os, offset); // Offset
      int info[] = resourceEntry.getResourceInfo(resources.get(resourceEntry).booleanValue());
      offset += info[0];
      FileWriterNI.writeInt(os, info[0]); // Size
      FileWriterNI.writeShort(os, (short)ResourceFactory.getKeyfile().getExtensionType(resourceEntry.getExtension()));
      FileWriterNI.writeShort(os, (short)0); // Unknown
      index++;
    }
    index = 1; // Tileset index starts at 1
    for (final ResourceEntry resourceEntry : tileresources.keySet()) {
      BIFFResourceEntry newentry = reloadNode(resourceEntry, index);
      FileWriterNI.writeInt(os, newentry.getLocator());
      FileWriterNI.writeInt(os, offset); // Offset
      int info[] = resourceEntry.getResourceInfo(tileresources.get(resourceEntry).booleanValue());
      FileWriterNI.writeInt(os, info[0]); // Number of tiles
      FileWriterNI.writeInt(os, info[1]); // Size of each tile (in bytes)
      offset += info[0] * info[1];
      FileWriterNI.writeShort(os, (short)ResourceFactory.getKeyfile().getExtensionType(resourceEntry.getExtension()));
      FileWriterNI.writeShort(os, (short)0); // Unknown
      index++;
    }
    for (final ResourceEntry resourceEntry : resources.keySet())
      FileWriterNI.writeBytes(os, resourceEntry.getResourceData(resources.get(resourceEntry).booleanValue()));
    for (final ResourceEntry resourceEntry : tileresources.keySet())
      FileWriterNI.writeBytes(os, resourceEntry.getResourceData(tileresources.get(resourceEntry).booleanValue()));
    os.close();
  }
}

