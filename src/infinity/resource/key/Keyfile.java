// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.util.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

public final class Keyfile
{
  public static final ImageIcon ICON_STRUCT = Icons.getIcon("RowInsertAfter16.gif");
  private static final ImageIcon ICON_TEXT = Icons.getIcon("Edit16.gif");
  private static final ImageIcon ICON_UNKNOWN = Icons.getIcon("Help16.gif");
  private static final ImageIcon ICON_SOUND = Icons.getIcon("Volume16.gif");
  private static final ImageIcon ICON_MOVIE = Icons.getIcon("Movie16.gif");
  private static final ImageIcon ICON_SCRIPT = Icons.getIcon("History16.gif");
  private static final ImageIcon ICON_IMAGE = Icons.getIcon("Color16.gif");
  private static final ImageIcon ICON_BUNDLE = Icons.getIcon("Bundle16.gif");
  private final File keyfile;
  private final IntegerHashMap<String> extmap = new IntegerHashMap<String>();
  private final Map<String, ImageIcon> resourceicons = new HashMap<String, ImageIcon>();
  private BIFFArchive currentBIFF; // Caching of last BifFile - improves performance
  private BIFFEntry currentBIFFEntry;
  private List<BIFFEntry> biffEntries;
  private String signature, version;

  public static void main(String args[]) throws Exception
  {
    // Compare two keyfiles
    Keyfile key1 = new Keyfile(new File(args[0]), ResourceFactory.ID_BG2TOB);
    Keyfile key2 = new Keyfile(new File(args[1]), ResourceFactory.ID_BG2TOB);
    ResourceTreeModel model1 = new ResourceTreeModel();
    ResourceTreeModel model2 = new ResourceTreeModel();
    key1.addBIFFResourceEntries(model1);
    key2.addBIFFResourceEntries(model2);
    model1.sort();
    model2.sort();
    List<BIFFResourceEntry> entries1 = model1.getBIFFResourceEntries();
    List<BIFFResourceEntry> entries2 = model2.getBIFFResourceEntries();
    if (!key1.equals(key2))
      System.out.println(key1 + " differs from " + key2);
    for (int i = 0; i < key1.biffEntries.size(); i++) {
      BIFFEntry entry1 = key1.biffEntries.get(i);
      BIFFEntry entry2 = key2.biffEntries.get(i);
      if (!entry1.equals(entry2))
        System.out.println(entry1 + " differs from " + entry2);
    }
    for (int i = 0; i < entries1.size(); i++) {
      ResourceEntry entry1 = entries1.get(i);
      ResourceEntry entry2 = entries2.get(i);
      if (!entry1.equals(entry2))
        System.out.println(entry1 + " differs from " + entry2);
    }
    System.exit(0);
  }

  public Keyfile(File keyfile, int currentGame)
  {
    this.keyfile = keyfile;
    resourceicons.clear();
    resourceicons.put("???", ICON_UNKNOWN);
    if (currentGame == ResourceFactory.ID_NWN) {
      extmap.put(0x001, "BMP");
      resourceicons.put("BMP", ICON_IMAGE);
      extmap.put(0x003, "TGA");
      resourceicons.put("TGA", ICON_IMAGE);
      extmap.put(0x004, "WAV");
      resourceicons.put("WAV", ICON_SOUND);
      extmap.put(0x006, "PLT");
      resourceicons.put("PLT", ICON_IMAGE);
      extmap.put(0x007, "INI");
      resourceicons.put("INI", ICON_TEXT);
      extmap.put(0x00A, "TXT");
      resourceicons.put("TXT", ICON_TEXT);
      extmap.put(0x7D2, "MDL"); // Aurora model - not supported
      extmap.put(0x7D9, "NSS");
      resourceicons.put("NSS", ICON_TEXT);
      extmap.put(0x7DA, "NCS");
      resourceicons.put("NCS", ICON_SCRIPT);
      extmap.put(0x7DC, "ARE");
      resourceicons.put("ARE", ICON_STRUCT);
      extmap.put(0x7DD, "SET");
      resourceicons.put("SET", ICON_TEXT);
      extmap.put(0x7DE, "IFO");
      resourceicons.put("IFO", ICON_STRUCT);
      extmap.put(0x7DF, "BIC");
      resourceicons.put("BIC", ICON_STRUCT);
      extmap.put(0x7E0, "WOK");
      resourceicons.put("WOK", ICON_TEXT);
      extmap.put(0x7E1, "2DA");
      resourceicons.put("2DA", ICON_TEXT);
      extmap.put(0x7E6, "TXI");
      resourceicons.put("TXI", ICON_TEXT);
      extmap.put(0x7E7, "GIT");
      resourceicons.put("GIT", ICON_STRUCT);
      extmap.put(0x7E9, "UTI");
      resourceicons.put("UTI", ICON_STRUCT);
      extmap.put(0x7EB, "UTC");
      resourceicons.put("UTC", ICON_STRUCT);
      extmap.put(0x7ED, "DLG");
      resourceicons.put("DLG", ICON_STRUCT);
      extmap.put(0x7EE, "ITP");
      resourceicons.put("ITP", ICON_STRUCT);
      extmap.put(0x7F0, "UTT");
      resourceicons.put("UTT", ICON_STRUCT);
      extmap.put(0x7F1, "DDS"); // Compressed texture file - not supported
      extmap.put(0x7F3, "UTS");
      resourceicons.put("UTS", ICON_STRUCT);
      extmap.put(0x7F4, "LTR"); // Letter-combo probability info for name generation - not supported
      extmap.put(0x7F5, "GFF");
      resourceicons.put("GFF", ICON_STRUCT);
      extmap.put(0x7F6, "FAC");
      resourceicons.put("FAC", ICON_STRUCT);
      extmap.put(0x7F8, "UTE");
      resourceicons.put("UTE", ICON_STRUCT);
      extmap.put(0x7FA, "UTD");
      resourceicons.put("UTD", ICON_STRUCT);
      extmap.put(0x7FC, "UTP");
      resourceicons.put("UTP", ICON_STRUCT);
      extmap.put(0x7FD, "DFT");
      resourceicons.put("DFT", ICON_TEXT);
      extmap.put(0x7FE, "GIC");
      resourceicons.put("GIC", ICON_STRUCT);
      extmap.put(0x7FF, "GUI");
      resourceicons.put("GUI", ICON_STRUCT);
      extmap.put(0x803, "UTM");
      resourceicons.put("UTM", ICON_STRUCT);
      extmap.put(0x804, "DWK");
      resourceicons.put("DWK", ICON_TEXT);
      extmap.put(0x805, "PWK");
      resourceicons.put("PWK", ICON_TEXT);
      extmap.put(0x808, "JRL");
      resourceicons.put("JRL", ICON_STRUCT);
      extmap.put(0x80A, "UTW");
      resourceicons.put("UTW", ICON_STRUCT);
      extmap.put(0x80C, "SSF");
      resourceicons.put("SSF", ICON_STRUCT);
      extmap.put(0x810, "NDB");
      resourceicons.put("NDB", ICON_TEXT);
      extmap.put(0x811, "PTM");
      resourceicons.put("PTM", ICON_STRUCT);
      extmap.put(0x812, "PTT");
      resourceicons.put("PTT", ICON_STRUCT);
//      extmap.put(0x270C, "INI"); Not in BioWare documentation

      resourceicons.put("BMU", ICON_SOUND);
      resourceicons.put("ERF", ICON_BUNDLE);
      resourceicons.put("HAK", ICON_BUNDLE);
      resourceicons.put("MOD", ICON_BUNDLE);
      resourceicons.put("NWM", ICON_BUNDLE);
      resourceicons.put("SAV", ICON_BUNDLE);
    }
    else if (currentGame == ResourceFactory.ID_KOTOR || currentGame == ResourceFactory.ID_KOTOR2) {
      extmap.put(0x000, "INV");
      resourceicons.put("INV", ICON_STRUCT);
      extmap.put(0x003, "TGA");
      resourceicons.put("TGA", ICON_IMAGE);
      extmap.put(0x004, "WAV");
      resourceicons.put("WAV", ICON_SOUND);
      extmap.put(0x7D2, "MDL"); // Aurora model - not supported
      extmap.put(0x7D9, "NSS");
      resourceicons.put("NSS", ICON_TEXT);
      extmap.put(0x7DA, "NCS"); 
      resourceicons.put("NCS", ICON_SCRIPT);
      extmap.put(0x7DC, "ARE");
      resourceicons.put("ARE", ICON_STRUCT);
      extmap.put(0x7DE, "IFO");
      resourceicons.put("IFO", ICON_STRUCT);
      extmap.put(0x7DF, "BIC");
      resourceicons.put("BIC", ICON_STRUCT);
      extmap.put(0x7E0, "BWM"); // ?????
      extmap.put(0x7E1, "2DA");
      resourceicons.put("2DA", ICON_TEXT);
      extmap.put(0x7E6, "TXI");
      resourceicons.put("TXI", ICON_TEXT);
      extmap.put(0x7E7, "GIT");
      resourceicons.put("GIT", ICON_STRUCT);
      extmap.put(0x7E8, "BTI");
      resourceicons.put("BTI", ICON_STRUCT);
      extmap.put(0x7E9, "UTI");
      resourceicons.put("UTI", ICON_STRUCT);
      extmap.put(0x7EA, "BTC");
      resourceicons.put("BTC", ICON_STRUCT);
      extmap.put(0x7EB, "UTC");
      resourceicons.put("UTC", ICON_STRUCT);
      extmap.put(0x7ED, "DLG");
      resourceicons.put("DLG", ICON_STRUCT);
      extmap.put(0x7EE, "ITP");
      resourceicons.put("ITP", ICON_STRUCT);
      extmap.put(0x7F0, "UTT");
      resourceicons.put("UTT", ICON_STRUCT);
      extmap.put(0x7F3, "UTS");
      resourceicons.put("UTS", ICON_STRUCT);
      extmap.put(0x7F4, "LTR"); // Letter-combo probability info for name generation - not supported
      extmap.put(0x7F6, "FAC");
      resourceicons.put("FAC", ICON_STRUCT);
      extmap.put(0x7F8, "UTE");
      resourceicons.put("UTE", ICON_STRUCT);
      extmap.put(0x7FA, "UTD");
      resourceicons.put("UTD", ICON_STRUCT);
      extmap.put(0x7FC, "UTP");
      resourceicons.put("UTP", ICON_STRUCT);
      extmap.put(0x7FF, "GUI");
      resourceicons.put("GUI", ICON_STRUCT);
      extmap.put(0x803, "UTM");
      resourceicons.put("UTM", ICON_STRUCT);
      extmap.put(0x804, "BWM"); // ??????
      extmap.put(0x805, "BWM"); // ??????
      extmap.put(0x808, "JRL");
      resourceicons.put("JRL", ICON_STRUCT);
      extmap.put(0x809, "MOD"); // MOD 1.0 - name might be incorrect
      extmap.put(0x80A, "UTW");
      resourceicons.put("UTW", ICON_STRUCT);
      extmap.put(0x80C, "SSF");
      resourceicons.put("SSF", ICON_STRUCT);
      extmap.put(0xBBB, "PTH");
      resourceicons.put("PTH", ICON_STRUCT);
      extmap.put(0xBBC, "LIP"); // ??? binary format

//      extmap.put(0xBB8, "???"); // ??? text format
//      extmap.put(0xBB9, "???"); // ??? text format
//      extmap.put(0xBBF, "???"); // ??? binary format
//      extmap.put(0xBC0, "???"); // ??? binary format

      resourceicons.put("RES", ICON_STRUCT);
      resourceicons.put("MOD", ICON_BUNDLE);
      resourceicons.put("SAV", ICON_BUNDLE);
      resourceicons.put("ERF", ICON_BUNDLE);
      resourceicons.put("RIM", ICON_BUNDLE);
    }
    else {
      extmap.put(0x001, "BMP");
      resourceicons.put("BMP", ICON_IMAGE);
      extmap.put(0x002, "MVE");
      resourceicons.put("MVE", ICON_MOVIE);
      extmap.put(0x004, "WAV");
      resourceicons.put("WAV", ICON_SOUND);
      extmap.put(0x005, "WFX");
      resourceicons.put("WFX", ICON_STRUCT);
      extmap.put(0x006, "PLT");
      resourceicons.put("PLT", ICON_IMAGE);
      extmap.put(0x3e8, "BAM");
      resourceicons.put("BAM", ICON_MOVIE);
      extmap.put(0x3e9, "WED");
      resourceicons.put("WED", ICON_STRUCT);
      extmap.put(0x3ea, "CHU");
      resourceicons.put("CHU", ICON_STRUCT);
      extmap.put(0x3eb, "TIS");
      resourceicons.put("TIS", ICON_IMAGE);
      extmap.put(0x3ec, "MOS");
      resourceicons.put("MOS", ICON_IMAGE);
      extmap.put(0x3ed, "ITM");
      resourceicons.put("ITM", ICON_STRUCT);
      extmap.put(0x3ee, "SPL");
      resourceicons.put("SPL", ICON_STRUCT);
      extmap.put(0x3ef, "BCS");
      resourceicons.put("BCS", ICON_SCRIPT);
      extmap.put(0x3f0, "IDS");
      resourceicons.put("IDS", ICON_TEXT);
      extmap.put(0x3f1, "CRE");
      resourceicons.put("CRE", ICON_STRUCT);
      extmap.put(0x3f2, "ARE");
      resourceicons.put("ARE", ICON_STRUCT);
      extmap.put(0x3f3, "DLG");
      resourceicons.put("DLG", ICON_STRUCT);
      extmap.put(0x3f4, "2DA");
      resourceicons.put("2DA", ICON_TEXT);
      extmap.put(0x3f5, "GAM");
      resourceicons.put("GAM", ICON_STRUCT);
      extmap.put(0x3f6, "STO");
      resourceicons.put("STO", ICON_STRUCT);
      extmap.put(0x3f7, "WMP");
      resourceicons.put("WMP", ICON_STRUCT);
      extmap.put(0x3f8, "EFF");
      resourceicons.put("EFF", ICON_STRUCT);
      extmap.put(0x3f9, "BS");
      resourceicons.put("BS", ICON_SCRIPT);
      extmap.put(0x3fa, "CHR");
      resourceicons.put("CHR", ICON_STRUCT);
      extmap.put(0x3fb, "VVC");
      resourceicons.put("VVC", ICON_STRUCT);
      extmap.put(0x3fc, "VEF"); // ????????
      extmap.put(0x3fd, "PRO");
      resourceicons.put("PRO", ICON_STRUCT);
      extmap.put(0x3fe, "BIO");
      resourceicons.put("BIO", ICON_TEXT);
      extmap.put(0x44c, "BAH"); // ???????
      extmap.put(0x802, "INI");
      resourceicons.put("INI", ICON_TEXT);
      extmap.put(0x803, "SRC");
      resourceicons.put("SRC", ICON_STRUCT);

      resourceicons.put("ACM", ICON_SOUND);
      resourceicons.put("MUS", ICON_SOUND);
      resourceicons.put("SAV", ICON_BUNDLE);
      resourceicons.put("TXT", ICON_TEXT);
      resourceicons.put("RES", ICON_TEXT);
      resourceicons.put("BAF", ICON_SCRIPT);
    }
  }

  public boolean equals(Object o)
  {
    if (!(o instanceof Keyfile))
      return false;
    Keyfile other = (Keyfile)o;
    return signature.equals(other.signature) && version.equals(other.version);
  }

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
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(keyfile));
    byte buffer[] = Filereader.readBytes(is, (int)keyfile.length());
    is.close();

    signature = new String(buffer, 0, 4);
    version = new String(buffer, 4, 4);
    if (!signature.equals("KEY ") || !version.equals("V1  ")) {
      JOptionPane.showMessageDialog(null, "Unsupported keyfile: " + keyfile, "Error",
                                    JOptionPane.ERROR_MESSAGE);
      throw new IOException();
    }
    int numbif = Byteconvert.convertInt(buffer, 8);
    int numres = Byteconvert.convertInt(buffer, 12);
    int bifoff = Byteconvert.convertInt(buffer, 16);
    int resoff = Byteconvert.convertInt(buffer, 20);

    biffEntries = new ArrayList<BIFFEntry>(numbif);
    if (ResourceFactory.getGameID() == ResourceFactory.ID_DEMO)
      for (int i = 0; i < numbif; i++)
        biffEntries.add(new BIFFEntry(i, buffer, bifoff + 8 * i, true));
    else
      for (int i = 0; i < numbif; i++)
        biffEntries.add(new BIFFEntry(i, buffer, bifoff + 12 * i));

    if (ResourceFactory.getGameID() == ResourceFactory.ID_NWN ||
        ResourceFactory.getGameID() == ResourceFactory.ID_KOTOR ||
        ResourceFactory.getGameID() == ResourceFactory.ID_KOTOR2) {
      for (int i = 0; i < numres; i++) {
        BIFFResourceEntry entry = new BIFFResourceEntry(buffer, resoff + 22 * i, 16);
        treemodel.addResourceEntry(entry, entry.getExtension());
      }
    }
    else {
      for (int i = 0; i < numres; i++) {
        BIFFResourceEntry entry = new BIFFResourceEntry(buffer, resoff + 14 * i, 8);
        treemodel.addResourceEntry(entry, entry.getExtension());
      }
    }
  }

  public boolean cleanUp()
  {
    closeBIFFFile();
    Set<BIFFEntry> toRemove = new HashSet<BIFFEntry>(biffEntries);
    // Determine BIFFs with no files in them
    List<BIFFResourceEntry> resourceEntries =
            ResourceFactory.getInstance().getResources().getBIFFResourceEntries();
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

  public void closeBIFFFile()
  {
    if (currentBIFF != null)
      try {
        currentBIFF.close();
        currentBIFFEntry = null;
      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  public Object[] getBIFFEntriesSorted()
  {
    List<BIFFEntry> list = new ArrayList<BIFFEntry>(biffEntries);
    Collections.sort(list);
    return list.toArray();
  }

  public BIFFEntry getBIFFEntry(int index)
  {
    return biffEntries.get(index);
  }

  public BIFFArchive getBIFFFile(BIFFEntry entry) throws IOException
  {
    if (currentBIFFEntry == entry)
      return currentBIFF; // Caching
    File file = entry.getFile();
    if (file == null)
      throw new IOException(entry + " not found");
    if (currentBIFF != null)
      currentBIFF.close();
    currentBIFFEntry = entry;
    currentBIFF = new BIFFArchive(file);
    return currentBIFF;
  }

  public String getExtension(int type)
  {
    return extmap.get(type);
  }

  public int getExtensionType(String extension)
  {
    int keys[] = extmap.keys();
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
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(keyfile));
    int bifoff = 0x18;
    int offset = bifoff + 0x0c * biffEntries.size();
    for (int i = 0; i < biffEntries.size(); i++)
      offset += biffEntries.get(i).updateOffset(offset);
    int resoff = offset;

    List<BIFFResourceEntry> resourceentries =
            ResourceFactory.getInstance().getResources().getBIFFResourceEntries();

    Filewriter.writeString(os, signature, 4);
    Filewriter.writeString(os, version, 4);
    Filewriter.writeInt(os, biffEntries.size());
    Filewriter.writeInt(os, resourceentries.size());
    Filewriter.writeInt(os, bifoff);
    Filewriter.writeInt(os, resoff);

    for (int i = 0; i < biffEntries.size(); i++)
      biffEntries.get(i).write(os);
    for (int i = 0; i < biffEntries.size(); i++)
      biffEntries.get(i).writeString(os);

    for (int i = 0; i < resourceentries.size(); i++)
      resourceentries.get(i).write(os);
    os.close();
  }

  private void removeBIFFEntry(BIFFEntry entry)
  {
    System.out.println("Removing " + entry);
    int index = biffEntries.indexOf(entry);
    // Remove bogus BIFFResourceEntries
    ResourceTreeModel resources = ResourceFactory.getInstance().getResources();
    for (final BIFFResourceEntry resourceEntry : resources.getBIFFResourceEntries()) {
      if (resourceEntry.getBIFFEntry() == entry)
        resources.removeResourceEntry(resourceEntry);
      else
        resourceEntry.adjustSourceIndex(index);     // Update relevant BIFFResourceEntries
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

