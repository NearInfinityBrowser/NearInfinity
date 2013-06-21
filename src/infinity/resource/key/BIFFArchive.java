// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.NearInfinity;
import infinity.gui.WindowBlocker;
import infinity.util.*;

import javax.swing.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.*;

public final class BIFFArchive
{
  private static final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
  private final File file;
  private final String signature;
  private RandomAccessFile ranfile;
  private int biffEntryOff, comprOff, numFiles;

  BIFFArchive(File file) throws IOException
  {
    this.file = file;
    InputStream is = new BufferedInputStream(new FileInputStreamCI(file));
    signature = Filereader.readString(is, 4);
    if (signature.equals("BIFF")) {
      readBIFFHeader(is);
      ranfile = new RandomAccessFileCI(file, "r");
    }
    else if (signature.equals("BIF ")) {
      Filereader.readString(is, 4); // Version
      int namelength = Filereader.readInt(is);
      Filereader.readString(is, namelength); // Name
      Filereader.readInt(is); // Unc_length
      Filereader.readInt(is); // Com_length
      comprOff = 20 + namelength;
      InflaterInputStream iis = new InflaterInputStream(is);
      Filereader.readString(iis, 4); // BIFF
      readBIFFHeader(iis);
      iis.close();
    }
    else if (signature.equals("BIFC")) {
      Filereader.readString(is, 4); // Version
      Filereader.readInt(is); // Unc_length
      comprOff = 12;
      is.skip((long)8); // 8 - Header of BIFC Block
      InflaterInputStream iis = new InflaterInputStream(is);
      Filereader.readString(iis, 4); // BIFF
      readBIFFHeader(iis);
      iis.close();
    }
    else
      JOptionPane.showMessageDialog(null, "Unsupported BIFF file:" + file, "Error", JOptionPane.ERROR_MESSAGE);
    is.close();
  }

  public void close() throws IOException
  {
    if (ranfile != null)
      ranfile.close();
  }

  public File getFile()
  {
    return file;
  }

  public String getSignature()
  {
    return signature;
  }

  byte[] getResource(int offset, boolean isTile) throws IOException
  {
    if (isTile)
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    else
      offset *= 16;
    byte buffer[] = null;
    if (signature.equals("BIFF"))
      buffer = getBIFFResource(offset, isTile);
    else if (signature.equals("BIF "))
      buffer = getBIFResource(offset, isTile);
    else if (signature.equals("BIFC"))
      buffer = getBIFCResource(offset, isTile);
    blocker.setBlocked(false);
    return buffer;
  }

  InputStream getResourceAsStream(int offset, boolean isTile) throws IOException
  {
    if (isTile)
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    else
      offset *= 16;
    if (signature.equals("BIFF"))
      return getBIFFResourceAsStream(offset);
    else if (signature.equals("BIF "))
      return getBIFResourceAsStream(offset, isTile);
    else if (signature.equals("BIFC"))
      return getBIFCResourceAsStream(offset, isTile);
    throw new IOException("Not implemented");
  }

  int[] getResourceInfo(int offset, boolean isTile) throws IOException
  {
    if (isTile)
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    else
      offset *= 16;
    if (signature.equals("BIFF"))
      return getBIFFResourceInfo(offset, isTile);
    else if (signature.equals("BIF "))
      return getBIFResourceInfo(offset, isTile);
    return getBIFCResourceInfo(offset, isTile);
  }

  private byte[] getBIFCResource(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamCI(file));
    fis.skip((long)comprOff);
    int startoffset = biffEntryOff + offset;

    int currentoffset = 0;
    BifcBlock block = new BifcBlock(fis);
    while (startoffset > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      fis.skip((long)block.compSize);
      block = new BifcBlock(fis);
    }
    // File Header now begins inside block
    byte header[];
    if (isTile)
      header = new byte[20];
    else
      header = new byte[16];
    int hindex = 0;
    while (hindex < header.length) {
      int toread = Math.min(header.length, currentoffset + block.decompSize - startoffset);
      byte header2[] = block.getData(fis, startoffset - currentoffset, toread);
      System.arraycopy(header2, 0, header, hindex, header2.length);
      hindex += header2.length;
      if (hindex < header.length) {
        currentoffset += block.decompSize;
        block = new BifcBlock(fis);
      }
    }

    int size;
    byte[] tileheader = null;
//    Byteconvert.convertInt(header, 0); // Locator
    int resoff = Byteconvert.convertInt(header, 4);
    if (!isTile) {
      size = Byteconvert.convertInt(header, 8);
//      Byteconvert.convertShort(header, 12); // Type
//      Byteconvert.convertShort(header, 14); // Unknown
    }
    else {
      int tilecount = Byteconvert.convertInt(header, 8);
      int tilesize = Byteconvert.convertInt(header, 12);
      size = tilecount * tilesize;
      tileheader = getTisHeader(tilecount, tilesize);
//      Byteconvert.convertShort(header, 16); // Type
//      Byteconvert.convertShort(header, 18); // Unknown
    }

    if (size > 1000000)
      blocker.setBlocked(true);

    if (resoff > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      block = new BifcBlock(fis);
    }
    while (resoff > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      block.getData(fis, 0, block.decompSize);
      block = new BifcBlock(fis);
    }
    // Data now starts inside block
    byte buffer[] = new byte[isTile ? size + tileheader.length : size];
    int index = 0;
    int indexofs = 0;
    if (isTile) {
      System.arraycopy(tileheader, 0, buffer, 0, tileheader.length);
      indexofs += tileheader.length;
    }
    while (index < size) {
      int toread = Math.min(size - index, currentoffset + block.decompSize - (resoff + index));
      byte buffer2[] = block.getData(fis, resoff + index - currentoffset, toread);
      System.arraycopy(buffer2, 0, buffer, index + indexofs, buffer2.length);
      index += buffer2.length;
      currentoffset += block.decompSize;
      if (index < size)
        block = new BifcBlock(fis);
    }
    fis.close();
    return buffer;
  }

  private InputStream getBIFCResourceAsStream(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamCI(file));
    fis.skip((long)comprOff);
    int startoffset = biffEntryOff + offset;

    int currentoffset = 0;
    BifcBlock block = new BifcBlock(fis);
    while (startoffset > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      fis.skip((long)block.compSize);
      block = new BifcBlock(fis);
    }
    // File Header now begins inside block
    byte header[];
    if (isTile)
      header = new byte[20];
    else
      header = new byte[16];
    int hindex = 0;
    while (hindex < header.length) {
      int toread = Math.min(header.length, currentoffset + block.decompSize - startoffset);
      byte header2[] = block.getData(fis, startoffset - currentoffset, toread);
      System.arraycopy(header2, 0, header, hindex, header2.length);
      hindex += header2.length;
      if (hindex < header.length) {
        currentoffset += block.decompSize;
        block = new BifcBlock(fis);
      }
    }

    int size;
//    Byteconvert.convertInt(header, 0); // Locator
    int resoff = Byteconvert.convertInt(header, 4);
    if (!isTile) {
      size = Byteconvert.convertInt(header, 8);
//      Byteconvert.convertShort(header, 12); // Type
//      Byteconvert.convertShort(header, 14); // Unknown
    }
    else {
      int tilecount = Byteconvert.convertInt(header, 8);
      size = tilecount * Byteconvert.convertInt(header, 12);
//      Byteconvert.convertShort(header, 16); // Type
//      Byteconvert.convertShort(header, 18); // Unknown
    }

    if (resoff > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      block = new BifcBlock(fis);
    }
    while (resoff > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      block.getData(fis, 0, block.decompSize);
      block = new BifcBlock(fis);
    }
    // Data now starts inside block
    return new BifcInputStream(fis, size, block, resoff - currentoffset);
  }

  ///////////////////////////////
  // BIFC Support
  ///////////////////////////////

  private int[] getBIFCResourceInfo(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamCI(file));
    fis.skip((long)comprOff);
    int startoffset = biffEntryOff + offset;

    int currentoffset = 0;
    BifcBlock block = new BifcBlock(fis);
    while (startoffset > currentoffset + block.decompSize) {
      currentoffset += block.decompSize;
      fis.skip((long)block.compSize);
      block = new BifcBlock(fis);
    }
    // File Header now begins inside block
    byte header[];
    if (isTile)
      header = new byte[20];
    else
      header = new byte[16];
    int hindex = 0;
    while (hindex < header.length) {
      int toread = Math.min(header.length, currentoffset + block.decompSize - startoffset);
      byte header2[] = block.getData(fis, startoffset - currentoffset, toread);
      System.arraycopy(header2, 0, header, hindex, header2.length);
      hindex += header2.length;
      if (hindex < header.length) {
        currentoffset += block.decompSize;
        block = new BifcBlock(fis);
      }
    }
    fis.close();

    if (isTile)
      return new int[]{Byteconvert.convertInt(header, 8), Byteconvert.convertInt(header, 12)};
    return new int[]{Byteconvert.convertInt(header, 8)};
  }

  private byte[] getBIFFResource(int offset, boolean isTile) throws IOException
  {
    ranfile.seek((long)(biffEntryOff + offset));
    Filereader.readInt(ranfile); // Locator
    int resoff = Filereader.readInt(ranfile);
    int size = Filereader.readInt(ranfile);
    byte[] tileheader = null;
    if (isTile) {
      int tilesize = Filereader.readInt(ranfile);
      tileheader = getTisHeader(size, tilesize);
      size *= tilesize;
    }
//    Filereader.readShort(ranfile); // Type
//    Filereader.readShort(ranfile); // Unknown
    if (size > 1000000)
      blocker.setBlocked(true);

    byte buffer[] = new byte[isTile ? size + tileheader.length : size];
    int index = 0;
    if (isTile) {
      System.arraycopy(tileheader, 0, buffer, index, tileheader.length);
      index += tileheader.length;
    }
    ranfile.seek((long)resoff);
    ranfile.readFully(buffer, index, size);
    return buffer;
  }

  private InputStream getBIFFResourceAsStream(int offset) throws IOException
  {
    ranfile.seek((long)(biffEntryOff + offset));
    Filereader.readInt(ranfile); // Locator
    int resoff = Filereader.readInt(ranfile);

    InputStream is = new BufferedInputStream(new FileInputStreamCI(file));
    is.skip((long)resoff);

    return is;
  }

  ///////////////////////////////
  // BIFF Support
  ///////////////////////////////

  private int[] getBIFFResourceInfo(int offset, boolean isTile) throws IOException
  {
    ranfile.seek((long)(biffEntryOff + offset + 8));
    if (isTile)
      return new int[]{Filereader.readInt(ranfile), Filereader.readInt(ranfile)};
    return new int[]{Filereader.readInt(ranfile)};
  }

  private byte[] getBIFResource(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamCI(file));
    bis.skip((long)comprOff);

    InflaterInputStream iis = new InflaterInputStream(bis);
    iis.skip((long)(biffEntryOff + offset));
    Filereader.readInt(iis); // Locator
    int resoff = Filereader.readInt(iis);
    int size;
    byte[] tileheader = null;
    if (isTile) {
      int tilecount = Filereader.readInt(iis);
      int tilesize = Filereader.readInt(iis);
      size = tilecount * tilesize;
      offset += 4;
      tileheader = getTisHeader(tilecount, tilesize);
    }
    else
      size = Filereader.readInt(iis);
    Filereader.readShort(iis); // Type
    Filereader.readShort(iis); // Unknown

    if (size > 1000000)
      blocker.setBlocked(true);

    iis.skip((long)(resoff - (biffEntryOff + offset + 16)));
    byte buffer[] = new byte[isTile ? size + tileheader.length : size];
    int index = 0;
    if (isTile) {
      System.arraycopy(tileheader, 0, buffer, index, tileheader.length);
      index += tileheader.length;
    }
    System.arraycopy(Filereader.readBytes(iis, size), 0, buffer, index, size);
    iis.close();
    bis.close();
    return buffer;
  }

  private InputStream getBIFResourceAsStream(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamCI(file));
    bis.skip((long)comprOff);

    InflaterInputStream iis = new InflaterInputStream(bis);
    iis.skip((long)(biffEntryOff + offset));
    Filereader.readInt(iis); // Locator
    int resoff = Filereader.readInt(iis);
    Filereader.readInt(iis); // Size
    if (isTile) {
      Filereader.readInt(iis); // Tilesize
      offset += 4;
    }
    Filereader.readShort(iis); // Type
    Filereader.readShort(iis); // Unknown

    iis.skip((long)(resoff - (biffEntryOff + offset + 16)));
    return iis;
  }

  ///////////////////////////////
  // BIF Support
  ///////////////////////////////

  private int[] getBIFResourceInfo(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamCI(file));
    bis.skip((long)comprOff);

    InflaterInputStream iis = new InflaterInputStream(bis);
    iis.skip((long)(biffEntryOff + offset));

    Filereader.readInt(iis); // Locator
    Filereader.readInt(iis); // Resoff
    if (isTile) {
      int tilecount = Filereader.readInt(iis);
      int tilesize = Filereader.readInt(iis);
      iis.close();
      bis.close();
      return new int[]{tilecount, tilesize};
    }
    int size = Filereader.readInt(iis);
    iis.close();
    bis.close();
    return new int[]{size};
  }

  private void readBIFFHeader(InputStream is) throws IOException
  {
    Filereader.readString(is, 4); // Version
    numFiles = Filereader.readInt(is);
    Filereader.readInt(is); // Numtiles
    biffEntryOff = Filereader.readInt(is);
  }

  public static byte[] getTisHeader(int tilecount, int tilesize)
  {
    ByteBuffer buf = ByteBuffer.allocate(24);
    buf.order(ByteOrder.LITTLE_ENDIAN);
    buf.put(new String("TIS V1  ").getBytes(), 0, 8);
    buf.putInt(tilecount);
    buf.putInt(tilesize);
    buf.putInt(0x18);
    buf.putInt(64);
    return buf.array();
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class BifcBlock
  {
    private final int decompSize;
    private final int compSize;
    private byte buffer[];

    private BifcBlock(InputStream is) throws IOException
    {
      decompSize = Filereader.readInt(is);
      compSize = Filereader.readInt(is);
    }

    private byte[] getData(InputStream is, int offset, int length) throws IOException
    {
      if (buffer == null) {
        Inflater inflater = new Inflater();
        buffer = new byte[decompSize];
        inflater.setInput(Filereader.readBytes(is, compSize));
        try {
          inflater.inflate(buffer);
        } catch (DataFormatException e) {
          e.printStackTrace();
          throw new IOException();
        }
      }
      if (length == decompSize)
        return buffer;
      return ArrayUtil.getSubArray(buffer, offset, length);
    }
  }

  private static final class BifcInputStream extends InputStream
  {
    private final InputStream is;
    private int size;
    private BifcBlock block;
    private int blockIndex;

    private BifcInputStream(InputStream is, int size, BifcBlock block, int blockIndex)
    {
      this.is = is;
      this.size = size;
      this.block = block;
      this.blockIndex = blockIndex;
    }

    public int read() throws IOException
    {
      if (size == 0)
        return -1;
      if (blockIndex == block.decompSize) { // Read new block
        block = new BifcBlock(is);
        blockIndex = 0;
      }
      size--;
      byte b = block.getData(is, 0, block.decompSize)[blockIndex++];
      if (b < 0)
        return (int)b + 256;
      return (int)b;
    }

    public int read(byte b[], int off, int len) throws IOException
    {
      if (size == 0)
        return -1;
      int remainder = len;
      while (remainder > 0 && size > 0) {
        if (blockIndex == block.decompSize) { // Read new block
          block = new BifcBlock(is);
          blockIndex = 0;
        }
        int tocopy = Math.min(remainder, block.decompSize - blockIndex);
        System.arraycopy(block.getData(is, 0, block.decompSize), blockIndex, b, off, tocopy);
        off += tocopy;
        blockIndex += tocopy;
        size -= tocopy;
        remainder -= tocopy;
      }
      return len - remainder;
    }

    public int available()
    {
      return size;
    }

    public boolean markSupported()
    {
      return false;
    }

    public void close() throws IOException
    {
      is.close();
    }
  }
}

