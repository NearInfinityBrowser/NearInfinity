// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.awt.Component;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import org.infinity.NearInfinity;
import org.infinity.datatype.PriTypeBitmap;
import org.infinity.datatype.SecTypeBitmap;
import org.infinity.datatype.Song2daBitmap;
import org.infinity.datatype.Summon2daBitmap;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.IdsBrowser;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.bcs.BafResource;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.chu.ChuResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.gam.GamResource;
import org.infinity.resource.graphics.BamResource;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.MosResource;
import org.infinity.resource.graphics.PltResource;
import org.infinity.resource.graphics.PvrzResource;
import org.infinity.resource.graphics.TisResource;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.key.BIFFResourceEntry;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.Keyfile;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.key.ResourceTreeFolder;
import org.infinity.resource.key.ResourceTreeModel;
import org.infinity.resource.mus.MusResource;
import org.infinity.resource.other.EffResource;
import org.infinity.resource.other.FntResource;
import org.infinity.resource.other.TtfResource;
import org.infinity.resource.other.UnknownResource;
import org.infinity.resource.other.VvcResource;
import org.infinity.resource.other.WfxResource;
import org.infinity.resource.pro.ProResource;
import org.infinity.resource.sav.SavResource;
import org.infinity.resource.sound.SoundResource;
import org.infinity.resource.spl.SplResource;
import org.infinity.resource.src.SrcResource;
import org.infinity.resource.sto.StoResource;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.resource.to.TohResource;
import org.infinity.resource.to.TotResource;
import org.infinity.resource.var.VarResource;
import org.infinity.resource.vef.VefResource;
import org.infinity.resource.video.MveResource;
import org.infinity.resource.video.WbmResource;
import org.infinity.resource.wed.WedResource;
import org.infinity.resource.wmp.WmpResource;
import org.infinity.util.Decryptor;
import org.infinity.util.DynamicArray;
import org.infinity.util.IdsMapCache;
import org.infinity.util.StringResource;
import org.infinity.util.io.FileLookup;
import org.infinity.util.io.FileNI;
import org.infinity.util.io.FileOutputStreamNI;
import org.infinity.util.io.FileReaderNI;

/**
 * Handles game-specific resource access.
 */
public final class ResourceFactory
{

  private static ResourceFactory instance;

  private JFileChooser fc;
  private Keyfile keyfile;
  private ResourceTreeModel treeModel;

  public static Keyfile getKeyfile()
  {
    if (getInstance() != null) {
      return getInstance().keyfile;
    } else {
      return null;
    }
  }

  public static Resource getResource(ResourceEntry entry)
  {
    return getResource(entry, null);
  }

  public static Resource getResource(ResourceEntry entry, String forcedExtension)
  {
    Resource res = null;
    try {
      String ext = (forcedExtension != null) ? forcedExtension : entry.getExtension();
      if (ext.equalsIgnoreCase("BAM")) {
        res = new BamResource(entry);
      } else if (ext.equalsIgnoreCase("TIS")) {
        res = new TisResource(entry);
      } else if (ext.equalsIgnoreCase("BMP") || ext.equalsIgnoreCase("PNG")) {
        res = new GraphicsResource(entry);
      } else if (ext.equalsIgnoreCase("MOS")) {
        res = new MosResource(entry);
      } else if (ext.equalsIgnoreCase("WAV") || ext.equalsIgnoreCase("ACM")) {
        res = new SoundResource(entry);
      } else if (ext.equalsIgnoreCase("MUS")) {
        res = new MusResource(entry);
      } else if (ext.equalsIgnoreCase("IDS") || ext.equalsIgnoreCase("2DA") ||
                 ext.equalsIgnoreCase("BIO") || ext.equalsIgnoreCase("RES") ||
                 ext.equalsIgnoreCase("INI") || ext.equalsIgnoreCase("TXT") ||
                 (ext.equalsIgnoreCase("SRC") && Profile.getEngine() == Profile.Engine.IWD2) ||
                 (Profile.isEnhancedEdition() && (ext.equalsIgnoreCase("SQL") ||
                                                  ext.equalsIgnoreCase("GUI") ||
                                                  ext.equalsIgnoreCase("LUA") ||
                                                  ext.equalsIgnoreCase("MENU") ||
                                                  ext.equalsIgnoreCase("GLSL")))) {
        res = new PlainTextResource(entry);
      } else if (ext.equalsIgnoreCase("MVE")) {
        res = new MveResource(entry);
      } else if (ext.equalsIgnoreCase("WBM")) {
        res = new WbmResource(entry);
      } else if (ext.equalsIgnoreCase("PLT") && ext.equals(forcedExtension)) {
        res = new PltResource(entry);
      } else if (ext.equalsIgnoreCase("BCS") || ext.equalsIgnoreCase("BS")) {
        res = new BcsResource(entry);
      } else if (ext.equalsIgnoreCase("ITM")) {
        res = new ItmResource(entry);
      } else if (ext.equalsIgnoreCase("EFF")) {
        res = new EffResource(entry);
      } else if (ext.equalsIgnoreCase("VEF")) {
          res = new VefResource(entry);
      } else if (ext.equalsIgnoreCase("VVC")) {
        res = new VvcResource(entry);
      } else if (ext.equalsIgnoreCase("SRC")) {
        res = new SrcResource(entry);
      } else if (ext.equalsIgnoreCase("DLG")) {
        res = new DlgResource(entry);
      } else if (ext.equalsIgnoreCase("SPL")) {
        res = new SplResource(entry);
      } else if (ext.equalsIgnoreCase("STO")) {
        res = new StoResource(entry);
      } else if (ext.equalsIgnoreCase("WMP")) {
        res = new WmpResource(entry);
      } else if (ext.equalsIgnoreCase("CHU")) {
        res = new ChuResource(entry);
      } else if (ext.equalsIgnoreCase("CRE") || ext.equalsIgnoreCase("CHR")) {
        res = new CreResource(entry);
      } else if (ext.equalsIgnoreCase("ARE")) {
        res = new AreResource(entry);
      } else if (ext.equalsIgnoreCase("WFX")) {
        res = new WfxResource(entry);
      } else if (ext.equalsIgnoreCase("PRO")) {
        res = new ProResource(entry);
      } else if (ext.equalsIgnoreCase("WED")) {
        res = new WedResource(entry);
      } else if (ext.equalsIgnoreCase("GAM")) {
        res = new GamResource(entry);
      } else if (ext.equalsIgnoreCase("SAV")) {
        res = new SavResource(entry);
      } else if (ext.equalsIgnoreCase("VAR")) {
        res = new VarResource(entry);
      } else if (ext.equalsIgnoreCase("BAF")) {
        res = new BafResource(entry);
      } else if (ext.equalsIgnoreCase("TOH")) {
        res = new TohResource(entry);
      } else if (ext.equalsIgnoreCase("TOT")) {
        res = new TotResource(entry);
      } else if (ext.equalsIgnoreCase("PVRZ") && Profile.isEnhancedEdition()) {
        res = new PvrzResource(entry);
      } else if (ext.equalsIgnoreCase("FNT") && Profile.isEnhancedEdition()) {
        res = new FntResource(entry);
      } else if (ext.equalsIgnoreCase("TTF") && Profile.isEnhancedEdition()) {
        res = new TtfResource(entry);
      } else {
        res = detectResource(entry);
        if (res == null) {
          res = new UnknownResource(entry);
        }
      }
    } catch (Exception e) {
      if (NearInfinity.getInstance() != null && !BrowserMenuBar.getInstance().ignoreReadErrors()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                      "Error reading " + entry + '\n' + e.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      } else {
        final String msg = String.format("Error reading %1$s @ %2$s - %3$s",
                                         entry, entry.getActualFile(), e);
        NearInfinity.getInstance().getStatusBar().setMessage(msg);
      }
      System.err.println("Error reading " + entry);
      e.printStackTrace();
    }
    return res;
  }

  /**
   * Attempts to detect the resource type from the data itself
   * and returns the respective resource class instance, or <code>null</code> on failure.
   */
  public static Resource detectResource(ResourceEntry entry)
  {
    Resource res = null;
    if (entry != null) {
      try {
        int[] info = entry.getResourceInfo();
        if (info.length == 2) {
          res = getResource(entry, "TIS");
        } else if (info.length == 1) {
          if (info[0] > 4) {
            byte[] data = entry.getResourceData();
            String sig = DynamicArray.getString(data, 0, 4);
            if ("2DA ".equalsIgnoreCase(sig)) {
              res = getResource(entry, "2DA");
            } else if ("ARE ".equals(sig)) {
              res = getResource(entry, "ARE");
            } else if ("BAM ".equals(sig) || "BAMC".equals(sig)) {
              res = getResource(entry, "BAM");
            } else if ("CHR ".equals(sig)) {
              res = getResource(entry, "CHR");
            } else if ("CHUI".equals(sig)) {
              res = getResource(entry, "CHU");
            } else if ("CRE ".equals(sig)) {
              res = getResource(entry, "CRE");
            } else if ("DLG ".equals(sig)) {
              res = getResource(entry, "DLG");
            } else if ("EFF ".equals(sig)) {
              res = getResource(entry, "EFF");
            } else if ("GAME".equals(sig)) {
              res = getResource(entry, "GAM");
            } else if ("IDS ".equalsIgnoreCase(sig)) {
              res = getResource(entry, "IDS");
            } else if ("ITM ".equals(sig)) {
              res = getResource(entry, "ITM");
            } else if ("MOS ".equals(sig) || "MOSC".equals(sig)) {
              res = getResource(entry, "MOS");
            } else if ("PLT ".equals(sig)) {
              res = getResource(entry, "PLT");
            } else if ("PRO ".equals(sig)) {
              res = getResource(entry, "PRO");
            } else if ("SAV ".equals(sig)) {
              res = getResource(entry, "SAV");
            } else if ("SPL ".equals(sig)) {
              res = getResource(entry, "SPL");
            } else if ("STOR".equals(sig)) {
              res = getResource(entry, "STO");
            } else if ("TIS ".equals(sig)) {
              res = getResource(entry, "TIS");
            } else if ("VEF ".equals(sig)) {
              res = getResource(entry, "VEF");
            } else if ("VVC ".equals(sig)) {
              res = getResource(entry, "VVC");
            } else if ("WAVC".equals(sig) || "RIFF".equals(sig) || "OggS".equals(sig)) {
              res = getResource(entry, "WAV");
            } else if ("WED ".equals(sig)) {
              res = getResource(entry, "WED");
            } else if ("WFX ".equals(sig)) {
              res = getResource(entry, "WFX");
            } else if ("WMAP".equals(sig)) {
              res = getResource(entry, "WMP");
            } else {
              if ((Arrays.equals(new byte[]{0x53, 0x43, 0x0a}, Arrays.copyOfRange(data, 0, 3)) ||  // == "SC\n"
                   Arrays.equals(new byte[]{0x53, 0x43, 0x0d, 0x0a}, Arrays.copyOfRange(data, 0, 4)))) { // == "SC\r\n"
                res = getResource(entry, "BCS");
              } else if (data.length > 6 && "BM".equals(new String(data, 0, 2)) &&
                         DynamicArray.getInt(data, 2) == info[0]) {
                res = getResource(entry, "BMP");
              } else if (data.length > 18 && "Interplay MVE File".equals(new String(data, 0, 18))) {
                res = getResource(entry, "MVE");
              } else if (Arrays.equals(new byte[]{(byte)0x1a, (byte)0x45, (byte)0xdf, (byte)0xa3},
                                       Arrays.copyOfRange(data, 0, 4))) {
                res = getResource(entry, "WBM");
              } else if (data.length > 6 && data[3] == 0 && data[4] == 0x78) {  // just guessing...
                res = getResource(entry, "PVRZ");
              } else if (data.length > 4 && data[0] == 0x89 &&
                         data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47) {
                res = getResource(entry, "PNG");
              } else if (DynamicArray.getInt(data, 0) == 0x00000100) {  // wild guess...
                res = getResource(entry, "TTF");
              }
            }
          }
        } else {
          throw new Exception(entry.getResourceName() + ": Unable to determine resource type");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return res;
  }

  public static void exportResource(ResourceEntry entry, Component parent)
  {
    if (getInstance() != null) {
      try {
        getInstance().exportResourceInternal(entry, parent, null);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public static void exportResource(ResourceEntry entry, byte data[], String filename, Component parent)
  {
    if (getInstance() != null) {
      try {
        getInstance().exportResourceInternal(entry, data, filename, parent, null);
      } catch (Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  /** Exports "entry" to "output" without any user interaction. */
  public static void exportResource(ResourceEntry entry, File output) throws Exception
  {
    if (getInstance() != null && output != null) {
      getInstance().exportResourceInternal(entry, null, output);
    }
  }

  public static File getFile(String filename)
  {
    if (getInstance() != null) {
      return getInstance().getFileInternal(filename);
    } else {
      return null;
    }
  }

  /**
   * Returns whether the specified resource exists.
   * @param resourceName The resource filename.
   * @return <code>true</code> if the resource exists in BIFF archives or override folders,
   *         <code>false</code> otherwise.
   */
  public static boolean resourceExists(String resourceName)
  {
    return (getResourceEntry(resourceName, false) != null);
  }

  /**
   * Returns whether the specified resource exists.
   * @param resourceName The resource filename.
   * @param searchExtraDirs If <code>true</code>, all supported override folders will be searched.
   *                        If <code>false</code>, only the default 'override' folders will be searched.
   * @return <code>true</code> if the resource exists in BIFF archives or override folders,
   *         <code>false</code> otherwise.
   */
  public static boolean resourceExists(String resourceName, boolean searchExtraDirs)
  {
    return (getResourceEntry(resourceName, searchExtraDirs) != null);
  }

  /**
   * Returns whether the specified resource exists.
   * @param resourceName The resource filename.
   * @param searchExtraDirs If <code>true</code>, all supported override folders will be searched.
   *                        If <code>false</code>, only the default 'override' folders will be searched.
   * @param extraDirs       A list of File entries pointing to additional folders to search, not
   *                        covered by the default override folder list (e.g. "install:/music").
   * @return <code>true</code> if the resource exists in BIFF archives or override folders,
   *         <code>false</code> otherwise.
   */
  public static boolean resourceExists(String resourceName, boolean searchExtraDirs, List<File> extraDirs)
  {
    return (getResourceEntry(resourceName, searchExtraDirs, extraDirs) != null);
  }

  /**
   * Returns a ResourceEntry instance of the given resource name.
   * @param resourceName The resource filename.
   * @return A ResourceEntry instance of the given resource filename, or <code>null</code> if not
   *         available.
   */
  public static ResourceEntry getResourceEntry(String resourceName)
  {
    return getResourceEntry(resourceName, false, null);
  }

  /**
   * Returns a ResourceEntry instance of the given resource name.
   * @param resourceName The resource filename.
   * @param searchExtraDirs If <code>true</code>, all supported override folders will be searched.
   *                        If <code>false</code>, only the default 'override' folders will be searched.
   * @return A ResourceEntry instance of the given resource filename, or <code>null</code> if not
   *         available.
   */
  public static ResourceEntry getResourceEntry(String resourceName, boolean searchExtraDirs)
  {
    return getResourceEntry(resourceName, searchExtraDirs, null);
  }

  /**
   * Returns a ResourceEntry instance of the given resource name.
   * @param resourceName The resource filename.
   * @param searchExtraDirs If <code>true</code>, all supported override folders will be searched.
   *                        If <code>false</code>, only the default 'override' folders will be searched.
   * @param extraDirs       A list of File entries pointing to additional folders to search, not
   *                        covered by the default override folder list (e.g. "install:/music").
   * @return A ResourceEntry instance of the given resource filename, or <code>null</code> if not
   *         available.
   */
  public static ResourceEntry getResourceEntry(String resourceName, boolean searchExtraDirs, List<File> extraDirs)
  {
    if (getInstance() != null) {
//      System.out.println(String.format("ResourceFactory.getResourceEntry(\"%s\", %s, ???)", resourceName, Boolean.toString(searchExtraDirs)));
      ResourceEntry entry = getInstance().treeModel.getResourceEntry(resourceName);

      // checking default override folder list
      if (searchExtraDirs && (entry == null)) {
        @SuppressWarnings("unchecked")
        List<File> extraFolders = (List<File>)Profile.getProperty(Profile.GET_GAME_OVERRIDE_FOLDERS);
        if (extraFolders != null) {
          for (final File folder: extraFolders) {
            File f = new FileNI(folder, resourceName);
            if (f.isFile()) {
              entry = new FileResourceEntry(f);
              break;
            }
          }
        }
      }

      // checking custom folder list
      if (extraDirs != null && (entry == null)) {
        for (final File folder: extraDirs) {
          File f = new FileNI(folder, resourceName);
          if (f.isFile()) {
            entry = new FileResourceEntry(f);
            break;
          }
        }
      }

      return entry;
    } else {
      return null;
    }
  }

  public static ResourceTreeModel getResources()
  {
    if (getInstance() != null) {
      return getInstance().treeModel;
    } else {
      return null;
    }
  }

  public static List<ResourceEntry> getResources(String type)
  {
    if (getInstance() != null) {
      return getInstance().getResourcesInternal(type);
    } else {
      return null;
    }
  }

  public static void loadResources() throws Exception
  {
    if (getInstance() != null) {
      getInstance().loadResourcesInternal();
    }
  }

  public static void saveCopyOfResource(ResourceEntry entry)
  {
    if (getInstance() != null) {
      getInstance().saveCopyOfResourceInternal(entry);
    }
  }

  public static boolean saveResource(Resource resource, Component parent)
  {
    if (getInstance() != null) {
      return getInstance().saveResourceInternal(resource, parent);
    } else {
      return false;
    }
  }

  /**
   * Returns a list of available game language directories for the current game in Enhanced Edition games.
   * Returns an empty list otherwise.
   */
  public static List<File> getAvailableGameLanguages()
  {
    List<File> list = new ArrayList<File>();

    if (Profile.isEnhancedEdition()) {
      File langDir = (File)Profile.getProperty(Profile.GET_GAME_LANG_FOLDER_BASE);
      if (langDir.isDirectory()) {
        File[] langDirList = langDir.listFiles(new FileFilter() {
          @Override
          public boolean accept(File pathname)
          {
            // accept only folder names in language code format containing a dialog.tlk file
            return (pathname != null && pathname.isDirectory() &&
                    pathname.getName().matches("[a-z]{2}_[A-Z]{2}") &&
                    (new FileNI(pathname, (String)Profile.getProperty(Profile.GET_GLOBAL_DIALOG_NAME))).isFile());
          }
        });
        for (final File lang: langDirList) {
          list.add(lang);
        }
      }
    }
    return list;
  }

  /**  Return the game language specified in the given baldur.ini if found. Returns <code>en_US</code> otherwise. */
  public static String autodetectGameLanguage(File iniFile)
  {
    final String langDefault = "en_US";   // using default language, if no language entry found
    if (Profile.isEnhancedEdition() && iniFile != null && iniFile.isFile()) {
      // Attempt to autodetect game language
      if (iniFile != null && iniFile.isFile()) {
        try {
          BufferedReader br = new BufferedReader(new FileReaderNI(iniFile));
          String line = br.readLine();
          while (line != null) {
            if (line.contains("'Language'")) {
              String[] entries = line.split(",");
              if (entries.length == 3) {
                // Note: replace operation is compatible with both baldur.ini and baldur.lua
                String lang = entries[2].replaceFirst("^[^']*'", "");
                lang = lang.replaceFirst("'.*$", "");
                if (lang.matches("[A-Za-z]{2}_[A-Za-z]{2}")) {
                  File dir = new FileNI(Profile.getGameRoot(), "lang/" + lang);
                  if (dir.isDirectory()) {
                    String retVal;
                    try {
                      // try to fetch the actual path name to ensure correct case
                      retVal = dir.getCanonicalFile().getName();
                    } catch (Exception e) {
                      retVal = lang;
                    }
                    br.close();
                    return retVal;
                  }
                }
              }
            }
            line = br.readLine();
          }
          br.close();
        } catch (IOException e) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error parsing " + iniFile.getName() +
                                        ". Using language defaults.", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
    return langDefault;
  }

  /** Attempts to find the home folder of an Enhanced Edition game. */
  static File getHomeRoot()
  {
    if (Profile.isEnhancedEdition()) {
      final String EE_DOC_ROOT = FileSystemView.getFileSystemView().getDefaultDirectory().toString();
      final String EE_DIR = (String)Profile.getProperty(Profile.GET_GAME_HOME_FOLDER_NAME);
      File userDir = new FileNI(EE_DOC_ROOT, EE_DIR);
      if (userDir.isDirectory()) {
        return userDir;
      } else {
        // fallback solution
        String userPrefix = System.getProperty("user.home");
        String userSuffix = null;
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.contains("windows")) {
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
        } else if (osName.contains("mac") || osName.contains("darwin")) {
          userSuffix = File.separator + "Documents" + File.separator + EE_DIR;
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("bsd")) {
          userSuffix = File.separator + ".local" + File.separator + "share" + File.separator + EE_DIR;
        }
        if (userSuffix != null) {
          userDir = new FileNI(userPrefix, userSuffix);
          if (userDir.isDirectory()) {
            return userDir;
          }
        }
      }
    }
    return null;
  }

  /** Attempts to find folders containing BIFF archives. */
  static List<File> getBIFFDirs()
  {
    List<File> dirList = new ArrayList<File>();

    // fetching the CD folders in a game installation
    if (!Profile.isEnhancedEdition()) {
      File iniFile = (File)Profile.getProperty(Profile.GET_GAME_INI_FILE);
      List<?> rootFolders = (List<?>)Profile.getProperty(Profile.GET_GAME_FOLDERS);
      if (iniFile != null && iniFile.isFile()) {
        try {
          BufferedReader br = new BufferedReader(new FileReaderNI(iniFile));
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
              if (line.startsWith("/")) {
                dir = FileNI.getFile(rootFolders, line);
              } else {
                dir = FileNI.getFile(line);
              }
              if (dir.isDirectory()) {
                dirList.add(dir);
              }
            }
            line = br.readLine();
          }
          br.close();
        } catch (Exception e) {
          e.printStackTrace();
          dirList.clear();
        }
      }
      if (dirList.size() == 0) {
        // Don't panic if an .ini-file cannot be found or contains errors
        File f;
        for (int i = 1; i < 7; i++) {
          f = FileNI.getFile(rootFolders, String.format("CD%1$d", i));
          if (f != null && f.isDirectory()) {
            dirList.add(f);
          }
        }
        // used in certain games
        f = FileNI.getFile(rootFolders, "CDALL");
        if (f != null && f.isDirectory()) {
          dirList.add(f);
        }
      }
    }
    return dirList;
  }

  // Returns the currently used language of an Enhanced Edition game.
  static String fetchGameLanguage(File iniFile)
  {
    final String langDefault = "en_US";   // using default language, if no language entry found

    if (Profile.isEnhancedEdition() && iniFile != null && iniFile.isFile()) {
      String lang = BrowserMenuBar.getInstance().getSelectedGameLanguage();

      if (lang == null || lang.isEmpty()) {
        return autodetectGameLanguage(iniFile);
      } else {
        // Using user-defined language
        if (lang.matches("[A-Za-z]{2}_[A-Za-z]{2}")) {
          File dir = new FileNI(Profile.getGameRoot(), "lang/" + lang);
          if (dir.isDirectory()) {
            String retVal;
            try {
              // try to fetch the actual path name to ensure correct case
              retVal = dir.getCanonicalFile().getName();
            } catch (Exception e) {
              retVal = lang;
            }
            return retVal;
          }
        }
      }
    }

    // falling back to default language
    return langDefault;
  }

  /** Used internally by the Profile class to open and initialize a new game. */
  static void openGame(File keyFile)
  {
    closeGame();
    new ResourceFactory(keyFile);
  }

  /** Closes the current game configuration. */
  private static void closeGame()
  {
    if (instance != null) {
      instance.close();
      instance = null;
    }
  }

  private static ResourceFactory getInstance()
  {
    return instance;
  }


  private ResourceFactory(File keyFile)
  {
    instance = this;
    this.keyfile = new Keyfile(keyFile);
    try {
      loadResourcesInternal();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, "No Infinity Engine game found", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
  }

  // Cleans up resources
  private void close()
  {
    // nothing to do yet...
  }

  private void exportResourceInternal(ResourceEntry entry, Component parent, File output) throws Exception
  {
    try {
      byte data[] = entry.getResourceData();
      final String ext = entry.getExtension();
      if (ext.equalsIgnoreCase("IDS") || ext.equalsIgnoreCase("2DA") ||
          ext.equalsIgnoreCase("BIO") || ext.equalsIgnoreCase("RES") ||
          ext.equalsIgnoreCase("INI") || ext.equalsIgnoreCase("SET") ||
          ext.equalsIgnoreCase("TXT") ||
          (Profile.getEngine() == Profile.Engine.IWD2 && ext.equalsIgnoreCase("SRC")) ||
          (Profile.isEnhancedEdition() && (ext.equalsIgnoreCase("GUI") ||
                                           ext.equalsIgnoreCase("SQL") ||
                                           ext.equalsIgnoreCase("GLSL")))) {
        if (data[0] == -1) {
          exportResourceInternal(entry, Decryptor.decrypt(data, 2, data.length).getBytes(),
                                 entry.toString(), parent, output);
        } else {
          exportResourceInternal(entry, data, entry.toString(), parent, output);
        }
      } else {
        exportResourceInternal(entry, data, entry.toString(), parent, output);
      }
    } catch (Exception e) {
      throw new Exception("Can't read " + entry);
    }
  }

  private void exportResourceInternal(ResourceEntry entry, byte data[], String filename, Component parent,
                                      File output) throws Exception
  {
    // ask for output file path if needed
    boolean interactive = (output == null);
    if (interactive) {
      if (fc == null) {
        fc = new JFileChooser(Profile.getGameRoot());
        fc.setDialogTitle("Export resource");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      }
      fc.setSelectedFile(new FileNI(fc.getCurrentDirectory(), filename));
      if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
        output = fc.getSelectedFile();
        if (output.exists()) {
          final String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(parent, output + " exists. Overwrite?", "Export resource",
                                           JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                           null, options, options[0]) != 0) {
            return;
          }
        }
      }
    }

    // exporting resource
    if (output != null) {
      try {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
        bos.write(data, 0, data.length);
        bos.close();
        if (interactive) {
          JOptionPane.showMessageDialog(parent, "File exported to " + output, "Export complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        }
      } catch (IOException e) {
        throw new Exception("Error while exporting " + entry);
      }
    }
  }

  private File getFileInternal(String filename)
  {
    File file = FileNI.getFile(Profile.getRootFolders(), filename);
    if (file.exists()) {
      return file;
    }
    List<?> biffDirs = (List<?>)Profile.getProperty(Profile.GET_GAME_BIFF_FOLDERS);
    for (final Object biffDir : biffDirs) {
      if (biffDir instanceof File) {
        file = new FileNI((File)biffDir, filename);
        if (file.exists()) {
          return file;
        }
      }
    }
    return null;
  }

  private void loadResourcesInternal() throws Exception
  {
    treeModel = new ResourceTreeModel();

    // Get resources from keyfile
    keyfile.addBIFFResourceEntries(treeModel);
    StringResource.init((File)Profile.getProperty(Profile.GET_GAME_DIALOG_FILE));

    // Add other resources
    List<?> extraDirs = (List<?>)Profile.getProperty(Profile.GET_GAME_EXTRA_FOLDERS);
    List<File> rootDirs = Profile.getRootFolders();
    for (final Object extraDir : extraDirs) {
      for (final File root: rootDirs) {
        File directory = FileNI.getFile(root, extraDir.toString());
        if (directory.isDirectory()) {
          treeModel.addDirectory((ResourceTreeFolder)treeModel.getRoot(), directory);
        }
      }
    }

    boolean overrideInOverride = (BrowserMenuBar.getInstance() != null &&
                                  BrowserMenuBar.getInstance().getOverrideMode() == BrowserMenuBar.OVERRIDE_IN_OVERRIDE);
    String overrideFolder = Profile.getOverrideFolderName();
    for (final File rootDir: rootDirs) {
      File overrideDir = FileNI.getFile(rootDir, overrideFolder);
      if (overrideDir.isDirectory()) {
        File overrideFiles[] = overrideDir.listFiles();
        for (final File overrideFile : overrideFiles) {
          if (!overrideFile.isDirectory()) {
            String filename = overrideFile.getName().toUpperCase(Locale.ENGLISH);
            ResourceEntry entry = getResourceEntry(filename);
            if (entry == null) {
              FileResourceEntry fileEntry = new FileResourceEntry(overrideFile, true);
              treeModel.addResourceEntry(fileEntry, fileEntry.getTreeFolder());
            }
            else if (entry instanceof BIFFResourceEntry) {
              ((BIFFResourceEntry)entry).setOverride(true);
              if (overrideInOverride) {
                treeModel.removeResourceEntry(entry, entry.getExtension());
                treeModel.addResourceEntry(new FileResourceEntry(overrideFile, true), overrideFolder);
              }
            }
          }
        }
      }
    }
    treeModel.sort();
  }

  private List<ResourceEntry> getResourcesInternal(String type)
  {
    List<ResourceEntry> list;
    ResourceTreeFolder bifNode = treeModel.getFolder(type);
    if (bifNode != null) {
      list = new ArrayList<ResourceEntry>(bifNode.getResourceEntries());
    } else {
      list = new ArrayList<ResourceEntry>();
    }
    int initsize = list.size();
    List<?> extraDirs = (List<?>)Profile.getProperty(Profile.GET_GAME_EXTRA_FOLDERS);
    for (Iterator<?> iter = extraDirs.iterator(); iter.hasNext();) {
      ResourceTreeFolder extraNode = treeModel.getFolder(iter.next().toString());
      if (extraNode != null) {
        list.addAll(extraNode.getResourceEntries(type));
      }
    }
    ResourceTreeFolder overrideNode = treeModel.getFolder(Profile.getOverrideFolderName());
    if (overrideNode != null) {
      list.addAll(overrideNode.getResourceEntries(type));
    }
    if (list.size() > initsize) {
      Collections.sort(list);
    }
    return list;
  }

  private void saveCopyOfResourceInternal(ResourceEntry entry)
  {
    String filename;
    do {
      filename = JOptionPane.showInputDialog(NearInfinity.getInstance(), "Enter new filename",
                                             "Add copy of " + entry.toString(),
                                             JOptionPane.QUESTION_MESSAGE);
      if (filename != null) {
        if (filename.indexOf(".") == -1) {
          filename += '.' + entry.getExtension();
        }
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
      } else {
        return;
      }
    } while (filename == null);

    // creating override folder in game directory if it doesn't exist
    File outdir = FileNI.getFile(Profile.getRootFolders(), Profile.getOverrideFolderName().toLowerCase(Locale.ENGLISH));
    if (!outdir.isDirectory()) {
      outdir.mkdir();
    }

    File output = FileNI.getFile(outdir, File.separatorChar + filename);
    if (entry.getExtension().equalsIgnoreCase("bs")) {
      output = FileNI.getFile(Profile.getRootFolders(), "Scripts" + File.separatorChar + filename);
    }

    if (output.exists()) {
      String options[] = {"Overwrite", "Cancel"};
      if (JOptionPane.showOptionDialog(NearInfinity.getInstance(), output + " exists. Overwrite?",
                                       "Save resource", JOptionPane.YES_NO_OPTION,
                                       JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
        return;
    }
    try {
      byte data[] = entry.getResourceData();
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
      bos.write(data, 0, data.length);
      bos.close();
      FileLookup.getInstance().add(output);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), entry.toString() + " copied to " + output,
                                    "Copy complete", JOptionPane.INFORMATION_MESSAGE);
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

  private boolean saveResourceInternal(Resource resource, Component parent)
  {
    if (!(resource instanceof Writeable)) {
      JOptionPane.showMessageDialog(parent, "Resource not savable", "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }
    ResourceEntry entry = resource.getResourceEntry();
    if (entry == null) {
      return false;
    }
    File output;
    if (entry instanceof BIFFResourceEntry) {
      output = FileNI.getFile(Profile.getRootFolders(), Profile.getOverrideFolderName() + File.separatorChar + entry.toString());
      File override = FileNI.getFile(Profile.getRootFolders(), Profile.getOverrideFolderName() + File.separatorChar);
      if (!override.exists()) {
        override.mkdir();
      }
      ((BIFFResourceEntry)entry).setOverride(true);
    } else {
      output = entry.getActualFile();
    }
    if (output != null && output.exists()) {
      String options[] = {"Overwrite", "Cancel"};
      if (JOptionPane.showOptionDialog(parent, output + " exists. Overwrite?", "Save resource",
                                       JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                       null, options, options[0]) == 0) {
        if (BrowserMenuBar.getInstance().backupOnSave()) {
          try {
            File bakFile = new FileNI(output.getCanonicalPath() + ".bak");
            if (bakFile.isFile()) {
              bakFile.delete();
            }
            if (!bakFile.exists()) {
              output.renameTo(bakFile);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      } else {
        return false;
      }
    }
    try {
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStreamNI(output));
      ((Writeable)resource).write(bos);
      bos.close();
      JOptionPane.showMessageDialog(parent, "File saved to \"" + output.getAbsolutePath() + '\"',
                                    "Save complete", JOptionPane.INFORMATION_MESSAGE);
      if (resource.getResourceEntry().getExtension().equals("IDS")) {
        IdsMapCache.cacheInvalid(resource.getResourceEntry());
        IdsBrowser idsbrowser = (IdsBrowser)ChildFrame.getFirstFrame(IdsBrowser.class);
        if (idsbrowser != null) {
          idsbrowser.refreshList();
        }
        Compiler.restartCompiler();
      } else if (resource.getResourceEntry().toString().equalsIgnoreCase(Song2daBitmap.getTableName())) {
        Song2daBitmap.resetSonglist();
      } else if (resource.getResourceEntry().toString().equalsIgnoreCase(Summon2daBitmap.getTableName())) {
        Summon2daBitmap.resetSummonTable();
      } else if (resource.getResourceEntry().toString().equalsIgnoreCase(PriTypeBitmap.getTableName())) {
        PriTypeBitmap.resetTypeTable();
      } else if (resource.getResourceEntry().toString().equalsIgnoreCase(SecTypeBitmap.getTableName())) {
        SecTypeBitmap.resetTypeTable();
      }
    } catch (IOException e) {
      JOptionPane.showMessageDialog(parent, "Error while saving " + resource.getResourceEntry().toString(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      return false;
    }
    return true;
  }
}
