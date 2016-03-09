// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.DynamicArray;
import org.infinity.util.IntegerHashMap;
import org.infinity.util.io.FileInputStreamNI;
import org.infinity.util.io.FileOutputStreamNI;
import org.infinity.util.io.FileReaderNI;
import org.infinity.util.io.FileWriterNI;

public final class Keyfile
{
  public static final ImageIcon ICON_STRUCT = Icons.getIcon(Icons.ICON_ROW_INSERT_AFTER_16);

  public static final int TYPE_BMP    = 0x001;
  public static final int TYPE_MVE    = 0x002;
  public static final int TYPE_WAV    = 0x004;
  public static final int TYPE_WFX    = 0x005;
  public static final int TYPE_PLT    = 0x006;
  public static final int TYPE_BAM    = 0x3e8;
  public static final int TYPE_WED    = 0x3e9;
  public static final int TYPE_CHU    = 0x3ea;
  public static final int TYPE_TIS    = 0x3eb;
  public static final int TYPE_MOS    = 0x3ec;
  public static final int TYPE_ITM    = 0x3ed;
  public static final int TYPE_SPL    = 0x3ee;
  public static final int TYPE_BCS    = 0x3ef;
  public static final int TYPE_IDS    = 0x3f0;
  public static final int TYPE_CRE    = 0x3f1;
  public static final int TYPE_ARE    = 0x3f2;
  public static final int TYPE_DLG    = 0x3f3;
  public static final int TYPE_2DA    = 0x3f4;
  public static final int TYPE_GAM    = 0x3f5;
  public static final int TYPE_STO    = 0x3f6;
  public static final int TYPE_WMP    = 0x3f7;
  public static final int TYPE_EFF    = 0x3f8;
  public static final int TYPE_BS     = 0x3f9;
  public static final int TYPE_CHR    = 0x3fa;
  public static final int TYPE_VVC    = 0x3fb;
  public static final int TYPE_VEF    = 0x3fc;
  public static final int TYPE_PRO    = 0x3fd;
  public static final int TYPE_BIO    = 0x3fe;
  public static final int TYPE_WBM    = 0x3ff;
  public static final int TYPE_FNT    = 0x400;
  public static final int TYPE_GUI    = 0x402;
  public static final int TYPE_SQL    = 0x403;
  public static final int TYPE_PVRZ   = 0x404;
  public static final int TYPE_GLSL   = 0x405;
  public static final int TYPE_TOT    = 0x406;
  public static final int TYPE_TOH    = 0x407;
  public static final int TYPE_MENU   = 0x408;
  public static final int TYPE_LUA    = 0x409;
  public static final int TYPE_TTF    = 0x40a;
  public static final int TYPE_PNG    = 0x40b;
  public static final int TYPE_BAH    = 0x44c;
  public static final int TYPE_INI    = 0x802;
  public static final int TYPE_SRC    = 0x803;
  public static final int TYPE_MUS    = 0xffe;  // not in bif?
  public static final int TYPE_ACM    = 0xfff;  // not in bif?

  private static final ImageIcon ICON_TEXT = Icons.getIcon(Icons.ICON_EDIT_16);
  private static final ImageIcon ICON_UNKNOWN = Icons.getIcon(Icons.ICON_HELP_16);
  private static final ImageIcon ICON_SOUND = Icons.getIcon(Icons.ICON_VOLUME_16);
  private static final ImageIcon ICON_MOVIE = Icons.getIcon(Icons.ICON_MOVIE_16);
  private static final ImageIcon ICON_SCRIPT = Icons.getIcon(Icons.ICON_HISTORY_16);
  private static final ImageIcon ICON_IMAGE = Icons.getIcon(Icons.ICON_COLOR_16);
  private static final ImageIcon ICON_BUNDLE = Icons.getIcon(Icons.ICON_BUNDLE_16);
  private final File keyfile;
  private final IntegerHashMap<String> extmap = new IntegerHashMap<String>();
  private final Map<String, ImageIcon> resourceicons = new HashMap<String, ImageIcon>();
  private BIFFArchive currentBIFF; // Caching of last BifFile - improves performance
  private BIFFEntry currentBIFFEntry;
  private List<BIFFEntry> biffEntries;
  private String signature, version;

  public Keyfile(File keyfile)
  {
    this.keyfile = keyfile;
    resourceicons.clear();
    resourceicons.put("???", ICON_UNKNOWN);
    extmap.put(TYPE_BMP, "BMP");
    resourceicons.put("BMP", ICON_IMAGE);
    extmap.put(TYPE_MVE, "MVE");
    resourceicons.put("MVE", ICON_MOVIE);
    extmap.put(TYPE_WAV, "WAV");
    resourceicons.put("WAV", ICON_SOUND);
    extmap.put(TYPE_WFX, "WFX");
    resourceicons.put("WFX", ICON_STRUCT);
    extmap.put(TYPE_PLT, "PLT");
    resourceicons.put("PLT", ICON_IMAGE);
    extmap.put(TYPE_BAM, "BAM");
    resourceicons.put("BAM", ICON_MOVIE);
    extmap.put(TYPE_WED, "WED");
    resourceicons.put("WED", ICON_STRUCT);
    extmap.put(TYPE_CHU, "CHU");
    resourceicons.put("CHU", ICON_STRUCT);
    extmap.put(TYPE_TIS, "TIS");
    resourceicons.put("TIS", ICON_IMAGE);
    extmap.put(TYPE_MOS, "MOS");
    resourceicons.put("MOS", ICON_IMAGE);
    extmap.put(TYPE_ITM, "ITM");
    resourceicons.put("ITM", ICON_STRUCT);
    extmap.put(TYPE_SPL, "SPL");
    resourceicons.put("SPL", ICON_STRUCT);
    extmap.put(TYPE_BCS, "BCS");
    resourceicons.put("BCS", ICON_SCRIPT);
    extmap.put(TYPE_IDS, "IDS");
    resourceicons.put("IDS", ICON_TEXT);
    extmap.put(TYPE_CRE, "CRE");
    resourceicons.put("CRE", ICON_STRUCT);
    extmap.put(TYPE_ARE, "ARE");
    resourceicons.put("ARE", ICON_STRUCT);
    extmap.put(TYPE_DLG, "DLG");
    resourceicons.put("DLG", ICON_STRUCT);
    extmap.put(TYPE_2DA, "2DA");
    resourceicons.put("2DA", ICON_TEXT);
    extmap.put(TYPE_GAM, "GAM");
    resourceicons.put("GAM", ICON_STRUCT);
    extmap.put(TYPE_STO, "STO");
    resourceicons.put("STO", ICON_STRUCT);
    extmap.put(TYPE_WMP, "WMP");
    resourceicons.put("WMP", ICON_STRUCT);
    extmap.put(TYPE_EFF, "EFF");
    resourceicons.put("EFF", ICON_STRUCT);
    extmap.put(TYPE_BS, "BS");
    resourceicons.put("BS", ICON_SCRIPT);
    extmap.put(TYPE_CHR, "CHR");
    resourceicons.put("CHR", ICON_STRUCT);
    extmap.put(TYPE_VVC, "VVC");
    resourceicons.put("VVC", ICON_STRUCT);
    extmap.put(TYPE_VEF, "VEF");
    resourceicons.put("VEF", ICON_STRUCT);
    extmap.put(TYPE_PRO, "PRO");
    resourceicons.put("PRO", ICON_STRUCT);
    extmap.put(TYPE_BIO, "BIO");
    resourceicons.put("BIO", ICON_TEXT);
    extmap.put(TYPE_WBM, "WBM");
    resourceicons.put("WBM", ICON_MOVIE);
    extmap.put(TYPE_BAH, "BAH"); // ???????
    extmap.put(TYPE_INI, "INI");
    resourceicons.put("INI", ICON_TEXT);
    extmap.put(TYPE_SRC, "SRC");
    resourceicons.put("SRC", ICON_STRUCT);
    extmap.put(TYPE_FNT, "FNT");
    resourceicons.put("FNT", ICON_IMAGE);
    extmap.put(TYPE_GUI, "GUI");
    resourceicons.put("GUI", ICON_TEXT);
    extmap.put(TYPE_SQL, "SQL");
    resourceicons.put("SQL", ICON_TEXT);
    extmap.put(TYPE_PVRZ, "PVRZ");
    resourceicons.put("PVRZ", ICON_IMAGE);
    extmap.put(TYPE_GLSL, "GLSL");
    resourceicons.put("GLSL", ICON_TEXT);
    extmap.put(TYPE_TOT, "TOT");
    resourceicons.put("TOT", ICON_STRUCT);
    extmap.put(TYPE_TOH, "TOH");
    resourceicons.put("TOH", ICON_STRUCT);
    extmap.put(TYPE_MENU, "MENU");
    resourceicons.put("MENU", ICON_SCRIPT);
    extmap.put(TYPE_LUA, "LUA");
    resourceicons.put("LUA", ICON_SCRIPT);
    extmap.put(TYPE_TTF, "TTF");
    resourceicons.put("TTF", ICON_IMAGE);
    extmap.put(TYPE_PNG, "PNG");
    resourceicons.put("PNG", ICON_IMAGE);
    extmap.put(TYPE_MUS, "MUS");
    resourceicons.put("MUS", ICON_SOUND);
    extmap.put(TYPE_ACM, "ACM");
    resourceicons.put("ACM", ICON_SOUND);
    resourceicons.put("SAV", ICON_BUNDLE);
    resourceicons.put("TXT", ICON_TEXT);
    resourceicons.put("RES", ICON_TEXT);
    resourceicons.put("BAF", ICON_SCRIPT);
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof Keyfile))
      return false;
    Keyfile other = (Keyfile)o;
    return signature.equals(other.signature) && version.equals(other.version);
  }

  @Override
  public String toString()
  {
    return keyfile.toString();
  }

  public void addBIFFEntry(BIFFEntry entry)
  {
    biffEntries.add(entry);
    entry.setIndex(biffEntries.size() - 1);
  }

  public void addBIFFResourceEntries(ResourceTreeModel treemodel) throws Exception
  {
    BufferedInputStream is = new BufferedInputStream(new FileInputStreamNI(keyfile));
    byte buffer[] = FileReaderNI.readBytes(is, (int)keyfile.length());
    is.close();

    signature = new String(buffer, 0, 4);
    version = new String(buffer, 4, 4);
    if (!signature.equals("KEY ") || !version.equals("V1  ")) {
      JOptionPane.showMessageDialog(null, "Unsupported keyfile: " + keyfile, "Error",
                                    JOptionPane.ERROR_MESSAGE);
      throw new IOException();
    }
    int numbif = DynamicArray.getInt(buffer, 8);
    int numres = DynamicArray.getInt(buffer, 12);
    int bifoff = DynamicArray.getInt(buffer, 16);
    int resoff = DynamicArray.getInt(buffer, 20);

    biffEntries = new ArrayList<BIFFEntry>(numbif);
    for (int i = 0; i < numbif; i++)
      biffEntries.add(new BIFFEntry(i, buffer, bifoff + 12 * i));

    for (int i = 0; i < numres; i++) {
      BIFFResourceEntry entry = new BIFFResourceEntry(buffer, resoff + 14 * i, 8);
      treemodel.addResourceEntry(entry, entry.getExtension());
    }
  }

  public boolean cleanUp()
  {
    closeBIFFFile();
    Set<BIFFEntry> toRemove = new HashSet<BIFFEntry>(biffEntries);
    // Determine BIFFs with no files in them
    List<BIFFResourceEntry> resourceEntries =
            ResourceFactory.getResources().getBIFFResourceEntries();
    for (int i = 0; i < resourceEntries.size(); i++) {
      BIFFResourceEntry entry = resourceEntries.get(i);
      toRemove.remove(entry.getBIFFEntry());
    }
    // Delete these BIFFs
    for (final BIFFEntry entry : toRemove) {
      File file = entry.getFile();
      System.out.println("Deleting " + file);
      if (file != null)
        file.delete();
    }
    // Determine non-existant BIFFs
    for (int i = 0; i < biffEntries.size(); i++) {
      BIFFEntry entry = biffEntries.get(i);
      if (entry.getFile() == null)
        toRemove.add(entry);
    }
    if (toRemove.isEmpty())
      return false;
    // Remove bugus BIFFs from keyfile
    for (final BIFFEntry entry : toRemove)
      removeBIFFEntry(entry);
    return true;
  }

  public synchronized void closeBIFFFile()
  {
    if (currentBIFF != null) {
      currentBIFF = null;
      currentBIFFEntry = null;
    }
  }

  public BIFFEntry[] getBIFFEntriesSorted()
  {
    List<BIFFEntry> list = new ArrayList<BIFFEntry>(biffEntries);
    Collections.sort(list);
    return list.toArray(new BIFFEntry[list.size()]);
  }

  public BIFFEntry getBIFFEntry(int index)
  {
    return biffEntries.get(index);
  }

  public synchronized BIFFArchive getBIFFFile(BIFFEntry entry) throws IOException
  {
    if (entry == null) {
      return null;
    } else if (entry.equals(currentBIFFEntry)) {
      return currentBIFF; // Caching
    } else {
      File file = entry.getFile();
      if (file == null) {
        throw new IOException(entry + " not found");
      }
      currentBIFFEntry = entry;
      currentBIFF = new BIFFArchive(file);
      return currentBIFF;
    }
  }

  public String getExtension(int type)
  {
    return extmap.get(type);
  }

  public int getExtensionType(String extension)
  {
    int[] keys = extmap.keys();
    for (int type : keys) {
      if (extmap.get(type).equalsIgnoreCase(extension))
        return type;
    }
    return -1;
  }

  public ImageIcon getIcon(String extension)
  {
    ImageIcon icon = resourceicons.get(extension);
    if (icon == null)
      return resourceicons.get("???");
    return icon;
  }

  public void write() throws IOException
  {
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStreamNI(keyfile));
    try {
      int bifoff = 0x18;
      int offset = bifoff + 0x0c * biffEntries.size();
      for (int i = 0; i < biffEntries.size(); i++) {
        offset += biffEntries.get(i).updateOffset(offset);
      }
      int resoff = offset;

      List<BIFFResourceEntry> resourceentries = ResourceFactory.getResources().getBIFFResourceEntries();

      FileWriterNI.writeString(os, signature, 4);
      FileWriterNI.writeString(os, version, 4);
      FileWriterNI.writeInt(os, biffEntries.size());
      FileWriterNI.writeInt(os, resourceentries.size());
      FileWriterNI.writeInt(os, bifoff);
      FileWriterNI.writeInt(os, resoff);

      for (int i = 0; i < biffEntries.size(); i++) {
        biffEntries.get(i).write(os);
      }
      for (int i = 0; i < biffEntries.size(); i++) {
        biffEntries.get(i).writeString(os);
      }

      for (int i = 0; i < resourceentries.size(); i++) {
        resourceentries.get(i).write(os);
      }
    } finally {
      os.close();
    }
  }

  private void removeBIFFEntry(BIFFEntry entry)
  {
    System.out.println("Removing " + entry);
    int index = biffEntries.indexOf(entry);
    // Remove bogus BIFFResourceEntries
    ResourceTreeModel resources = ResourceFactory.getResources();
    for (final BIFFResourceEntry resourceEntry : resources.getBIFFResourceEntries()) {
      if (resourceEntry.getBIFFEntry() == entry) {
        resources.removeResourceEntry(resourceEntry);
      } else {
        resourceEntry.adjustSourceIndex(index);     // Update relevant BIFFResourceEntries
      }
    }
    // Remove BIFFEntry
    biffEntries.remove(entry);
    // Update relevant BIFFEntries
    for (int i = index; i < biffEntries.size(); i++) {
      BIFFEntry e = biffEntries.get(i);
      e.setIndex(i);
    }
  }
}

