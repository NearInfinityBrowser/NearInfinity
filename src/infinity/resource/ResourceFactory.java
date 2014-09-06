// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.NearInfinity;
import infinity.datatype.Kit2daBitmap;
import infinity.datatype.Song2daBitmap;
import infinity.gui.BrowserMenuBar;
import infinity.gui.ChildFrame;
import infinity.gui.IdsBrowser;
import infinity.resource.are.AreResource;
import infinity.resource.bcs.BafResource;
import infinity.resource.bcs.BcsResource;
import infinity.resource.bcs.Compiler;
import infinity.resource.chu.ChuResource;
import infinity.resource.cre.CreResource;
import infinity.resource.dlg.DlgResource;
import infinity.resource.gam.GamResource;
import infinity.resource.graphics.BamResource;
import infinity.resource.graphics.BmpResource;
import infinity.resource.graphics.MosResource;
import infinity.resource.graphics.PltResource;
import infinity.resource.graphics.PvrzResource;
import infinity.resource.graphics.TisResource;
import infinity.resource.itm.ItmResource;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.Keyfile;
import infinity.resource.key.ResourceEntry;
import infinity.resource.key.ResourceTreeFolder;
import infinity.resource.key.ResourceTreeModel;
import infinity.resource.mus.MusResource;
import infinity.resource.other.EffResource;
import infinity.resource.other.FntResource;
import infinity.resource.other.UnknownResource;
import infinity.resource.other.VvcResource;
import infinity.resource.other.WfxResource;
import infinity.resource.pro.ProResource;
import infinity.resource.sav.SavResource;
import infinity.resource.sound.SoundResource;
import infinity.resource.spl.SplResource;
import infinity.resource.src.SrcResource;
import infinity.resource.sto.StoResource;
import infinity.resource.text.PlainTextResource;
import infinity.resource.to.TohResource;
import infinity.resource.to.TotResource;
import infinity.resource.var.VarResource;
import infinity.resource.vef.VefResource;
import infinity.resource.video.MveResource;
import infinity.resource.video.WbmResource;
import infinity.resource.wed.WedResource;
import infinity.resource.wmp.WmpResource;
import infinity.util.Decryptor;
import infinity.util.IdsMapCache;
import infinity.util.NIFile;
import infinity.util.StringResource;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

public final class ResourceFactory
{
  public static final String OVERRIDEFOLDER = "Override";
  public static final int ID_UNKNOWNGAME = 0, ID_BG1 = 1, ID_BG1TOTSC = 2, ID_TORMENT = 3, ID_ICEWIND = 4;
  public static final int ID_ICEWINDHOW = 5, ID_ICEWINDHOWTOT = 6, ID_BG2 = 7, ID_BG2TOB = 8, ID_NWN = 9;
  public static final int ID_ICEWIND2 = 10, ID_KOTOR = 11, ID_TUTU = 12, ID_DEMO = 13, ID_KOTOR2 = 14;
  public static final int ID_BGEE = 15, ID_BG2EE = 16, ID_IWDEE = 17;
  private static File rootDir, langRoot, userRoot;
  private static File[] rootDirs;
  private static final GameConfig[] games;
  private static Keyfile keyfile;
  private static ResourceFactory factory;
  private static final String DIALOGFILENAME = "dialog.tlk";
  private static int currentGame;
  private static String eeLang;
  private File[] biffDirs;
  private JFileChooser fc;
  private ResourceTreeModel treeModel;

  static
  {
    String bgdirs[] = {"Characters", "MPSave", "Music", "Portraits", "Save", "Screenshots",
                       "Scripts", "ScrnShot", "Sounds", "Temp", "TempSave"};
    String bgeeDirs[] = {"BPSave", "Characters", "Movies", "MPSave", "MPBPSave", "Music", "Portraits",
                         "Save", "Sounds", "ScrnShot", "Scripts", "Temp", "TempSave"};
    String iwdeeDirs[] = {"Characters", "Movies", "MPSave", "Music", "Portraits", "Save",
                          "Sounds", "ScrnShot", "Scripts", "Temp", "TempSave"};

    games = new GameConfig[18];
    games[ID_UNKNOWNGAME] = new GameConfig("Unknown game", "baldur.ini", bgdirs);
    games[ID_BG1] = new GameConfig("Baldur's Gate", "baldur.ini", bgdirs);
    games[ID_BG1TOTSC] = new GameConfig("Baldur's Gate - Tales of the Sword Coast",
                                        "baldur.ini", bgdirs);
    games[ID_TORMENT] = new GameConfig("Planescape: Torment", "torment.ini",
                                       new String[]{"Music", "Save", "Temp"});
    games[ID_ICEWIND] = new GameConfig("Icewind Dale", "icewind.ini", bgdirs);
    games[ID_ICEWINDHOW] = new GameConfig("Icewind Dale - Heart of Winter", "icewind.ini", bgdirs);
    games[ID_ICEWINDHOWTOT] = new GameConfig("Icewind Dale - Trials of the Luremaster",
                                             "icewind.ini", bgdirs);
    games[ID_BG2] = new GameConfig("Baldur's Gate 2 - Shadows of Amn", "baldur.ini", bgdirs);
    games[ID_BG2TOB] = new GameConfig("Baldur's Gate 2 - Throne of Bhaal", "baldur.ini", bgdirs);
    games[ID_ICEWIND2] = new GameConfig("Icewind Dale 2", "icewind2.ini", bgdirs);
    games[ID_NWN] = games[ID_UNKNOWNGAME];
    games[ID_KOTOR] = games[ID_UNKNOWNGAME];
    games[ID_TUTU] = games[ID_UNKNOWNGAME];
    games[ID_DEMO] = games[ID_UNKNOWNGAME];
    games[ID_KOTOR2] = games[ID_UNKNOWNGAME];
    games[ID_BGEE] = new GameConfig("Baldur's Gate - Enhanced Edition", "baldur.ini", bgeeDirs);
    games[ID_BG2EE] = new GameConfig("Baldur's Gate II - Enhanced Edition", "baldur.ini", bgeeDirs);
    games[ID_IWDEE] = new GameConfig("Icewind Dale - Enhanced Edition", "baldur.ini", iwdeeDirs);
  }


  public static boolean isEnhancedEdition()
  {
    return currentGame == ID_BGEE ||
           currentGame == ID_BG2EE ||
           currentGame == ID_IWDEE;
  }

  public static int getGameID()
  {
    return currentGame;
  }

  public static String getGameName(int gameID)
  {
    return games[gameID].toString();
  }

  public static ResourceFactory getInstance()
  {
    return factory;
  }

  public static Keyfile getKeyfile()
  {
    return keyfile;
  }

  public static Resource getResource(ResourceEntry entry)
  {
    Resource res = null;
    try {
      if (entry.getExtension().equalsIgnoreCase("BAM"))
        res = new BamResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("TIS"))
        res = new TisResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("BMP"))
        res = new BmpResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("MOS"))
        res = new MosResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("WAV") ||
               entry.getExtension().equalsIgnoreCase("ACM"))
        res = new SoundResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("MUS"))
        res = new MusResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("IDS") ||
               entry.getExtension().equalsIgnoreCase("2DA") ||
               entry.getExtension().equalsIgnoreCase("BIO") ||
               entry.getExtension().equalsIgnoreCase("RES") ||
               entry.getExtension().equalsIgnoreCase("INI") ||
               entry.getExtension().equalsIgnoreCase("TXT") ||
               (entry.getExtension().equalsIgnoreCase("SRC") && getGameID() == ID_ICEWIND2) ||
               entry.getExtension().equalsIgnoreCase("SQL") ||
               entry.getExtension().equalsIgnoreCase("GUI") ||
               (entry.getExtension().equalsIgnoreCase("GLSL") && isEnhancedEdition()))
        res = new PlainTextResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("MVE"))
        res = new MveResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("WBM"))
        res = new WbmResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("PLT"))
        res = new PltResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("BCS") ||
               entry.getExtension().equalsIgnoreCase("BS"))
        res = new BcsResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("ITM"))
        res = new ItmResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("EFF"))
        res = new EffResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("VEF"))
          res = new VefResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("VVC"))
        res = new VvcResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("SRC"))
        res = new SrcResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("DLG"))
        res = new DlgResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("SPL"))
        res = new SplResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("STO"))
        res = new StoResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("WMP"))
        res = new WmpResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("CHU"))
        res = new ChuResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("CRE") ||
               entry.getExtension().equalsIgnoreCase("CHR"))
        res = new CreResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("ARE"))
        res = new AreResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("WFX"))
        res = new WfxResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("PRO"))
        res = new ProResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("WED"))
        res = new WedResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("GAM"))
        res = new GamResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("SAV"))
        res = new SavResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("VAR"))
        res = new VarResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("BAF"))
        res = new BafResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("TOH"))
        res = new TohResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("TOT"))
        res = new TotResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("PVRZ") && isEnhancedEdition())
        res = new PvrzResource(entry);
      else if (entry.getExtension().equalsIgnoreCase("FNT") && isEnhancedEdition())
        res = new FntResource(entry);
      else
        res = new UnknownResource(entry);
    } catch (Exception e) {
      if (NearInfinity.getInstance() != null && !BrowserMenuBar.getInstance().ignoreReadErrors())
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error reading " + entry + '\n' +
                                                                  e.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      else
        NearInfinity.getInstance().getStatusBar().setMessage("Error reading " + entry + " @ " +
                                                             entry.getActualFile() + " - " + e.toString());
      System.err.println("Error reading " + entry);
      e.printStackTrace();
    }
    return res;
  }

  /**
   * Attempts to find the user-profile game folder (supported by BG1EE)
   */
  public static File getUserRoot(int gameID)
  {
    if (isEnhancedEdition()) {
      final String EE_DOC_ROOT = FileSystemView.getFileSystemView().getDefaultDirectory().toString();
      final String EE_DIR = games[gameID].name;
      File userDir = new File(EE_DOC_ROOT, EE_DIR);
      if (userDir.exists()) {
        return userDir;
      } else {
        // fallback solution
        String userPrefix = System.getProperty("user.home");
        String userSuffix = null;
        if (System.getProperty("os.name").contains("Windows")) {
          try {
            Process p = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v personal");
            p.waitFor();
            InputStream in = p.getInputStream();
            byte[] b = new byte[in.available()];
            in.read(b);
            in.close();
            String[] splitted = new String(b).split("\\s\\s+");
            userPrefix = splitted[splitted.length-1];
            userSuffix = EE_DIR;
          } catch (Throwable t) {
            return null;
          }
        } else if (System.getProperty("os.name").contains("Mac")) {
          userSuffix = File.separator + "Documents" + File.separator + EE_DIR;
        }
        if (userSuffix != null) {
          userDir = new File(userPrefix, userSuffix);
          if (userDir.exists()) {
            return userDir;
          }
        }
      }
    }
    return null;
  }

  public static File getRootDir()
  {
    return rootDir;
  }

  public static File getTLKRoot()
  {
    return (langRoot != null) ? langRoot : rootDir;
  }

  public static File getUserRoot()
  {
    return (userRoot != null) ? userRoot : rootDir;
  }

  public static File[] getRootDirs()
  {
    return rootDirs;
  }

  public ResourceFactory(File file)
  {
    rootDir = file.getAbsoluteFile().getParentFile();   // global application rootdir
    rootDirs = new File[]{rootDir};                     // preliminary rootdir list

    // Main game detection
    currentGame = ID_UNKNOWNGAME;
    if (new File(rootDir, "movies/howseer.wbm").exists()) {
      currentGame = ID_IWDEE;
    } else if (new File(rootDir, "movies/pocketzz.wbm").exists()) {
      currentGame = ID_BG2EE;
    } else if (new File(rootDir, "movies/bgenter.wbm").exists()) {
      currentGame = ID_BGEE;
    } else if (new File(rootDir, "torment.exe").exists() &&
               !(new File(rootDir, "movies/sigil.wbm").exists())) {
      currentGame = ID_TORMENT;
    } else if (new File(rootDir, "idmain.exe").exists() &&
               !(new File(rootDir, "movies/howseer.wbm").exists())) {
      currentGame = ID_ICEWIND;
    } else if (new File(rootDir, "iwd2.exe").exists() &&
               new File(rootDir, "Data/Credits.mve").exists()) {
      currentGame = ID_ICEWIND2;
    } else if (new File(rootDir, "baldur.exe").exists() &&
               new File(rootDir, "BGConfig.exe").exists()) {
      currentGame = ID_BG2;
    } else if (new File(rootDir, "movies/graphsim.mov").exists() || // Mac BG1 detection hack
               (new File(rootDir, "baldur.exe").exists() && new File(rootDir, "Config.exe").exists())) {
      currentGame = ID_BG1;
    }

    // Considering three different sources of resource files
    // Note: The order of the root directories is important. NIFile will take the first one available.
    userRoot = getUserRoot(currentGame);
    eeLang = fetchLanguage(userRoot);   // BGEE stores the currently used language in its inifile
    langRoot = NIFile.getFile(rootDir, "lang" + File.separator + eeLang);
    if (!langRoot.exists()) {
      langRoot = null;
    }
    if (userRoot != null && langRoot != null) {
      rootDirs = new File[]{langRoot, userRoot, rootDir};
    } else if (userRoot != null && langRoot == null) {
      rootDirs = new File[]{userRoot, rootDir};
    } else if (userRoot == null && langRoot != null) {
      rootDirs = new File[]{langRoot, rootDir};
    } else {
      rootDirs = new File[]{rootDir};
    }

    keyfile = new Keyfile(file, currentGame);
    factory = this;

    try {
      loadResources();

      // Mac BG2 and ID detection
      if (currentGame == ID_UNKNOWNGAME && resourceExists("MPLYLOGO.MVE")) {
        if (resourceExists("FLYTHR01.MVE"))
          currentGame = ID_BG2;
        else
          currentGame = ID_ICEWIND;
      }

      // Expansion pack detection
      if (currentGame == ID_ICEWIND && resourceExists("HOWDRAG.MVE")) {
      // Detect Trials of the Luremaster
        if (resourceExists("AR9715.ARE")) {
          currentGame = ID_ICEWINDHOWTOT;
        } else {
          currentGame = ID_ICEWINDHOW;
        }
      }
      if (currentGame == ID_BG2 && resourceExists("SARADUSH.MVE")) {
        currentGame = ID_BG2TOB;
      }
      if (currentGame == ID_BG1 && resourceExists("DURLAG.MVE")) {
        currentGame = ID_BG1TOTSC;
      }

      fetchBIFFDirs();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "No Infinity Engine game found", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
  }

  private void fetchBIFFDirs()
  {
    // fetching the CD folders in a game installation
    if (!isEnhancedEdition() && games[currentGame].inifile != null) {
      File iniFile = NIFile.getFile(rootDirs, games[currentGame].inifile);
      List<File> dirList = new ArrayList<File>();
      try {
        BufferedReader br = new BufferedReader(new FileReader(iniFile));
        String line = br.readLine();
        while (line != null) {
          if (line.length() > 5 && line.substring(3, 5).equals(":=")) {
            line = line.substring(5);
            int index = line.indexOf((int)';');
            if (index != -1)
              line = line.substring(0, index);
            if (line.endsWith(":"))
              line = line.replace(':', '/');
            File dir;
            // Try to handle Mac relative paths
            if (line.startsWith("/"))
              dir = NIFile.getFile(rootDirs, line);
            else
              dir = NIFile.getFile(line);
            if (dir.exists())
              dirList.add(dir);
          }
          line = br.readLine();
        }
        br.close();
      } catch (Exception e) {
        e.printStackTrace();
        dirList.clear();
      }
      if (dirList.size() == 0) {
        // Don't panic if an .ini-file cannot be found or contains errors
        dirList.add(NIFile.getFile(rootDirs, "CD1"));
        dirList.add(NIFile.getFile(rootDirs, "CD2"));
        dirList.add(NIFile.getFile(rootDirs, "CD3"));
        dirList.add(NIFile.getFile(rootDirs, "CD4"));
        dirList.add(NIFile.getFile(rootDirs, "CD5"));
        dirList.add(NIFile.getFile(rootDirs, "CD6"));
      }
      biffDirs = new File[dirList.size()];
      for (int i = 0; i < dirList.size(); i++)
        biffDirs[i] = dirList.get(i);
    }
  }

  private String fetchLanguage(File iniRoot)
  {
    final String langDefault = "en_US";   // using default language, if no language entry found

    if (isEnhancedEdition()) {
      String lang = BrowserMenuBar.getInstance().getSelectedGameLanguage();

      if (lang == null || lang.isEmpty()) {
        // Attempting to autodetect game language
        String iniFileName = games[currentGame].inifile;
        if (iniFileName == null || iniFileName.isEmpty())
          iniFileName = "baldur.ini";
        File iniFile = new File(iniRoot, iniFileName);
        if (iniFile.exists()) {
          try {
            BufferedReader br = new BufferedReader(new FileReader(iniFile));
            String line = br.readLine();
            while (line != null) {
              if (line.contains("'Language'")) {
                String[] entries = line.split(",");
                if (entries.length == 3) {
                  lang = entries[2].replace('\'', ' ').trim();
                  if (lang.matches("[a-z]{2}_[A-Z]{2}")) {
                    if (new File(rootDir, "lang" + File.separator + lang).exists()) {
                      br.close();
                      return lang;
                    }
                  }
                }
              }
              line = br.readLine();
            }
            br.close();
          } catch (IOException e) {
            JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error parsing " + iniFileName +
                                          ". Using language defaults.", "Error", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else {
        // Using user-defined language
        if (lang.matches("[a-z]{2}_[A-Z]{2}") &&
            new File(rootDir, "lang" + File.separator + lang).exists()) {
          return lang;
        }
      }
    }

    // falling back to default language
    return langDefault;
  }

  public void exportResource(ResourceEntry entry, Component parent)
  {
    try {
      byte data[] = entry.getResourceData();
      if (entry.getExtension().equalsIgnoreCase("IDS") ||
          entry.getExtension().equalsIgnoreCase("2DA") ||
          entry.getExtension().equalsIgnoreCase("BIO") ||
          entry.getExtension().equalsIgnoreCase("RES") ||
          entry.getExtension().equalsIgnoreCase("INI") ||
          entry.getExtension().equalsIgnoreCase("SET") ||
          entry.getExtension().equalsIgnoreCase("WOK") ||
          entry.getExtension().equalsIgnoreCase("TXI") ||
          entry.getExtension().equalsIgnoreCase("DWK") ||
          entry.getExtension().equalsIgnoreCase("PWK") ||
          entry.getExtension().equalsIgnoreCase("NSS") ||
          entry.getExtension().equalsIgnoreCase("TXT") || (entry.getExtension().equalsIgnoreCase("SRC") &&
                                                           getGameID() == ID_ICEWIND2)) {
        if (data[0] == -1)
          exportResource(entry, Decryptor.decrypt(data, 2, data.length).getBytes(), entry.toString(),
                         parent);
        else
          exportResource(entry, data, entry.toString(), parent);
      }
      else
        exportResource(entry, data, entry.toString(), parent);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(parent, "Can't read " + entry, "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
  }

  public void exportResource(ResourceEntry entry, byte data[], String filename, Component parent)
  {
    if (fc == null) {
      fc = new JFileChooser(getRootDir());
      fc.setDialogTitle("Export resource");
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), filename));
    if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
      File output = fc.getSelectedFile();
      if (output.exists()) {
        String options[] = {"Overwrite", "Cancel"};
        if (JOptionPane.showOptionDialog(parent, output + " exists. Overwrite?", "Export resource",
                                         JOptionPane.YES_NO_OPTION,
                                         JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
          return;
      }
      try {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
        bos.write(data, 0, data.length);
        bos.close();
        JOptionPane.showMessageDialog(parent, "File exported to " + output, "Export complete",
                                      JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(parent, "Error while exporting " + entry, "Error",
                                      JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
  }

  public File getFile(String filename)
  {
    File file = NIFile.getFile(rootDirs, filename);
    if (file.exists())
      return file;
    for (final File biffDir : biffDirs) {
      file = new File(biffDir, filename);
      if (file.exists())
        return file;
    }
    return null;
  }

  public ResourceEntry getResourceEntry(String resourcename)
  {
    return treeModel.getResourceEntry(resourcename);
  }

  public ResourceTreeModel getResources()
  {
    return treeModel;
  }

  public List<ResourceEntry> getResources(String type)
  {
    List<ResourceEntry> list;
    ResourceTreeFolder bifnode = treeModel.getFolder(type);
    if (bifnode != null)
      list = new ArrayList<ResourceEntry>(bifnode.getResourceEntries());
    else
      list = new ArrayList<ResourceEntry>();
    int initsize = list.size();
    for (final String extraDir : games[currentGame].extraDirs) {
      ResourceTreeFolder extranode = treeModel.getFolder(extraDir);
      if (extranode != null)
        list.addAll(extranode.getResourceEntries(type));
    }
    ResourceTreeFolder overridenode = treeModel.getFolder(OVERRIDEFOLDER);
    if (overridenode != null)
      list.addAll(overridenode.getResourceEntries(type));
    if (list.size() > initsize)
      Collections.sort(list);
    return list;
  }

  public void loadResources() throws Exception
  {
    treeModel = new ResourceTreeModel();

    // Get resources from keyfile
    keyfile.addBIFFResourceEntries(treeModel);
    File dlg_file = new File(getTLKRoot(), DIALOGFILENAME);
    StringResource.init(dlg_file);

    // Add other resources
    for (final String extraDir : games[currentGame].extraDirs) {
      for (final File root: rootDirs) {
        File directory = NIFile.getFile(root, extraDir);
        if (directory.exists())
          treeModel.addDirectory((ResourceTreeFolder)treeModel.getRoot(), directory);
      }
    }

    boolean overrideInOverride = (BrowserMenuBar.getInstance() != null &&
                                  BrowserMenuBar.getInstance().getOverrideMode() == BrowserMenuBar.OVERRIDE_IN_OVERRIDE);
    for (final File rootDir: rootDirs) {
      // excluding language folder from search
      if (langRoot == rootDir)
        continue;
      File overrideDir = NIFile.getFile(rootDir, OVERRIDEFOLDER);
      if (overrideDir.exists()) {
        File overrideFiles[] = overrideDir.listFiles();
        for (final File overrideFile : overrideFiles) {
          if (!overrideFile.isDirectory()) {
            String filename = overrideFile.getName().toUpperCase();
            ResourceEntry entry = getResourceEntry(filename);
            if (entry == null) {
              FileResourceEntry fileEntry = new FileResourceEntry(overrideFile, true);
              treeModel.addResourceEntry(fileEntry, fileEntry.getTreeFolder());
            }
            else if (entry instanceof BIFFResourceEntry) {
              ((BIFFResourceEntry)entry).setOverride(true);
              if (overrideInOverride) {
                treeModel.removeResourceEntry(entry, entry.getExtension());
                treeModel.addResourceEntry(new FileResourceEntry(overrideFile, true), OVERRIDEFOLDER);
              }
            }
          }
        }
      }
    }
    treeModel.sort();
  }

  public boolean resourceExists(String resourcename)
  {
    return getResourceEntry(resourcename) != null;
  }

  public void saveCopyOfResource(ResourceEntry entry)
  {
    String filename;
    do {
      filename =
      JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter new filename",
                                  "Add copy of " + entry.toString(), JOptionPane.QUESTION_MESSAGE);
      if (filename != null) {
        if (filename.indexOf(".") == -1)
          filename += '.' + entry.getExtension();
        if (filename.lastIndexOf('.') > 8) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                        "Filenames can only be up to 8 characters long (not including the file extension).",
                                        "Error", JOptionPane.ERROR_MESSAGE);
          filename = null;
        }
        if (resourceExists(filename)) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "File already exists!",
                                        "Error", JOptionPane.ERROR_MESSAGE);
          filename = null;
        }
      }
      else
        return;
    } while (filename == null);
    File output = NIFile.getFile(rootDirs, OVERRIDEFOLDER + File.separatorChar + filename);
    if (entry.getExtension().equalsIgnoreCase("bs"))
      output = NIFile.getFile(rootDirs, "Scripts" + File.separatorChar + filename);

    if (output.exists()) {
      String options[] = {"Overwrite", "Cancel"};
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), output + " exists. Overwrite?", "Save resource",
                                       JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return;
    }
    try {
      byte data[] = entry.getResourceData();
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
      bos.write(data, 0, data.length);
      bos.close();
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), entry.toString() + " copied to " + output, "Copy complete",
                                    JOptionPane.INFORMATION_MESSAGE);
      ResourceEntry newentry = new FileResourceEntry(output, !entry.getExtension().equalsIgnoreCase("bs"));
      treeModel.addResourceEntry(newentry, newentry.getTreeFolder());
      treeModel.sort();
      NearInfinity.getInstance().showResourceEntry(newentry);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error while copying " + entry,
                                    "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
  }

  public boolean saveResource(Resource resource, Component parent)
  {
    if (!(resource instanceof Writeable)) {
      JOptionPane.showMessageDialog(parent, "Resource not savable", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    ResourceEntry entry = resource.getResourceEntry();
    if (entry == null)
      return false;
    File output;
    if (entry instanceof BIFFResourceEntry) {
      output = NIFile.getFile(rootDirs, OVERRIDEFOLDER + File.separatorChar + entry.toString());
      File override = NIFile.getFile(rootDirs, OVERRIDEFOLDER + File.separatorChar);
      if (!override.exists())
        override.mkdir();
      ((BIFFResourceEntry)entry).setOverride(true);
    }
    else
      output = entry.getActualFile();
    if (output != null && output.exists()) {
      String options[] = {"Overwrite", "Cancel"};
      if (JOptionPane.showOptionDialog(parent, output + " exists. Overwrite?", "Save resource",
                                       JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return false;
    }
    try {
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
      ((Writeable)resource).write(bos);
      bos.close();
      JOptionPane.showMessageDialog(parent, "File saved to \"" + output.getAbsolutePath() + '\"',
                                    "Save complete", JOptionPane.INFORMATION_MESSAGE);
      if (resource.getResourceEntry().getExtension().equals("IDS")) {
        IdsMapCache.cacheInvalid(resource.getResourceEntry());
        IdsBrowser idsbrowser = (IdsBrowser)ChildFrame.getFirstFrame(IdsBrowser.class);
        if (idsbrowser != null)
          idsbrowser.refreshList();
        Compiler.restartCompiler();
      }
      else if (resource.getResourceEntry().toString().equalsIgnoreCase("KITLIST.2DA"))
        Kit2daBitmap.resetKitlist();
      else if (resource.getResourceEntry().toString().equalsIgnoreCase("SONGLIST.2DA"))
        Song2daBitmap.resetSonglist();
    } catch (IOException e) {
      JOptionPane.showMessageDialog(parent, "Error while saving " + resource.getResourceEntry().toString(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      return false;
    }
    return true;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class GameConfig
  {
    private final String name;
    private final String inifile;
    private final String extraDirs[];

    private GameConfig(String name, String inifile, String[] extraDirs)
    {
      this.name = name;
      this.inifile = inifile;
      this.extraDirs = extraDirs;
    }

    private GameConfig(String name, String[] extraDirs)
    {
      this.name = name;
      this.inifile = null;
      this.extraDirs = extraDirs;
    }

    @Override
    public String toString()
    {
      return name;
    }
  }
}

