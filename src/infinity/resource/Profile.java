// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import infinity.NearInfinity;
import infinity.util.io.FileNI;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Provides engine- and game-specific properties of the currently opened Infinity Engine game.<br>
 * <br>
 * Properties can be accessed by unique identifiers. The returned property can be
 * of any type defined by the enum {@link Profile.Type} or <code>null</code>.
 */
public final class Profile
{
  /** Supported data types for properties. */
  public enum Type {
    /** Property data is of type {@link Boolean}. */
    Boolean,
    /** Property data is of type {@link Integer}. */
    Integer,
    /** Property data is of type {@link String}. */
    String,
    /** Property data is of type {@link java.util.List}. */
    List,
    /** Property data is of type {@link java.io.File}. */
    File,
    /** Property data is of any custom data type. */
    Object
  }

  /** Supported Infinity Engine games. */
  public enum Game {
    Unknown,
    BG1, BG1TotSC,
    BG2SoA, BG2ToB, Tutu, BGT,
    PST,
    IWD, IWDHoW, IWDHowToTLM,
    IWD2,
    BG1EE, BG2EE, IWDEE, EET
  }

  /** Supported Infinity Engine types by feature levels. */
  public enum Engine {
    /** Default for unrecognized or unsupported games. */
    Unknown,
    /** Includes BG1 and BG1TotSC. */
    BG1,
    /** Includes BG2SoA, BG2ToB, Tutu and BGT. */
    BG2,
    /** Includes PST. */
    PST,
    /** Includes IWD, IWDHoW and IWDHowTotLM. */
    IWD,
    /** Includes IWD2. */
    IWD2,
    /** Includes BG1EE, BG2EE, IWDEE and EET. */
    EE
  }

  /*
   * Note: Use positive key values for properties which require NO additional parameters.
   *       Use negative key values for properties which require an additional parameter.
   *       Properties with additional parameters must be handled directly in the static
   *       method getProperty(int, Object).
   */

  // Static properties
  /** Property: (String) List of supported games. */
  public static final int GET_GLOBAL_NEARINFINITY_VERSION     = 1;
  /** Property: (List&lt;Game&gt;) List of supported games. */
  public static final int GET_GLOBAL_GAMES                    = 2;
  /** Property: (String) The name of the override folder ("<code>Override</code>"). */
  public static final int GET_GLOBAL_OVERRIDE_NAME            = 3;
  /** Property: (String) Returns "<code>dialog.tlk</code>". */
  public static final int GET_GLOBAL_DIALOG_NAME              = 4;
  /** Property: (String) Returns "<code>dialogf.tlk</code>". */
  public static final int GET_GLOBAL_DIALOG_NAME_FEMALE       = 5;

  // Static properties which require an additional parameter.
  /** Property: (String) Returns the game's title. Extra parameter: Desired {@link Game}. */
  public static final int GET_GLOBAL_GAME_TITLE               = -1;
  /** Property: (List&lt;String&gt;) Returns a list of extra folders for the specified game.
   *            Extra parameter: Desired {@link Game}. */
  public static final int GET_GLOBAL_EXTRA_FOLDERS            = -2;
  /** Property: (String) Returns the game's home folder name.
   *            Extra parameter: Desired <em>Enhanced Edition</em> {@link Game}. */
  public static final int GET_GLOBAL_HOME_FOLDER              = -3;

  // Properties set at runtime
  /** Property: ({@link Game}) Game identifier. */
  public static final int GET_GAME_TYPE                       = 100;
  /** Property: ({@link Engine}) Engine identifier. */
  public static final int GET_GAME_ENGINE                     = 101;
  /** Property: (String) Name of the game's root folder. */
  public static final int GET_GAME_ROOT_FOLDER_NAME           = 102;
  /** Property: (String) Name of the game's home folder. (Enhanced Editions only) */
  public static final int GET_GAME_HOME_FOLDER_NAME           = 103;
  /** Property: (String) Name of the currently selected game language folder. (Enhanced Editions only) */
  public static final int GET_GAME_LANG_FOLDER_NAME           = 104;
  /** Property: (List&lt;String&gt;) List of available languages as language code
   *            for the current game. (Enhanced Editions only) */
  public static final int GET_GAME_LANG_FOLDER_NAMES_AVAILABLE  = 105;
  /** Property: (List&lt;File&gt;) List of valid root folder, sorted by priority in descending order. */
  public static final int GET_GAME_FOLDERS                    = 106;
  /** Property: (File) Game's root folder. */
  public static final int GET_GAME_ROOT_FOLDER                = 107;
  /** Property: (File) Game's home folder. (Enhanced Editions only) */
  public static final int GET_GAME_HOME_FOLDER                = 108;
  /** Property: (File) Game's language folder. (Enhanced Editions only) */
  public static final int GET_GAME_LANG_FOLDER                = 109;
  /** Property: (List<&lt;File&gt;) List of available game language folders. (Enhanced Editions only) */
  public static final int GET_GAME_LANG_FOLDERS_AVAILABLE     = 110;
  /** Property: (File) Game's language root folder (where the actual language subfolder reside).
   *            (Enhanced Editions only) */
  public static final int GET_GAME_LANG_FOLDER_BASE           = 111;
  /** Property: (List&lt;File&gt;) List of override folders to search for game resources,
   *            sorted by priority in ascending order. */
  public static final int GET_GAME_OVERRIDE_FOLDERS           = 112;
  /** Property: (List&lt;String&gt;) List of extra folders containing game-related resources,
   *            sorted alphabetically in ascending order. */
  public static final int GET_GAME_EXTRA_FOLDERS              = 113;
  /** Property: (File) The game's chitin.key. */
  public static final int GET_GAME_CHITIN_KEY                 = 114;
  /** Property: (String) Title of the game. */
  public static final int GET_GAME_TITLE                      = 115;
  /** Property: (String) A short user-defined description or name of the game.
   *            Can be used to tell specific game installations apart. */
  public static final int GET_GAME_DESC                       = 116;
  /** Property: (String) Name of the game's ini file. */
  public static final int GET_GAME_INI_NAME                   = 117;
  /** Property: (File) Path of the game's ini file. */
  public static final int GET_GAME_INI_FILE                   = 118;
  /** Property: (File) Path to the currently selected <code>dialog.tlk</code>. */
  public static final int GET_GAME_DIALOG_FILE                = 120;
  /** Property: (File) Path to the currently selected female <code>dialogf.tlk</code>.
   *            Returns <code>null</code> if the language does not require a dialogf.tlk. */
  public static final int GET_GAME_DIALOGF_FILE               = 121;
  /** Property: (List&lt;File&gt;) Unsorted list of extra folders containing BIFF archives.
   *            (Non-Enhanced Editions only) */
  public static final int GET_GAME_BIFF_FOLDERS               = 122;
  /** Property: (Boolean) Is game an Enhanced Edition game? */
  public static final int IS_ENHANCED_EDITION                 = 123;

  /** Property: (Boolean) Are <code>2DA</code> resources supported? */
  public static final int IS_SUPPORTED_2DA                    = 1001;
  /** Property: (Boolean) Are <code>ACM</code> resources supported? */
  public static final int IS_SUPPORTED_ACM                    = 1002;
  /** Property: (Boolean) Are <code>ARE V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_ARE_V10                = 1003;
  /** Property: (Boolean) Are <code>ARE V9.1</code> resources supported? */
  public static final int IS_SUPPORTED_ARE_V91                = 1004;
  /** Property: (Boolean) Are <code>BAM V1</code> resources supported? */
  public static final int IS_SUPPORTED_BAM_V1                 = 1005;
  /** Property: (Boolean) Are <code>BAM V1</code> resources supported? */
  public static final int IS_SUPPORTED_BAMC_V1                = 1006;
  /** Property: (Boolean) Are <code>BAM V2</code> resources supported? */
  public static final int IS_SUPPORTED_BAM_V2                 = 1007;
  /** Property: (Boolean) Are <code>BCS</code> resources supported? */
  public static final int IS_SUPPORTED_BCS                    = 1008;
  /** Property: (Boolean) Are uncompressed <code>BIFF V1</code> resources supported? */
  public static final int IS_SUPPORTED_BIFF                   = 1009;
  /** Property: (Boolean) Are compressed <code>BIF V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_BIF                    = 1010;
  /** Property: (Boolean) Are compressed <code>BIFC V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_BIFC                   = 1011;
  /** Property: (Boolean) Are <code>BIK</code> resources supported? */
  public static final int IS_SUPPORTED_BIK                    = 1012;
  /** Property: (Boolean) Are <code>BIO</code> resources supported? */
  public static final int IS_SUPPORTED_BIO                    = 1013;
  /** Property: (Boolean) Are paletted <code>BMP</code> resources supported? */
  public static final int IS_SUPPORTED_BMP_PAL                = 1014;
  /** Property: (Boolean) Are alpha-blended <code>BMP</code> resources supported? */
  public static final int IS_SUPPORTED_BMP_ALPHA              = 1015;
  /** Property: (Boolean) Are <code>CHR V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_CHR_V10                = 1016;
  /** Property: (Boolean) Are <code>CHR V1.2</code> resources supported? */
  public static final int IS_SUPPORTED_CHR_V12                = 1017;
  /** Property: (Boolean) Are <code>CHR V2.0</code> resources supported? */
  public static final int IS_SUPPORTED_CHR_V20                = 1018;
  /** Property: (Boolean) Are <code>CHR V2.1</code> resources supported? */
  public static final int IS_SUPPORTED_CHR_V21                = 1019;
  /** Property: (Boolean) Are <code>CHR V2.2</code> resources supported? */
  public static final int IS_SUPPORTED_CHR_V22                = 1020;
  /** Property: (Boolean) Are <code>CHU</code> resources supported? */
  public static final int IS_SUPPORTED_CHU                    = 1022;
  /** Property: (Boolean) Are <code>CRE V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_CRE_V10                = 1023;
  /** Property: (Boolean) Are <code>CRE V1.2</code> resources supported? */
  public static final int IS_SUPPORTED_CRE_V12                = 1024;
  /** Property: (Boolean) Are <code>CRE V2.2</code> resources supported? */
  public static final int IS_SUPPORTED_CRE_V22                = 1025;
  /** Property: (Boolean) Are <code>CRE V9.0</code> resources supported? */
  public static final int IS_SUPPORTED_CRE_V90                = 1026;
  /** Property: (Boolean) Are <code>DLG</code> resources supported? */
  public static final int IS_SUPPORTED_DLG                    = 1027;
  /** Property: (Boolean) Are <code>EFF</code> resources supported? */
  public static final int IS_SUPPORTED_EFF                    = 1028;
  /** Property: (Boolean) Are <code>FNT</code> resources supported? */
  public static final int IS_SUPPORTED_FNT                    = 1029;
  /** Property: (Boolean) Are <code>GAM V1.1</code> resources supported? */
  public static final int IS_SUPPORTED_GAM_V11                = 1030;
  /** Property: (Boolean) Are <code>GAM V2.0</code> resources supported? */
  public static final int IS_SUPPORTED_GAM_V20                = 1031;
  /** Property: (Boolean) Are <code>GAM V2.1</code> resources supported? */
  public static final int IS_SUPPORTED_GAM_V21                = 1032;
  /** Property: (Boolean) Are <code>GAM V2.2</code> resources supported? */
  public static final int IS_SUPPORTED_GAM_V22                = 1033;
  /** Property: (Boolean) Are <code>GLSL</code> resources supported? */
  public static final int IS_SUPPORTED_GLSL                   = 1034;
  /** Property: (Boolean) Are <code>GUI</code> resources supported? */
  public static final int IS_SUPPORTED_GUI                    = 1035;
  /** Property: (Boolean) Are <code>IDS</code> resources supported? */
  public static final int IS_SUPPORTED_IDS                    = 1036;
  /** Property: (Boolean) Are <code>INI</code> resources supported? */
  public static final int IS_SUPPORTED_INI                    = 1037;
  /** Property: (Boolean) Are <code>ITM V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_ITM_V10                = 1038;
  /** Property: (Boolean) Are <code>ITM V1.1</code> resources supported? */
  public static final int IS_SUPPORTED_ITM_V11                = 1039;
  /** Property: (Boolean) Are <code>ITM V2.0</code> resources supported? */
  public static final int IS_SUPPORTED_ITM_V20                = 1040;
  /** Property: (Boolean) Are <code>KEY</code> resources supported? */
  public static final int IS_SUPPORTED_KEY                    = 1041;
  /** Property: (Boolean) Are <code>MOS V1</code> resources supported? */
  public static final int IS_SUPPORTED_MOS_V1                 = 1042;
  /** Property: (Boolean) Are <code>MOSC V1</code> resources supported? */
  public static final int IS_SUPPORTED_MOSC_V1                = 1043;
  /** Property: (Boolean) Are <code>MOS V2</code> resources supported? */
  public static final int IS_SUPPORTED_MOS_V2                 = 1044;
  /** Property: (Boolean) Are <code>MUS</code> resources supported? */
  public static final int IS_SUPPORTED_MUS                    = 1045;
  /** Property: (Boolean) Are <code>MVE</code> resources supported? */
  public static final int IS_SUPPORTED_MVE                    = 1046;
  /** Property: (Boolean) Are <code>OGG</code> resources supported? */
  public static final int IS_SUPPORTED_OGG                    = 1047;
  /** Property: (Boolean) Are <code>PLT</code> resources supported? */
  public static final int IS_SUPPORTED_PLT                    = 1048;
  /** Property: (Boolean) Are <code>PVRZ</code> resources supported? */
  public static final int IS_SUPPORTED_PVRZ                   = 1049;
  /** Property: (Boolean) Are <code>PRO</code> resources supported? */
  public static final int IS_SUPPORTED_PRO                    = 1050;
  /** Property: (Boolean) Are <code>RES</code> resources supported? */
  public static final int IS_SUPPORTED_RES                    = 1051;
  /** Property: (Boolean) Are <code>SAV</code> resources supported? */
  public static final int IS_SUPPORTED_SAV                    = 1052;
  /** Property: (Boolean) Are <code>SPL V1</code> resources supported? */
  public static final int IS_SUPPORTED_SPL_V1                 = 1053;
  /** Property: (Boolean) Are <code>SPL V2</code> resources supported? */
  public static final int IS_SUPPORTED_SPL_V2                 = 1054;
  /** Property: (Boolean) Are <code>SQL</code> resources supported? */
  public static final int IS_SUPPORTED_SQL                    = 1055;
  /** Property: (Boolean) Are (PST) <code>SRC</code> resources supported? */
  public static final int IS_SUPPORTED_SRC_PST                = 1056;
  /** Property: (Boolean) Are (IWD2) <code>SRC</code> resources supported? */
  public static final int IS_SUPPORTED_SRC_IWD2               = 1057;
  /** Property: (Boolean) Are <code>STO V1.0</code> resources supported? */
  public static final int IS_SUPPORTED_STO_V10                = 1058;
  /** Property: (Boolean) Are <code>STO V1.1</code> resources supported? */
  public static final int IS_SUPPORTED_STO_V11                = 1059;
  /** Property: (Boolean) Are <code>STO V9.0</code> resources supported? */
  public static final int IS_SUPPORTED_STO_V90                = 1060;
  /** Property: (Boolean) Are (palette-based) <code>TIS V1</code> resources supported? */
  public static final int IS_SUPPORTED_TIS_V1                 = 1061;
  /** Property: (Boolean) Are (PVRZ-based) <code>TIS V2</code> resources supported? */
  public static final int IS_SUPPORTED_TIS_V2                 = 1062;
  /** Property: (Boolean) Are <code>TLK</code> resources supported? */
  public static final int IS_SUPPORTED_TLK                    = 1063;
  /** Property: (Boolean) Are <code>TO V1</code> (TOH/TOT) resources supported? */
  public static final int IS_SUPPORTED_TO_V1                  = 1064;
  /** Property: (Boolean) Are <code>TO V2</code> (TOH only) resources supported? */
  public static final int IS_SUPPORTED_TO_V2                  = 1065;
  /** Property: (Boolean) Are <code>VAR</code> resources supported? */
  public static final int IS_SUPPORTED_VAR                    = 1066;
  /** Property: (Boolean) Are <code>VEF</code> resources supported? */
  public static final int IS_SUPPORTED_VEF                    = 1067;
  /** Property: (Boolean) Are <code>VVC</code> resources supported? */
  public static final int IS_SUPPORTED_VVC                    = 1068;
  /** Property: (Boolean) Are <code>WAV</code> resources supported? */
  public static final int IS_SUPPORTED_WAV                    = 1069;
  /** Property: (Boolean) Are <code>WAVC</code> resources supported? */
  public static final int IS_SUPPORTED_WAVC                   = 1070;
  /** Property: (Boolean) Are <code>WBM</code> resources supported? */
  public static final int IS_SUPPORTED_WBM                    = 1071;
  /** Property: (Boolean) Are <code>WED</code> resources supported? */
  public static final int IS_SUPPORTED_WED                    = 1072;
  /** Property: (Boolean) Are <code>WFX</code> resources supported? */
  public static final int IS_SUPPORTED_WFX                    = 1073;
  /** Property: (Boolean) Are <code>WMP</code> resources supported? */
  public static final int IS_SUPPORTED_WMP                    = 1074;

  /** Property: (Boolean) Are Kits supported? */
  public static final int IS_SUPPORTED_KITS                   = 2001;
  /** Property: (String) The name of the ALIGNMENT IDS resource. */
  public static final int GET_IDS_ALIGNMENT                   = 2002;
  /** Property: (Boolean) Indices whether overlays in tilesets are stenciled. */
  public static final int IS_TILESET_STENCILED                = 2003;


  // Container for Property entries
  private static final HashMap<Integer, Profile.Property> properties = new HashMap<Integer, Profile.Property>(200);
  // Unique titles for all supported games
  private static final EnumMap<Game, String> GAME_TITLE = new EnumMap<Game, String>(Game.class);
  // List of supported extra folders for all supported games
  private static final EnumMap<Game, List<String>> GAME_EXTRA_FOLDERS = new EnumMap<Game, List<String>>(Game.class);
  // Home folder name for Enhanced Edition Games
  private static final EnumMap<Game, String> GAME_HOME_FOLDER = new EnumMap<Game, String>(Game.class);

  // Using the singleton approach
  private static Profile instance = null;

  static {
    // initializing game titles
    GAME_TITLE.put(Game.Unknown, "Unknown game");
    GAME_TITLE.put(Game.BG1, "Baldur's Gate");
    GAME_TITLE.put(Game.BG1TotSC, "Baldur's Gate: Tales of the Sword Coast");
    GAME_TITLE.put(Game.BG2SoA, "Baldur's Gate II: Shadows of Amn");
    GAME_TITLE.put(Game.BG2ToB, "Baldur's Gate II: Throne of Bhaal");
    GAME_TITLE.put(Game.Tutu, "Baldur's Gate - Tutu");
    GAME_TITLE.put(Game.BGT, "Baldur's Gate Trilogy");
    GAME_TITLE.put(Game.PST, "Planescape: Torment");
    GAME_TITLE.put(Game.IWD, "Icewind Dale");
    GAME_TITLE.put(Game.IWDHoW, "Icewind Dale: Heart of Winter");
    GAME_TITLE.put(Game.IWDHowToTLM, "Icewind Dale: Trials of the Luremaster");
    GAME_TITLE.put(Game.IWD2, "Icewind Dale II");
    GAME_TITLE.put(Game.BG1EE, "Baldur's Gate: Enhanced Edition");
    GAME_TITLE.put(Game.BG2EE, "Baldur's Gate II: Enhanced Edition");
    GAME_TITLE.put(Game.IWDEE, "Icewind Dale: Enhanced Edition");
    GAME_TITLE.put(Game.EET, "Baldur's Gate - Enhanced Edition Trilogy");

    // initializing extra folders for each supported game
    final String[] PST_EXTRA_FOLDERS = { "Music", "Save", "Temp" };
    final String[] BG_EXTRA_FOLDERS = { "Characters", "MPSave", "Music", "Portraits", "Save", "Screenshots",
                                        "Scripts", "ScrnShot", "Sounds", "Temp", "TempSave" };
    final String[] BGEE_EXTRA_FOLDERS = { "BPSave", "Characters", "Fonts", "Movies", "MPSave", "MPBPSave",
                                          "Music", "Portraits", "Save", "Sounds", "ScrnShot", "Scripts",
                                          "Temp", "TempSave" };
    final String[] IWDEE_EXTRA_FOLDERS = { "Characters", "Fonts", "Movies", "MPSave", "Music", "Portraits",
                                           "Save", "Sounds", "ScrnShot", "Scripts", "Temp", "TempSave" };
    GAME_EXTRA_FOLDERS.put(Game.Unknown, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG1, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG1TotSC, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG2SoA, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG2ToB, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.Tutu, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BGT, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.PST, Arrays.asList(PST_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWD, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWDHoW, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWDHowToTLM, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWD2, Arrays.asList(BG_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG1EE, Arrays.asList(BGEE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG2EE, Arrays.asList(BGEE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWDEE, Arrays.asList(IWDEE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.EET, Arrays.asList(BGEE_EXTRA_FOLDERS));

    // initializing home folder names for Enhanced Edition games
    GAME_HOME_FOLDER.put(Game.BG1EE, "Baldur's Gate - Enhanced Edition");
    GAME_HOME_FOLDER.put(Game.BG2EE, "Baldur's Gate II - Enhanced Edition");
    GAME_HOME_FOLDER.put(Game.EET, GAME_HOME_FOLDER.get(Game.BG2EE));
    GAME_HOME_FOLDER.put(Game.IWDEE, "Icewind Dale - Enhanced Edition");

    // static properties are always available
    initStaticProperties();
  }

  /**
   * Converts the string representation of a game type into a {@link Game} enum type.
   * Falls back to <code>Game.Unknown</code> on error.
   */
  public static Game gameFromString(String gameName)
  {
    if (gameName != null && !gameName.isEmpty()) {
      try {
        return Enum.valueOf(Game.class, gameName);
      } catch (IllegalArgumentException e) {
        System.err.println("Unknown game type \"" + gameName + "\" specified. Falling back to \"Unknown\".");
      }
    }
    return Game.Unknown;
  }

  /**
   * Initializes properties of a new game.
   * @param keyFile Full path to the chitin.key of the opened game.
   * @return <code>true</code> if the game has been initialized successfully, <code>false</code> otherwise.
   */
  public static boolean openGame(File keyFile)
  {
    return openGame(keyFile, null);
  }

  /**
   * Initializes properties of a new game.
   * @param keyFile Full path to the chitin.key of the opened game.
   * @param desc An optional description associated with the game.
   * @return <code>true</code> if the game has been initialized successfully, <code>false</code> otherwise.
   */
  public static boolean openGame(File keyFile, String desc)
  {
    try {
      closeGame();
      instance = new Profile(keyFile, desc);
      return true;
    } catch (Exception e) {
      e.printStackTrace();
    }
    closeGame();
    return false;
  }

  /**
   * Returns whether a game has been initialized.
   * @return <code>true</code> if a game has been initialized, <code>false</code> if not.
   */
  public static boolean isGameOpen()
  {
    return (instance != null);
  }

  /**
   * Returns whether a property of the specified key is available.
   * @param key A key to identify the property.
   * @return <code>true</code> if the key is pointing to an existing property, <code>false</code> otherwise.
   */
  public static boolean hasProperty(int key)
  {
    return properties.containsKey(Integer.valueOf(key));
  }

  /**
   * Returns the data type of the specified property.
   * @param key The unique key identifying the property.
   * @return The data type of the property, or <code>null</code> if not available.
   */
  public static Type getPropertyType(int key)
  {
    Property prop = getEntry(key);
    if (prop != null) {
      return prop.getType();
    } else {
      return null;
    }
  }

  /**
   * Returns the actual data of the specified property.
   * @param key The unique key identifying the property.
   * @return The data of the property, or <code>null</code> if not available.
   */
  public static Object getProperty(int key)
  {
    return getProperty(key, null);
  }

  /**
   * Returns the actual data of the specified property.
   * @param key The unique key identifying the property.
   * @param param An additional parameter required by a small number of properties.
   *              Specify <code>null</code> to always return the parent structure of the property.
   * @return The data of the property, or <code>null</code> if not available.
   */
  public static Object getProperty(int key, Object param)
  {
    Property prop = getEntry(key);
    if (prop != null) {
      if (key >= 0 || param == null) {
        return prop.getData();
      } else {
        // handling properties which require an additional parameter
        switch (key) {
          case GET_GLOBAL_GAME_TITLE:
            if (param instanceof Game) {
              return ((EnumMap<?, ?>)prop.getData()).get(param);
            }
            break;
          case GET_GLOBAL_EXTRA_FOLDERS:
            if (param instanceof Game) {
              return ((EnumMap<?, ?>)prop.getData()).get(param);
            }
            break;
          case GET_GLOBAL_HOME_FOLDER:
            if (param instanceof Game) {
              return ((EnumMap<?, ?>)prop.getData()).get(param);
            }
            break;
        }
      }
    }
    return null;
  }

  /**
   * Updates the value of the Property entry specified by <code>key</code> and returns
   * the previous Property value.
   * @param key The Property key.
   * @param data The new value of the Property instance.
   * @return the previous value of the specified Property, or <code>null</code> if not available or applicable.
   */
  public static Object updateProperty(int key, Object data)
  {
    // Properties requiring extra parameters cannot be updated
    if (key >= 0) {
      Property prop = properties.get(Integer.valueOf(key));
      if (prop != null) {
        return prop.setData(data);
      }
    }
    return null;
  }

  /**
   * Adds or updates a Property entry.
   * @param key The Property key.
   * @param type The data type.
   * @param data The data of the Property instance.
   * @return <code>true</code> if Property has been added or updated, <code>false</code> otherwise.
   */
  public static boolean addProperty(int key, Type type, Object data)
  {
    if (key >= 0) {
      if (properties.containsKey(Integer.valueOf(key))) {
        updateProperty(key, data);
        return true;
      } else {
        addEntry(key, type, data);
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the name of the default override folder (without path). Does not need an opened game
   * to return a valid value.
   * @return The name of the override folder.
   */
  public static String getOverrideFolderName()
  {
    return (String)getProperty(GET_GLOBAL_OVERRIDE_NAME);
  }

  /**
   * Returns the game type of the current game.
   * @return The {@link Game} type of the current game or <code>Game.Unknown</code> otherwise.
   */
  public static Game getGame()
  {
    Object ret = getProperty(GET_GAME_TYPE);
    return (ret instanceof Game) ? (Game)ret : Game.Unknown;
  }

  /**
   * Returns the engine type of the current game.
   * @return The {@link Engine} type of the current game or <code>Engine.Unknown</code> otherwise.
   */
  public static Engine getEngine()
  {
    Object ret = getProperty(GET_GAME_ENGINE);
    return (ret instanceof Engine) ? (Engine)ret : Engine.Unknown;
  }

  /**
   * Returns whether the current game is an Enhanced Edition game.
   * @return <code>true</code> if the current game is an Enhanced Edition Game, <code>false> otherwise.
   */
  public static boolean isEnhancedEdition()
  {
    Object ret = getProperty(IS_ENHANCED_EDITION);
    return (ret instanceof Boolean) ? (Boolean)ret : false;
  }

  /**
   * Returns the game's root folder.
   * @return The game's root folder as File object.
   */
  public static File getGameRoot()
  {
    return (File)getProperty(GET_GAME_ROOT_FOLDER);
  }

  /**
   * Returns the game's home folder. A Non-enhanced Edition game will always return the game's
   * root folder instead.
   * @return The game's home folder as File object.
   */
  public static File getHomeRoot()
  {
    Object ret = getProperty(GET_GAME_HOME_FOLDER);
    return (ret instanceof File) ? (File)ret : getGameRoot();
  }

  /**
   * Returns the game's language folder (where the dialog.tlk is located).
   * A Non-enhanced Edition game will always return the game's root folder instead.
   * @return The game's language folder as File object.
   */
  public static File getLanguageRoot()
  {
    Object ret = getProperty(GET_GAME_LANG_FOLDER);
    return (ret instanceof File) ? (File)ret : getGameRoot();
  }

  /**
   * Returns all available root folders of the game as a list of File objects
   * (which includes the home and language root for Enhanced Edition games).
   * @return A list of File objects specifying the game's root folders.
   */
  @SuppressWarnings("unchecked")
  public static List<File> getRootFolders()
  {
    List<?> ret = (List<?>)getProperty(GET_GAME_FOLDERS);
    if (ret instanceof List) {
      return (List<File>)ret;
    } else {
      List<File> list = new ArrayList<File>();
      list.add(getGameRoot());
      return list;
    }
  }

  /**
   * Returns the full path to the chitin.key of the currently open game.
   * @return The full path of the chitin.key as File object.
   */
  public static File getChitinKey()
  {
    Object ret = getProperty(GET_GAME_CHITIN_KEY);
    return (ret instanceof File) ? (File)ret : null;
  }

  /**
   * Updates language-related Properties with the specified game language. (Enhanced Editions only)
   * @param language The name of the language subfolder.
   * @return <code>true</code> if the game language has been updated successfully, <code>false</code> otherwise.
   */
  public static boolean updateGameLanguage(String language)
  {
    if (isEnhancedEdition() && language != null) {
      List<?> languages = (List<?>)getProperty(GET_GAME_LANG_FOLDER_NAMES_AVAILABLE);
      for (Iterator<?> iter = languages.iterator(); iter.hasNext();) {
        String curLang = (String)iter.next();
        if (curLang.equalsIgnoreCase(language)) {
          // updating language names and folders
          updateProperty(GET_GAME_LANG_FOLDER_NAME, curLang);
          File langPath = new FileNI((File)getProperty(GET_GAME_LANG_FOLDER_BASE), curLang);
          updateProperty(GET_GAME_LANG_FOLDER, langPath);
          // updating dialog.tlks
          updateProperty(GET_GAME_DIALOG_FILE, new FileNI(langPath, (String)getProperty(GET_GLOBAL_DIALOG_NAME)));
          File femaleTlkFile = new FileNI(langPath, (String)getProperty(GET_GLOBAL_DIALOG_NAME_FEMALE));
          if (femaleTlkFile.isFile()) {
            addProperty(GET_GAME_DIALOGF_FILE, Type.File, femaleTlkFile);
          } else {
            updateProperty(GET_GAME_DIALOGF_FILE, null);
          }
          return true;
        }
      }
    }
    return false;
  }

  // Returns the Property object assigned to the given key.
  private static Property getEntry(int key)
  {
    return properties.get(Integer.valueOf(key));
  }

  // Adds a new Property entry to the list.
  private static void addEntry(int key, Type type, Object data)
  {
    Property prop = new Property(key, type, data);
    properties.put(prop.getKey(), prop);
  }

  // Cleans up data when closing a game profile
  private static void closeGame()
  {
    properties.clear();
    initStaticProperties();
    instance = null;
  }

  // Initializes properties not related to a specific game
  private static void initStaticProperties()
  {
    // setting current NI version
    addEntry(GET_GLOBAL_NEARINFINITY_VERSION, Type.String, NearInfinity.getVersion());

    // setting list of supported games and associated data
    List<Game> gameList = new ArrayList<Game>();
    for (Game game : Game.values()) {
      gameList.add(game);
    }
    addEntry(GET_GLOBAL_GAMES, Type.List, gameList);
    addEntry(GET_GLOBAL_GAME_TITLE, Type.String, GAME_TITLE);
    addEntry(GET_GLOBAL_EXTRA_FOLDERS, Type.List, GAME_EXTRA_FOLDERS);
    addEntry(GET_GLOBAL_HOME_FOLDER, Type.String, GAME_HOME_FOLDER);

    // setting default override folder name
    addEntry(GET_GLOBAL_OVERRIDE_NAME, Type.String, "Override");

    // setting dialog.tlk file names
    addEntry(GET_GLOBAL_DIALOG_NAME, Type.String, "dialog.tlk");
    addEntry(GET_GLOBAL_DIALOG_NAME_FEMALE, Type.String, "dialogf.tlk");
  }


  private Profile(File keyFile, String desc) throws Exception
  {
    init(keyFile, desc);
  }

  // Initializes profile
  private void init(File keyFile, String desc) throws Exception
  {
    if (keyFile == null) {
      throw new Exception("No chitin.key specified");
    } else if (!keyFile.isFile()) {
      throw new Exception(keyFile.toString() + " does not exist");
    }

    if (desc != null) {
      addEntry(GET_GAME_DESC, Type.String, desc);
    }

    // adding chitin.key path
    addEntry(GET_GAME_CHITIN_KEY, Type.File, keyFile);

    // adding game's root folder and name
    File rootDir = keyFile.getAbsoluteFile().getParentFile();
    addEntry(GET_GAME_ROOT_FOLDER, Type.File, rootDir);
    addEntry(GET_GAME_ROOT_FOLDER_NAME, Type.String, rootDir.getName());

    initGame();
  }

  private void initGame() throws Exception
  {
    // Main game detection
    Game game;

    File gameRoot = (File)getProperty(GET_GAME_ROOT_FOLDER);
    if (new FileNI(gameRoot, "movies/howseer.wbm").isFile()) {
      game = Game.IWDEE;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
    } else if (new FileNI(gameRoot, "movies/pocketzz.wbm").isFile()) {
      game = Game.BG2EE;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
    } else if (new FileNI(gameRoot, "movies/bgenter.wbm").isFile()) {
      game = Game.BG1EE;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
    } else if (new FileNI(gameRoot, "torment.exe").isFile() &&
               !(new FileNI(gameRoot, "movies/sigil.wbm").isFile())) {
      game = Game.PST;
      addEntry(GET_GAME_INI_NAME, Type.String, "torment.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "idmain.exe").isFile() &&
               !(new FileNI(gameRoot, "movies/howseer.wbm").isFile())) {
      game = Game.IWD;
      addEntry(GET_GAME_INI_NAME, Type.String, "icewind.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "iwd2.exe").isFile() &&
               new FileNI(gameRoot, "Data/Credits.mve").isFile()) {
      game = Game.IWD2;
      addEntry(GET_GAME_INI_NAME, Type.String, "icewind2.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "baldur.exe").isFile() &&
               new FileNI(gameRoot, "BGConfig.exe").isFile()) {
      game = Game.BG2SoA;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "movies/graphsim.mov").isFile() || // Mac BG1 detection hack
               (new FileNI(gameRoot, "baldur.exe").isFile() && new FileNI(gameRoot, "Config.exe").isFile())) {
      game = Game.BG1;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "bg1tutu.exe").isFile()) {
      game = Game.Tutu;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    } else {
      // game == Game.Unknown
      game = Game.Unknown;
      addEntry(GET_GAME_INI_NAME, Type.String, "baldur.ini");
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(gameRoot, (String)getProperty(GET_GAME_INI_NAME)));
    }
    // adding priliminary game type into storage
    addEntry(GET_GAME_TYPE, Type.Object, game);
    addEntry(GET_GAME_EXTRA_FOLDERS, Type.List, GAME_EXTRA_FOLDERS.get(game));

    // determining game engine
    initGameEngine();

    boolean isEE = (game == Game.BG1EE || game == Game.BG2EE || game == Game.IWDEE);
    addEntry(IS_ENHANCED_EDITION, Type.Boolean, Boolean.valueOf(isEE));

    if (isEE) {
      File langDir = new FileNI(gameRoot, "lang");
      if (langDir.isDirectory()) {
        addEntry(GET_GAME_LANG_FOLDER_BASE, Type.File, langDir);
      }
    }

    if (GAME_HOME_FOLDER.containsKey(game)) {
      addEntry(GET_GAME_HOME_FOLDER_NAME, Type.String, GAME_HOME_FOLDER.get(game));
    }

    // Considering three different root folders to locate game resources
    // Note: Order of the root directories is important. FileNI will take the first one available.
    File homeRoot = ResourceFactory.getHomeRoot();
    String language = ResourceFactory.fetchGameLanguage(new FileNI(homeRoot, (String)getProperty(GET_GAME_INI_NAME)));
    File langRoot = FileNI.getFile((File)getProperty(GET_GAME_LANG_FOLDER_BASE), language);
    if (!langRoot.isDirectory()) {
      langRoot = null;
    }
    List<File> listRoots = new ArrayList<File>();
    if (langRoot != null) {
      addEntry(GET_GAME_LANG_FOLDER_NAME, Type.String, language);
      addEntry(GET_GAME_LANG_FOLDER, Type.File, langRoot);
      List<File> langPaths = ResourceFactory.getAvailableGameLanguages();
      addEntry(GET_GAME_LANG_FOLDERS_AVAILABLE, Type.List, langPaths);
      List<String> languages = new ArrayList<String>(langPaths.size());
      for (Iterator<File> iter = langPaths.iterator(); iter.hasNext();) {
        languages.add(iter.next().getName());
      }
      addEntry(GET_GAME_LANG_FOLDER_NAMES_AVAILABLE, Type.List, languages);
      listRoots.add(langRoot);
    }
    if (homeRoot != null) {
      addEntry(GET_GAME_HOME_FOLDER, Type.File, homeRoot);
      addEntry(GET_GAME_INI_FILE, Type.File, new FileNI(homeRoot, (String)getProperty(GET_GAME_INI_NAME)));
      listRoots.add(homeRoot);
    }
    listRoots.add(gameRoot);
    addEntry(GET_GAME_FOLDERS, Type.File, listRoots);

    // initializing dialog.tlk and dialogf.tlk
    File tlk = FileNI.getFile((List<?>)getProperty(GET_GAME_FOLDERS),
                              (String)getProperty(GET_GLOBAL_DIALOG_NAME));
    if (tlk != null && tlk.isFile()) {
      addEntry(GET_GAME_DIALOG_FILE, Type.File, tlk);
    }
    tlk = FileNI.getFile((List<?>)getProperty(GET_GAME_FOLDERS),
                         (String)getProperty(GET_GLOBAL_DIALOG_NAME_FEMALE));
    if (tlk != null && tlk.isFile()) {
      addEntry(GET_GAME_DIALOGF_FILE, Type.File, tlk);
    }

    // Initializing resource structure
    ResourceFactory.openGame((File)getProperty(GET_GAME_CHITIN_KEY));

    // Expansion pack detection
    if (game == Game.IWD && ResourceFactory.resourceExists("HOWDRAG.MVE")) {
      // detect Trials of the Luremaster
      if (ResourceFactory.resourceExists("AR9715.ARE")) {
        game = Game.IWDHowToTLM;
      } else {
        game = Game.IWDHoW;
      }
    } else if (game == Game.BG2SoA && ResourceFactory.resourceExists("SARADUSH.MVE")) {
      // detect BG Trilogy
      if (ResourceFactory.resourceExists("ARU000.ARE")) {
        game = Game.BGT;
      } else {
        game = Game.BG2ToB;
      }
    } else if (game == Game.BG1 && ResourceFactory.resourceExists("DURLAG.MVE")) {
      game = Game.BG1TotSC;
    } else if (game == Game.BG2EE && ResourceFactory.resourceExists("FH2600.ARE")) {
      // Note: EET is still in development
      game = Game.EET;
    }

    // updating game type
    addEntry(GET_GAME_TYPE, Type.Object, game);
    addEntry(GET_GAME_TITLE, Type.String, GAME_TITLE.get(game));

    // initializing list of folders containing BIFF archives
    List<File> biffDirs = ResourceFactory.getBIFFDirs();
    if (biffDirs != null && !biffDirs.isEmpty()) {
      addEntry(GET_GAME_BIFF_FOLDERS, Type.List, biffDirs);
    }

    // initializing list of available override folders
    initOverrides();

    // initializing supported resource types
    initResourceTypes();

    // initializing engine-specific traits
    initFeatures();
  }

  // Initializes the engine type
  private void initGameEngine()
  {
    // determining game engine
    Game game = getGame();
    Engine engine;
    switch (game) {
      case BG1:
      case BG1TotSC:
        engine = Engine.BG1;
        break;
      case BG2SoA:
      case BG2ToB:
      case Tutu:
      case BGT:
        engine = Engine.BG2;
        break;
      case PST:
        engine = Engine.PST;
        break;
      case IWD:
      case IWDHoW:
      case IWDHowToTLM:
        engine = Engine.IWD;
        break;
      case IWD2:
        engine = Engine.IWD2;
        break;
      case BG1EE:
      case BG2EE:
      case IWDEE:
      case EET:
        engine = Engine.EE;
        break;
      default:
        engine = Engine.Unknown;
    }
    addEntry(GET_GAME_ENGINE, Type.Object, engine);
  }

  // Initializes supported override folders used by specific games
  private void initOverrides()
  {
    List<File> list = new ArrayList<File>();
    if (isEnhancedEdition()) {
      File gameRoot = (File)getProperty(GET_GAME_ROOT_FOLDER);
      File homeRoot = (File)getProperty(GET_GAME_HOME_FOLDER);
      File langRoot = (File)getProperty(GET_GAME_LANG_FOLDER);
      list.add(new FileNI(langRoot, "Movies"));
      list.add(new FileNI(langRoot, "Sounds"));
      list.add(new FileNI(homeRoot, "Movies"));
      list.add(new FileNI(homeRoot, "Characters"));
      list.add(new FileNI(homeRoot, "Portraits"));
      list.add(new FileNI(homeRoot, "Sounds"));
      list.add(new FileNI(homeRoot, "Scripts"));
      list.add(new FileNI(homeRoot, "Override"));
      list.add(new FileNI(gameRoot, "Movies"));
      list.add(new FileNI(gameRoot, "Characters"));
      list.add(new FileNI(gameRoot, "Portraits"));
      list.add(new FileNI(gameRoot, "Sounds"));
      list.add(new FileNI(gameRoot, "Scripts"));
      list.add(new FileNI(gameRoot, "Override"));
    } else {
      File gameRoot = (File)getProperty(GET_GAME_ROOT_FOLDER);
      list.add(new FileNI(gameRoot, "Characters"));
      list.add(new FileNI(gameRoot, "Portraits"));
      list.add(new FileNI(gameRoot, "Sounds"));
      list.add(new FileNI(gameRoot, "Scripts"));
      list.add(new FileNI(gameRoot, "Override"));
    }
    addEntry(GET_GAME_OVERRIDE_FOLDERS, Type.List, list);
  }

  // Initializes supported resource types
  private void initResourceTypes()
  {
    Game game = getGame();
    Engine engine = getEngine();

    addEntry(IS_SUPPORTED_2DA, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_ACM, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_ARE_V10, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.IWD || engine == Engine.PST ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_ARE_V91, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_BAM_V1, Type.Boolean, Boolean.valueOf(true));
    addEntry(IS_SUPPORTED_BAMC_V1, Type.Boolean, (engine == Engine.BG2 || engine == Engine.IWD ||
                                                  engine == Engine.IWD2 || engine == Engine.EE));
    addEntry(IS_SUPPORTED_BAM_V2, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_BCS, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_BIFF, Type.Boolean, Boolean.valueOf(true));
    addEntry(IS_SUPPORTED_BIF, Type.Boolean, (engine == Engine.IWD));
    addEntry(IS_SUPPORTED_BIFC, Type.Boolean, (engine == Engine.BG2 || engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_BIK, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_BIO, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                              engine == Engine.EE || engine == Engine.Unknown));

    addEntry(IS_SUPPORTED_BMP_PAL, Type.Boolean, Boolean.valueOf(true));
    addEntry(IS_SUPPORTED_BMP_ALPHA, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_CHR_V10, Type.Boolean, (engine == Engine.BG1 || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_CHR_V12, Type.Boolean, (engine == Engine.PST));
    addEntry(IS_SUPPORTED_CHR_V20, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));
    addEntry(IS_SUPPORTED_CHR_V21, Type.Boolean, (game == Game.BG2ToB || engine == Engine.EE));
    addEntry(IS_SUPPORTED_CHR_V22, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_CHU, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_CRE_V10, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_CRE_V12, Type.Boolean, (engine == Engine.PST));
    addEntry(IS_SUPPORTED_CRE_V22, Type.Boolean, (engine == Engine.IWD2));
    addEntry(IS_SUPPORTED_CRE_V90, Type.Boolean, (engine == Engine.IWD));

    addEntry(IS_SUPPORTED_DLG, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_EFF, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                              engine == Engine.IWD || engine == Engine.EE ||
                                              engine == Engine.Unknown));

    addEntry(IS_SUPPORTED_FNT, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_GAM_V11, Type.Boolean, (engine == Engine.BG1 || engine == Engine.IWD ||
                                                  engine == Engine.PST || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_GAM_V20, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));
    addEntry(IS_SUPPORTED_GAM_V21, Type.Boolean, (game == Game.BG2ToB || engine == Engine.EE));
    addEntry(IS_SUPPORTED_GAM_V22, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_GLSL, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_GUI, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_IDS, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_INI, Type.Boolean, (engine == Engine.IWD || engine == Engine.IWD2 ||
                                              engine == Engine.PST || game == Game.IWDEE));

    addEntry(IS_SUPPORTED_ITM_V10, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.IWD || engine == Engine.EE ||
                                                  engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_ITM_V11, Type.Boolean, (engine == Engine.PST));
    addEntry(IS_SUPPORTED_ITM_V20, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_KEY, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_MOS_V1, Type.Boolean, Boolean.valueOf(true));
    addEntry(IS_SUPPORTED_MOSC_V1, Type.Boolean, (engine == Engine.BG2 || engine == Engine.IWD ||
                                                  engine == Engine.IWD2 || engine == Engine.EE));
    addEntry(IS_SUPPORTED_MOS_V2, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_MUS, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_MVE, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_OGG, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_PLT, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_PVRZ, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_PRO, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(IS_SUPPORTED_RES, Type.Boolean, (engine == Engine.IWD || engine == Engine.IWD2 ||
                                              game == Game.IWDEE));

    addEntry(IS_SUPPORTED_SAV, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_SPL_V1, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                 engine == Engine.IWD || engine == Engine.PST ||
                                                 engine == Engine.EE || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_SPL_V2, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_SQL, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_SRC_PST, Type.Boolean, (engine == Engine.PST));

    addEntry(IS_SUPPORTED_SRC_IWD2, Type.Boolean, (engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_STO_V10, Type.Boolean, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(IS_SUPPORTED_STO_V11, Type.Boolean, (engine == Engine.PST));
    addEntry(IS_SUPPORTED_STO_V90, Type.Boolean, (engine == Engine.IWD || engine == Engine.IWD2));

    addEntry(IS_SUPPORTED_TIS_V1, Type.Boolean, Boolean.valueOf(true));
    addEntry(IS_SUPPORTED_TIS_V2, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_TLK, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_TO_V1, Type.Boolean, (engine == Engine.BG2 || engine == Engine.IWD));
    addEntry(IS_SUPPORTED_TO_V2, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_VAR, Type.Boolean, (engine == Engine.PST));

    addEntry(IS_SUPPORTED_VEF, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(IS_SUPPORTED_VVC, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(IS_SUPPORTED_WAV, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_WAVC, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_WBM, Type.Boolean, isEnhancedEdition());

    addEntry(IS_SUPPORTED_WED, Type.Boolean, Boolean.valueOf(true));

    addEntry(IS_SUPPORTED_WFX, Type.Boolean, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(IS_SUPPORTED_WMP, Type.Boolean, Boolean.valueOf(true));
  }

  // Initializes game-specific features
  private void initFeatures()
  {
    Game game = getGame();
    Engine engine = getEngine();

    // Are Kits supported?
    addEntry(IS_SUPPORTED_KITS, Type.Boolean, (engine == Engine.BG2 || engine == Engine.IWD2 ||
                                               engine == Engine.EE));

    // the actual name of the "Alignment" IDS resource
    addEntry(GET_IDS_ALIGNMENT, Type.String, (engine == Engine.IWD2) ? "ALIGNMNT.IDS" : "ALIGNMEN.IDS");

    // display mode of overlays in tilesets
    addEntry(IS_TILESET_STENCILED, Type.Boolean, (engine == Engine.BG2 || game == Game.BG2EE));
  }

//-------------------------- INNER CLASSES --------------------------

  // Internal definition of a property entry
  private static class Property
  {
    private final Integer key;
    private final Type type;

    private Object data;

    /**
     * Initialize a new property.
     * @param key A unique identifier for the property. Cannot be modified afterwards.
     * @param type The data type of the property. Cannot be modified afterwards.
     * @param data The actual data of the property. Can be modified afterwards.
     */
    public Property(int key, Type type, Object data)
    {
      this.key = Integer.valueOf(key);
      this.type = type;
      this.data = data;
    }

    /** Returns a unique key which identifies this property. */
    public Integer getKey() { return key; }

    /** Returns the data type of this property. */
    public Type getType() { return type; }

    /** Returns the actual data of this property. */
    public Object getData() { return data; }

    /** Sets new data value of this property. Returns the previous data. */
    public Object setData(Object newValue) { Object retVal = data; data = newValue; return retVal; }

    @Override
    public String toString()
    {
      return String.format("%1$d:[%2$s] = %3$s", key, type, data);
    }
  }
}
