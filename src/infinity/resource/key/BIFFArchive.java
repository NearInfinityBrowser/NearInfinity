// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.NearInfinity;
import infinity.gui.WindowBlocker;
import infinity.util.DynamicArray;
import infinity.util.io.FileInputStreamNI;
import infinity.util.io.FileReaderNI;
import infinity.util.io.RandomAccessFileNI;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.swing.JOptionPane;

public final class BIFFArchive
{
  /** Available BIFF file versions */
  public enum Type {
    /** Uncompressed BIFF V1 */
    BIFF,
    /** File-compressed BIF V1.0 */
    BIF,
    /** Block-compressed BIFC V1.0 */
    BIFC,
    /** Unsupported file type */
    Unknown
  }

  private static final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
  private final File file;
  private final Type biffType;
  private int biffEntryOff, comprOff, numFiles;

  BIFFArchive(File file) throws IOException
  {
    this.file = file;
    InputStream is = new BufferedInputStream(new FileInputStreamNI(file));
    try {
      String signature = FileReaderNI.readString(is, 4);
      if (signature.equals("BIFF")) {
        biffType = Type.BIFF;
      } else if (signature.equals("BIF ")) {
        biffType = Type.BIF;
      } else if (signature.equals("BIFC")) {
        biffType = Type.BIFC;
      } else {
        biffType = Type.Unknown;
      }

      switch (biffType) {
        case BIFF:
        {
          readBIFFHeader(is);
          break;
        }
        case BIF:
        {
          FileReaderNI.readString(is, 4); // Version
          int namelength = FileReaderNI.readInt(is);
          FileReaderNI.readString(is, namelength); // Name
          FileReaderNI.readInt(is); // Unc_length
          FileReaderNI.readInt(is); // Com_length
          comprOff = 20 + namelength;
          InflaterInputStream iis = new InflaterInputStream(is);
          try {
            FileReaderNI.readString(iis, 4); // BIFF
            readBIFFHeader(iis);
          } finally {
            iis.close();
          }
          break;
        }
        case BIFC:
        {
          FileReaderNI.readString(is, 4); // Version
          FileReaderNI.readInt(is); // Unc_length
          comprOff = 12;
          is.skip((long)8); // 8 - Header of BIFC Block
          InflaterInputStream iis = new InflaterInputStream(is);
          try {
            FileReaderNI.readString(iis, 4); // BIFF
            readBIFFHeader(iis);
          } finally {
            iis.close();
          }
          break;
        }
        default:
          JOptionPane.showMessageDialog(null, "Unsupported BIFF file:" + file, "Error", JOptionPane.ERROR_MESSAGE);
      }
    } finally {
      is.close();
    }
  }

  public File getFile()
  {
    return file;
  }

  public Type getType()
  {
    return biffType;
  }

  synchronized byte[] getResource(int offset, boolean isTile) throws IOException
  {
    if (isTile) {
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    } else {
      offset *= 16;
    }
    byte[] buffer;
    switch (biffType) {
      case BIFF:
        buffer = getBIFFResource(offset, isTile);
        break;
      case BIF:
        buffer = getBIFResource(offset, isTile);
        break;
      case BIFC:
        buffer = getBIFCResource(offset, isTile);
        break;
      default:
        buffer = null;
    }
    blocker.setBlocked(false);
    return buffer;
  }

  synchronized InputStream getResourceAsStream(int offset, boolean isTile) throws IOException
  {
    if (isTile) {
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    } else {
      offset *= 16;
    }
    switch (biffType) {
      case BIFF:
        return getBIFFResourceAsStream(offset);
      case BIF:
        return getBIFResourceAsStream(offset, isTile);
      case BIFC:
        return getBIFCResourceAsStream(offset, isTile);
      default:
        throw new IOException("Not implemented");
    }
  }

  synchronized int[] getResourceInfo(int offset, boolean isTile) throws IOException
  {
    if (isTile) {
      offset = 16 * numFiles + 20 * (offset - 1); // Tileset index starts at 1
    } else {
      offset *= 16;
    }
    switch (biffType) {
      case BIFF:
        return getBIFFResourceInfo(offset, isTile);
      case BIF:
        return getBIFResourceInfo(offset, isTile);
      case BIFC:
        return getBIFCResourceInfo(offset, isTile);
      default:
        return null;
    }
  }

  private byte[] getBIFCResource(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamNI(file));
    try {
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
  //    DynamicArray.getInt(header, 0); // Locator
      int resoff = DynamicArray.getInt(header, 4);
      if (!isTile) {
        size = DynamicArray.getInt(header, 8);
  //      DynamicArray.getShort(header, 12); // Type
  //      DynamicArray.getShort(header, 14); // Unknown
      }
      else {
        int tilecount = DynamicArray.getInt(header, 8);
        int tilesize = DynamicArray.getInt(header, 12);
        size = tilecount * tilesize;
        tileheader = getTisHeader(tilecount, tilesize);
  //      DynamicArray.getShort(header, 16); // Type
  //      DynamicArray.getShort(header, 18); // Unknown
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
      byte[] buffer = new byte[isTile ? size + tileheader.length : size];
      int index = 0;
      int indexofs = 0;
      if (isTile) {
        System.arraycopy(tileheader, 0, buffer, 0, tileheader.length);
        indexofs += tileheader.length;
      }
      while (index < size) {
        int toread = Math.min(size - index, currentoffset + block.decompSize - (resoff + index));
        byte[] buffer2 = block.getData(fis, resoff + index - currentoffset, toread);
        System.arraycopy(buffer2, 0, buffer, index + indexofs, buffer2.length);
        index += buffer2.length;
        currentoffset += block.decompSize;
        if (index < size)
          block = new BifcBlock(fis);
      }
      return buffer;
    } finally {
      fis.close();
    }
  }

  private InputStream getBIFCResourceAsStream(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamNI(file));
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
//    DynamicArray.getInt(header, 0); // Locator
    int resoff = DynamicArray.getInt(header, 4);
    if (!isTile) {
      size = DynamicArray.getInt(header, 8);
//      DynamicArray.getShort(header, 12); // Type
//      DynamicArray.getShort(header, 14); // Unknown
    }
    else {
      int tilecount = DynamicArray.getInt(header, 8);
      size = tilecount * DynamicArray.getInt(header, 12);
//      DynamicArray.getShort(header, 16); // Type
//      DynamicArray.getShort(header, 18); // Unknown
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
    BufferedInputStream fis = new BufferedInputStream(new FileInputStreamNI(file));
    try {
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

      if (isTile) {
        return new int[]{DynamicArray.getInt(header, 8), DynamicArray.getInt(header, 12)};
      } else {
        return new int[]{DynamicArray.getInt(header, 8)};
      }
    } finally {
      fis.close();
    }
  }

  private synchronized byte[] getBIFFResource(int offset, boolean isTile) throws IOException
  {
    if (file == null) {
      throw new IOException("No file specified");
    }
    RandomAccessFile ranfile = new RandomAccessFileNI(file, "r");
    try {
      ranfile.seek((long)(biffEntryOff + offset + 4));  // skip locator
      int resoff = FileReaderNI.readInt(ranfile);
      int size = FileReaderNI.readInt(ranfile);
      byte[] tileheader = null;
      if (isTile) {
        int tilesize = FileReaderNI.readInt(ranfile);
        tileheader = getTisHeader(size, tilesize);
        size *= tilesize;
      }
  //    Filereader.readShort(ranfile); // Type
  //    Filereader.readShort(ranfile); // Unknown
      if (size > 1000000)
        blocker.setBlocked(true);

      byte[] buffer = new byte[isTile ? size + tileheader.length : size];
      int index = 0;
      if (isTile) {
        System.arraycopy(tileheader, 0, buffer, index, tileheader.length);
        index += tileheader.length;
      }
      ranfile.seek((long)resoff);
      ranfile.readFully(buffer, index, size);
      return buffer;
    } finally {
      ranfile.close();
    }
  }

  private synchronized InputStream getBIFFResourceAsStream(int offset) throws IOException
  {
    if (file == null) {
      throw new IOException("No file specified");
    }
    RandomAccessFile ranfile = new RandomAccessFileNI(file, "r");
    try {
      ranfile.seek((long)(biffEntryOff + offset + 4));  // skip locator
      int resoff = FileReaderNI.readInt(ranfile);

      InputStream is = new BufferedInputStream(new FileInputStreamNI(file));
      is.skip((long)resoff);

      return is;
    } finally {
      ranfile.close();
    }
  }

  ///////////////////////////////
  // BIFF Support
  ///////////////////////////////

  private synchronized int[] getBIFFResourceInfo(int offset, boolean isTile) throws IOException
  {
    if (file == null) {
      throw new IOException("No file specified");
    }
    RandomAccessFile ranfile = new RandomAccessFileNI(file, "r");
    try {
      ranfile.seek((long)(biffEntryOff + offset + 8));
      if (isTile)
        return new int[]{FileReaderNI.readInt(ranfile), FileReaderNI.readInt(ranfile)};
      return new int[]{FileReaderNI.readInt(ranfile)};
    } finally {
      ranfile.close();
    }
  }

  private byte[] getBIFResource(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamNI(file));
    try {
      bis.skip((long)comprOff);

      InflaterInputStream iis = new InflaterInputStream(bis);
      try {
        iis.skip((long)(biffEntryOff + offset));
        FileReaderNI.readInt(iis); // Locator
        int resoff = FileReaderNI.readInt(iis);
        int size;
        byte[] tileheader = null;
        if (isTile) {
          int tilecount = FileReaderNI.readInt(iis);
          int tilesize = FileReaderNI.readInt(iis);
          size = tilecount * tilesize;
          offset += 4;
          tileheader = getTisHeader(tilecount, tilesize);
        }
        else
          size = FileReaderNI.readInt(iis);
        FileReaderNI.readShort(iis); // Type
        FileReaderNI.readShort(iis); // Unknown

        if (size > 1000000)
          blocker.setBlocked(true);

        iis.skip((long)(resoff - (biffEntryOff + offset + 16)));
        byte[] buffer = new byte[isTile ? size + tileheader.length : size];
        int index = 0;
        if (isTile) {
          System.arraycopy(tileheader, 0, buffer, index, tileheader.length);
          index += tileheader.length;
        }
        System.arraycopy(FileReaderNI.readBytes(iis, size), 0, buffer, index, size);
        return buffer;
      } finally {
        iis.close();
      }
    } finally {
      bis.close();
    }
  }

  private InputStream getBIFResourceAsStream(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamNI(file));
    bis.skip((long)comprOff);

    InflaterInputStream iis = new InflaterInputStream(bis);
    iis.skip((long)(biffEntryOff + offset));
    FileReaderNI.readInt(iis); // Locator
    int resoff = FileReaderNI.readInt(iis);
    FileReaderNI.readInt(iis); // Size
    if (isTile) {
      FileReaderNI.readInt(iis); // Tilesize
      offset += 4;
    }
    FileReaderNI.readShort(iis); // Type
    FileReaderNI.readShort(iis); // Unknown

    iis.skip((long)(resoff - (biffEntryOff + offset + 16)));
    return iis;
  }

  ///////////////////////////////
  // BIF Support
  ///////////////////////////////

  private int[] getBIFResourceInfo(int offset, boolean isTile) throws IOException
  {
    BufferedInputStream bis = new BufferedInputStream(new FileInputStreamNI(file));
    try {
      bis.skip((long)comprOff);

      InflaterInputStream iis = new InflaterInputStream(bis);
      try {
        iis.skip((long)(biffEntryOff + offset));

        FileReaderNI.readInt(iis); // Locator
        FileReaderNI.readInt(iis); // Resoff
        if (isTile) {
          int tilecount = FileReaderNI.readInt(iis);
          int tilesize = FileReaderNI.readInt(iis);
          iis.close();
          bis.close();
          return new int[]{tilecount, tilesize};
        }
        int size = FileReaderNI.readInt(iis);
        return new int[]{size};
      } finally {
        iis.close();
      }
    } finally {
      bis.close();
    }
  }

  private synchronized void readBIFFHeader(InputStream is) throws IOException
  {
    FileReaderNI.readString(is, 4); // Version
    numFiles = FileReaderNI.readInt(is);
    FileReaderNI.readInt(is); // Numtiles
    biffEntryOff = FileReaderNI.readInt(is);
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
      decompSize = FileReaderNI.readInt(is);
      compSize = FileReaderNI.readInt(is);
    }

    private byte[] getData(InputStream is, int offset, int length) throws IOException
    {
      if (buffer == null) {
        Inflater inflater = new Inflater();
        buffer = new byte[decompSize];
        inflater.setInput(FileReaderNI.readBytes(is, compSize));
        try {
          inflater.inflate(buffer);
        } catch (DataFormatException e) {
          e.printStackTrace();
          throw new IOException();
        }
      }
      if (length == decompSize)
        return buffer;
      return Arrays.copyOfRange(buffer, offset, offset + length);
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

    @Override
    public int read() throws IOException
    {
      if (size == 0)
        return -1;
      if (blockIndex == block.decompSize) { // Read new block
        block.getData(is, 0, block.decompSize);
        block = new BifcBlock(is);
        blockIndex = 0;
      }
      size--;
      byte b = block.getData(is, 0, block.decompSize)[blockIndex++];
      if (b < 0)
        return (int)b + 256;
      return (int)b;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException
    {
      if (size == 0)
        return -1;
      int remainder = len;
      while (remainder > 0 && size > 0) {
        if (blockIndex == block.decompSize) { // Read new block
          block.getData(is, 0, block.decompSize);
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

    @Override
    public int available()
    {
      return size;
    }

    @Override
    public boolean markSupported()
    {
      return false;
    }

    @Override
    public void close() throws IOException
    {
      is.close();
    }
  }
}

