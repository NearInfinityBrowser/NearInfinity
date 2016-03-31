// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.IntegerHashMap;
import org.infinity.util.io.StreamUtils;

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

  private static final String KEY_SIGNATURE = "KEY ";
  private static final String KEY_VERSION   = "V1  ";

  private final Path keyFile;
  private final IntegerHashMap<String> extMap = new IntegerHashMap<String>();
  private final Map<String, ImageIcon> resourceIcons = new HashMap<String, ImageIcon>();
  private List<BIFFEntry> biffEntries;

  /** Returns a list of BIFFEntry objects referenced by the specified key file. */
  public static List<BIFFEntry> getBiffEntries(Path keyFile) throws Exception
  {
    ArrayList<BIFFEntry> retVal = new ArrayList<>();
    try (SeekableByteChannel ch = Files.newByteChannel(keyFile, StandardOpenOption.READ)) {
      ByteBuffer buffer = StreamUtils.getByteBuffer((int)ch.size());
      if (ch.read(buffer) < ch.size()) {
        throw new IOException();
      }
      String sig = StreamUtils.readString(buffer, 0, 4);
      String ver = StreamUtils.readString(buffer, 4, 4);
      if (!sig.equals(KEY_SIGNATURE) || !ver.equals(KEY_VERSION)) {
        throw new IOException("Unsupported key file: " + keyFile.toString());
      }
      int numBif = buffer.getInt(0x08);
      int ofsBif = buffer.getInt(0x10);

      retVal.ensureCapacity(numBif);
      for (int i = 0; i < numBif; i++) {
        retVal.add(new BIFFEntry(keyFile, i, buffer, ofsBif + 12*i));
      }
    }
    return retVal;
  }

  /** Returns a list of BIFFResourceEntry objects listed in the specified key file. */
  public static List<BIFFResourceEntry> getBiffResources(Path keyFile) throws Exception
  {
    ArrayList<BIFFResourceEntry> retVal = new ArrayList<>();
    try (SeekableByteChannel ch = Files.newByteChannel(keyFile, StandardOpenOption.READ)) {
      ByteBuffer buffer = StreamUtils.getByteBuffer((int)ch.size());
      if (ch.read(buffer) < ch.size()) {
        throw new IOException();
      }
      String sig = StreamUtils.readString(buffer, 0, 4);
      String ver = StreamUtils.readString(buffer, 4, 4);
      if (!sig.equals(KEY_SIGNATURE) || !ver.equals(KEY_VERSION)) {
        throw new IOException("Unsupported key file: " + keyFile.toString());
      }
      int numRes = buffer.getInt(0x0c);
      int ofsRes = buffer.getInt(0x14);

      retVal.ensureCapacity(numRes);
      for (int i = 0; i < numRes; i++) {
        retVal.add(new BIFFResourceEntry(buffer, ofsRes + 14 * i, 8));
      }
    }
    return retVal;
  }

  public Keyfile(Path keyFile) throws IOException
  {
    if (keyFile == null) {
      throw new NullPointerException("No keyfile specified");
    }
    if (!Files.isRegularFile(keyFile)) {
      throw new IOException("Keyfile not found");
    }
    this.keyFile = keyFile;
    resourceIcons.clear();
    resourceIcons.put("???", ICON_UNKNOWN);
    extMap.put(TYPE_BMP, "BMP");
    resourceIcons.put("BMP", ICON_IMAGE);
    extMap.put(TYPE_MVE, "MVE");
    resourceIcons.put("MVE", ICON_MOVIE);
    extMap.put(TYPE_WAV, "WAV");
    resourceIcons.put("WAV", ICON_SOUND);
    extMap.put(TYPE_WFX, "WFX");
    resourceIcons.put("WFX", ICON_STRUCT);
    extMap.put(TYPE_PLT, "PLT");
    resourceIcons.put("PLT", ICON_IMAGE);
    extMap.put(TYPE_BAM, "BAM");
    resourceIcons.put("BAM", ICON_MOVIE);
    extMap.put(TYPE_WED, "WED");
    resourceIcons.put("WED", ICON_STRUCT);
    extMap.put(TYPE_CHU, "CHU");
    resourceIcons.put("CHU", ICON_STRUCT);
    extMap.put(TYPE_TIS, "TIS");
    resourceIcons.put("TIS", ICON_IMAGE);
    extMap.put(TYPE_MOS, "MOS");
    resourceIcons.put("MOS", ICON_IMAGE);
    extMap.put(TYPE_ITM, "ITM");
    resourceIcons.put("ITM", ICON_STRUCT);
    extMap.put(TYPE_SPL, "SPL");
    resourceIcons.put("SPL", ICON_STRUCT);
    extMap.put(TYPE_BCS, "BCS");
    resourceIcons.put("BCS", ICON_SCRIPT);
    extMap.put(TYPE_IDS, "IDS");
    resourceIcons.put("IDS", ICON_TEXT);
    extMap.put(TYPE_CRE, "CRE");
    resourceIcons.put("CRE", ICON_STRUCT);
    extMap.put(TYPE_ARE, "ARE");
    resourceIcons.put("ARE", ICON_STRUCT);
    extMap.put(TYPE_DLG, "DLG");
    resourceIcons.put("DLG", ICON_STRUCT);
    extMap.put(TYPE_2DA, "2DA");
    resourceIcons.put("2DA", ICON_TEXT);
    extMap.put(TYPE_GAM, "GAM");
    resourceIcons.put("GAM", ICON_STRUCT);
    extMap.put(TYPE_STO, "STO");
    resourceIcons.put("STO", ICON_STRUCT);
    extMap.put(TYPE_WMP, "WMP");
    resourceIcons.put("WMP", ICON_STRUCT);
    extMap.put(TYPE_EFF, "EFF");
    resourceIcons.put("EFF", ICON_STRUCT);
    extMap.put(TYPE_BS, "BS");
    resourceIcons.put("BS", ICON_SCRIPT);
    extMap.put(TYPE_CHR, "CHR");
    resourceIcons.put("CHR", ICON_STRUCT);
    extMap.put(TYPE_VVC, "VVC");
    resourceIcons.put("VVC", ICON_STRUCT);
    extMap.put(TYPE_VEF, "VEF");
    resourceIcons.put("VEF", ICON_STRUCT);
    extMap.put(TYPE_PRO, "PRO");
    resourceIcons.put("PRO", ICON_STRUCT);
    extMap.put(TYPE_BIO, "BIO");
    resourceIcons.put("BIO", ICON_TEXT);
    extMap.put(TYPE_WBM, "WBM");
    resourceIcons.put("WBM", ICON_MOVIE);
    extMap.put(TYPE_BAH, "BAH"); // ???????
    extMap.put(TYPE_INI, "INI");
    resourceIcons.put("INI", ICON_TEXT);
    extMap.put(TYPE_SRC, "SRC");
    resourceIcons.put("SRC", ICON_STRUCT);
    extMap.put(TYPE_FNT, "FNT");
    resourceIcons.put("FNT", ICON_IMAGE);
    extMap.put(TYPE_GUI, "GUI");
    resourceIcons.put("GUI", ICON_TEXT);
    extMap.put(TYPE_SQL, "SQL");
    resourceIcons.put("SQL", ICON_TEXT);
    extMap.put(TYPE_PVRZ, "PVRZ");
    resourceIcons.put("PVRZ", ICON_IMAGE);
    extMap.put(TYPE_GLSL, "GLSL");
    resourceIcons.put("GLSL", ICON_TEXT);
    extMap.put(TYPE_TOT, "TOT");
    resourceIcons.put("TOT", ICON_STRUCT);
    extMap.put(TYPE_TOH, "TOH");
    resourceIcons.put("TOH", ICON_STRUCT);
    extMap.put(TYPE_MENU, "MENU");
    resourceIcons.put("MENU", ICON_SCRIPT);
    extMap.put(TYPE_LUA, "LUA");
    resourceIcons.put("LUA", ICON_SCRIPT);
    extMap.put(TYPE_TTF, "TTF");
    resourceIcons.put("TTF", ICON_IMAGE);
    extMap.put(TYPE_PNG, "PNG");
    resourceIcons.put("PNG", ICON_IMAGE);
    extMap.put(TYPE_MUS, "MUS");
    resourceIcons.put("MUS", ICON_SOUND);
    extMap.put(TYPE_ACM, "ACM");
    resourceIcons.put("ACM", ICON_SOUND);
    resourceIcons.put("SAV", ICON_BUNDLE);
    resourceIcons.put("TXT", ICON_TEXT);
    resourceIcons.put("RES", ICON_TEXT);
    resourceIcons.put("BAF", ICON_SCRIPT);
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    } else if (o instanceof Keyfile) {
      Keyfile other = (Keyfile)o;
      return (other.keyFile.equals(keyFile));
    } else {
      return false;
    }
  }

  @Override
  public String toString()
  {
    return keyFile.toString();
  }

  public void addBIFFEntry(BIFFEntry entry)
  {
    biffEntries.add(entry);
    entry.setIndex(biffEntries.size() - 1);
  }

  public void addBIFFResourceEntries(ResourceTreeModel treemodel) throws Exception
  {
    List<BIFFResourceEntry> resEntries = null;
    try {
      biffEntries = getBiffEntries(keyFile);
      resEntries = getBiffResources(keyFile);
    } catch (Exception e) {
      String msg = e.getMessage();
      if (msg == null || msg.length() == 0) {
        msg = "Error loading BIFF resources.";
      }
      JOptionPane.showMessageDialog(null, msg, "Error",
                                    JOptionPane.ERROR_MESSAGE);
      throw new IOException();
    }
    for (final BIFFResourceEntry entry: resEntries) {
      treemodel.addResourceEntry(entry, entry.getExtension(), true);
    }
    if (BrowserMenuBar.getInstance() != null && BrowserMenuBar.getInstance().cacheBIFFs()) {
      cacheBIFFs();
    }
  }

  public boolean cleanUp()
  {
    closeBIFFFiles();
    Set<BIFFEntry> toRemove = new HashSet<BIFFEntry>(biffEntries);
    // Determine BIFFs with no files in them
    List<BIFFResourceEntry> resourceEntries = ResourceFactory.getResources().getBIFFResourceEntries();
    for (int i = 0; i < resourceEntries.size(); i++) {
      BIFFResourceEntry entry = resourceEntries.get(i);
      toRemove.remove(entry.getBIFFEntry());
    }
    // Delete these BIFFs
    for (final BIFFEntry entry : toRemove) {
      Path file = entry.getPath();
      System.out.println("Deleting " + file);
      if (file != null) {
        try {
          Files.deleteIfExists(file);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    // Determine non-existant BIFFs
    for (int i = 0; i < biffEntries.size(); i++) {
      BIFFEntry entry = biffEntries.get(i);
      if (entry.getPath() == null) {
        toRemove.add(entry);
      }
    }
    if (toRemove.isEmpty()) {
      return false;
    }

    // Remove bogus BIFFs from keyfile
    for (final BIFFEntry entry : toRemove) {
      removeBIFFEntry(entry);
    }
    return true;
  }

  public void closeBIFFFiles()
  {
    AbstractBIFFReader.resetCache();
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

  public AbstractBIFFReader getBIFFFile(BIFFEntry entry) throws Exception
  {
    if (entry == null) {
      return null;
    } else if (entry.getPath() == null) {
      throw new IOException(entry + " not found");
    } else {
      return AbstractBIFFReader.open(entry.getPath());
    }
  }

  public String getExtension(int type)
  {
    return extMap.get(type);
  }

  public int getExtensionType(String extension)
  {
    int[] keys = extMap.keys();
    for (int type : keys) {
      if (extMap.get(type).equalsIgnoreCase(extension))
        return type;
    }
    return -1;
  }

  public ImageIcon getIcon(String extension)
  {
    ImageIcon icon = resourceIcons.get(extension);
    if (icon == null)
      return resourceIcons.get("???");
    return icon;
  }

  public void write() throws IOException
  {
    try (OutputStream os = StreamUtils.getOutputStream(keyFile)) {
      int bifoff = 0x18;
      int offset = bifoff + 0x0c * biffEntries.size();
      for (int i = 0; i < biffEntries.size(); i++) {
        offset += biffEntries.get(i).updateOffset(offset);
      }
      int resoff = offset;

      List<BIFFResourceEntry> resourceentries = ResourceFactory.getResources().getBIFFResourceEntries();

      StreamUtils.writeString(os, KEY_SIGNATURE, 4);
      StreamUtils.writeString(os, KEY_VERSION, 4);
      StreamUtils.writeInt(os, biffEntries.size());
      StreamUtils.writeInt(os, resourceentries.size());
      StreamUtils.writeInt(os, bifoff);
      StreamUtils.writeInt(os, resoff);

      for (int i = 0; i < biffEntries.size(); i++) {
        biffEntries.get(i).write(os);
      }
      for (int i = 0; i < biffEntries.size(); i++) {
        biffEntries.get(i).writeString(os);
      }

      for (int i = 0; i < resourceentries.size(); i++) {
        resourceentries.get(i).write(os);
      }
    }
  }

  // caches all BIFF files referenced in the current KEY file
  private void cacheBIFFs()
  {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        if (biffEntries != null) {
          for (final BIFFEntry entry: biffEntries) {
            if (entry != null) {
              Path biffPath = entry.getPath();
              if (biffPath != null && Files.isRegularFile(biffPath)) {
                try {
                  AbstractBIFFReader.open(biffPath);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    });
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

