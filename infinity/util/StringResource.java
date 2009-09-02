// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

import javax.swing.*;
import java.io.*;
import java.nio.charset.Charset;

public final class StringResource
{
  private static File ffile;
  private static RandomAccessFile file;
  private static String version;
  private static int maxnr, startindex;
  private static Charset charset = Charset.forName("windows-1252");

  public static Charset getCharset() {
    return charset;
  }

  public static void setCharset(String cs) {
    charset = Charset.forName(cs);
  }

  public static void close()
  {
    if (file == null) return;
    try {
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    file = null;
  }

  public static File getFile()
  {
    return ffile;
  }

  public static int getMaxIndex()
  {
    return maxnr;
  }

  public static String getResource(int index)
  {
    try {
      if (file == null)
        open();
      if (index >= maxnr || index == 0xffffffff) return null;
      byte buffer[] = null;
      if (version.equalsIgnoreCase("V1  ")) {
        index *= 0x1a;
        file.seek((long)(0x12 + index + 0x02));
        buffer = new byte[8];
      }
      else if (version.equalsIgnoreCase("V3.0")) {
        index *= 0x28;
        file.seek((long)(0x14 + index + 0x04));
        buffer = new byte[16];
      }
      file.readFully(buffer);
      if (buffer[0] == 0)
        return null;
      int max = buffer.length;
      for (int i = 0; i < buffer.length; i++) {
        if (buffer[i] == 0x00) {
          max = i;
          break;
        }
      }
      if (max != buffer.length)
        buffer = ArrayUtil.getSubArray(buffer, 0, max);
      return new String(buffer);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + ffile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return null;
  }

  public static String getStringRef(int index)
  {
    try {
      if (file == null)
        open();
      if (index >= maxnr || index < 0) return "No such index";
//      if (index == 0xffffffff) return "none";
      if (version.equalsIgnoreCase("V1  ")) {
        index *= 0x1A;
        file.seek((long)(0x12 + index + 0x12));
      }
      else if (version.equalsIgnoreCase("V3.0")) {
        index *= 0x28;
        file.seek((long)(0x14 + index + 0x1C));
      }
      int offset = startindex + Filereader.readInt(file);
      int length = Filereader.readInt(file);
      file.seek((long)offset);
      return Filereader.readString(file, length, charset);
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error reading " + ffile.getName(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    return "Error";
  }

  public static void init(File ffile)
  {
    close();
    StringResource.ffile = ffile;
  }

  private static void open() throws IOException
  {
    file = new RandomAccessFile(ffile, "r");
    file.seek((long)0x00);
    String signature = Filereader.readString(file, 4);
    if (!signature.equalsIgnoreCase("TLK "))
      throw new IOException("Not valid TLK file");
    version = Filereader.readString(file, 4);
    if (version.equalsIgnoreCase("V1  "))
      file.seek((long)0x0A);
    else if (version.equalsIgnoreCase("V3.0"))
      file.seek((long)0x0C);
    maxnr = Filereader.readInt(file);
    startindex = Filereader.readInt(file);
  }

  private StringResource(){}
}

