// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import org.infinity.NearInfinity;
import org.infinity.util.io.FileNI;

/**
 * Provides engine- and game-specific properties of the currently opened Infinity Engine game.<br>
 * <br>
 * Properties can be accessed by unique identifiers. The returned property can be
 * of any type defined by the enum {@link Profile.Type} or {@code null}.
 */
public final class Profile
{
  /** Supported data types for properties. */
  public enum Type {
    /** Property data is of type {@link Boolean}. */
    BOOLEAN,
    /** Property data is of type {@link Integer}. */
    INTEGER,
    /** Property data is of type {@link String}. */
    STRING,
    /** Property data is of type {@link java.util.List}. */
    LIST,
    /** Property data is of type {@link java.io.File}. */
    FILE,
    /** Property data is of any custom data type. */
    OBJECT,
  }

  /** Supported Infinity Engine games. */
  public enum Game {
    /** Default for unrecognized or unsupported engine. */
    Unknown,
    /** Baldur's Gate */
    BG1,
    /** Baldur's Gate: Tales of the Sword Coast */
    BG1TotSC,
    /** Baldur's Gate II: Shadows of Amn */
    BG2SoA,
    /** Baldur's Gate II: Throne of Bhaal */
    BG2ToB,
    /** BGTutu and EasyTutu */
    Tutu,
    /** Baldur's Gate Trilogy */
    BGT,
    /** Planescape: Torment */
    PST,
    /** Icewind Dale */
    IWD,
    /** Icewind Dale: Heart of Winter */
    IWDHoW,
    /** Icewind Dale: Trials of the Luremaster */
    IWDHowToTLM,
    /** Icewind Dale II */
    IWD2,
    /** Baldur's Gate: Enhanced Edition */
    BG1EE,
    /** Baldur's Gate: Siege of Dragonspear */
    BG1SoD,
    /** Baldur's Gate II: Enhanced Edition */
    BG2EE,
    /** Icewind Dale: Enhanced Edition */
    IWDEE,
    /** Enhanced Edition Trilogy */
    EET,
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
    /** Includes IWD, IWDHoW and IWDTotLM. */
    IWD,
    /** Includes IWD2. */
    IWD2,
    /** Includes BG1EE, BG1SoD, BG2EE, IWDEE and EET. */
    EE,
  }

  /**
   * Keys for querying global, game- or engine-specific properties.
   */
  public enum Key {
    // Static properties
    /** Property: (String) List of supported games. */
    GET_GLOBAL_NEARINFINITY_VERSION,
    /** Property: (List&lt;Game&gt;) List of supported games. */
    GET_GLOBAL_GAMES,
    /** Property: (String) The name of the override folder ("{@code Override}"). */
    GET_GLOBAL_OVERRIDE_NAME,
    /** Property: (String) Returns "{@code dialog.tlk}". */
    GET_GLOBAL_DIALOG_NAME,
    /** Property: (String) Returns "{@code dialogf.tlk}". */
    GET_GLOBAL_DIALOG_NAME_FEMALE,

    // Static properties which require an additional parameter.
    /** Property: (String) Returns the game's title. Extra parameter: Desired {@link Game}. */
    GET_GLOBAL_GAME_TITLE,
    /** Property: (List&lt;String&gt;) Returns a list of extra folders for the specified game.
     *            Extra parameter: Desired {@link Game}. */
    GET_GLOBAL_EXTRA_FOLDERS,
    /** Property: (String) Returns the game's home folder name.
     *            Extra parameter: Desired <em>Enhanced Edition</em> {@link Game}. */
    GET_GLOBAL_HOME_FOLDER,

    // Properties set at runtime
    /** Property: ({@link Game}) Game identifier. */
    GET_GAME_TYPE,
    /** Property: ({@link Engine}) Engine identifier. */
    GET_GAME_ENGINE,
    /** Property: (String) Name of the game's root folder. */
    GET_GAME_ROOT_FOLDER_NAME,
    /** Property: (String) Name of the game's home folder. (Enhanced Editions only) */
    GET_GAME_HOME_FOLDER_NAME,
    /** Property: (String) Name of the currently selected game language folder. (Enhanced Editions only) */
    GET_GAME_LANG_FOLDER_NAME,
    /** Property: (List&lt;String&gt;) List of available languages as language code
     *            for the current game. (Enhanced Editions only) */
    GET_GAME_LANG_FOLDER_NAMES_AVAILABLE,
    /** Property: (List&lt;File&gt;) List of valid root folder, sorted by priority in descending order. */
    GET_GAME_FOLDERS,
    /** Property: (File) Game's root folder. */
    GET_GAME_ROOT_FOLDER,
    /** Property: (File) Game's home folder. (Enhanced Editions only) */
    GET_GAME_HOME_FOLDER,
    /** Property: (File) Game's language folder. (Enhanced Editions only) */
    GET_GAME_LANG_FOLDER,
    /** Property: (List<&lt;File&gt;) List of available game language folders. (Enhanced Editions only) */
    GET_GAME_LANG_FOLDERS_AVAILABLE,
    /** Property: (File) Game's language root folder (where the actual language subfolder reside).
     *            (Enhanced Editions only) */
    GET_GAME_LANG_FOLDER_BASE,
    /** Property: (List&lt;File&gt;) List of override folders to search for game resources,
     *            sorted by priority in ascending order. */
    GET_GAME_OVERRIDE_FOLDERS,
    /** Property: (List&lt;String&gt;) List of extra folders containing game-related resources,
     *            sorted alphabetically in ascending order. */
    GET_GAME_EXTRA_FOLDERS,
    /** Property: (File) The game's chitin.key. */
    GET_GAME_CHITIN_KEY,
    /** Property: (String) Title of the game. */
    GET_GAME_TITLE,
    /** Property: (String) A short user-defined description or name of the game.
     *            Can be used to tell specific game installations apart. */
    GET_GAME_DESC,
    /** Property: (String) Name of the game's ini file. */
    GET_GAME_INI_NAME,
    /** Property: (File) Path of the game's ini file. */
    GET_GAME_INI_FILE,
    /** Property: (File) Path to the currently selected {@code dialog.tlk}. */
    GET_GAME_DIALOG_FILE,
    /** Property: (File) Path to the currently selected female {@code dialogf.tlk}.
     *            Returns {@code null} if the language does not require a dialogf.tlk. */
    GET_GAME_DIALOGF_FILE,
    /** Property: (List&lt;File&gt;) Unsorted list of extra folders containing BIFF archives.
     *            (Non-Enhanced Editions only) */
    GET_GAME_BIFF_FOLDERS,
    /** Property: (Boolean) Is game an Enhanced Edition game? */
    IS_ENHANCED_EDITION,
    /** Property: (Boolean) Has current game been enhanced by TobEx? */
    IS_GAME_TOBEX,

    /** Property: (Boolean) Are {@code 2DA} resources supported? */
    IS_SUPPORTED_2DA,
    /** Property: (Boolean) Are {@code ACM} resources supported? */
    IS_SUPPORTED_ACM,
    /** Property: (Boolean) Are {@code ARE V1.0} resources supported? */
    IS_SUPPORTED_ARE_V10,
    /** Property: (Boolean) Are {@code ARE V9.1} resources supported? */
    IS_SUPPORTED_ARE_V91,
    /** Property: (Boolean) Are {@code BAM V1} resources supported? */
    IS_SUPPORTED_BAM_V1,
    /** Property: (Boolean) Are {@code BAM V1} resources supported? */
    IS_SUPPORTED_BAMC_V1,
    /** Property: (Boolean) Are {@code BAM V2} resources supported? */
    IS_SUPPORTED_BAM_V2,
    /** Property: (Boolean) Are {@code BCS} resources supported? */
    IS_SUPPORTED_BCS,
    /** Property: (Boolean) Are uncompressed {@code BIFF V1} resources supported? */
    IS_SUPPORTED_BIFF,
    /** Property: (Boolean) Are compressed {@code BIF V1.0} resources supported? */
    IS_SUPPORTED_BIF,
    /** Property: (Boolean) Are compressed {@code BIFC V1.0} resources supported? */
    IS_SUPPORTED_BIFC,
    /** Property: (Boolean) Are {@code BIK} resources supported? */
    IS_SUPPORTED_BIK,
    /** Property: (Boolean) Are {@code BIO} resources supported? */
    IS_SUPPORTED_BIO,
    /** Property: (Boolean) Are paletted {@code BMP} resources supported? */
    IS_SUPPORTED_BMP_PAL,
    /** Property: (Boolean) Are alpha-blended {@code BMP} resources supported? */
    IS_SUPPORTED_BMP_ALPHA,
    /** Property: (Boolean) Are {@code CHR V1.0} resources supported? */
    IS_SUPPORTED_CHR_V10,
    /** Property: (Boolean) Are {@code CHR V2.0} resources supported? */
    IS_SUPPORTED_CHR_V20,
    /** Property: (Boolean) Are {@code CHR V2.1} resources supported? */
    IS_SUPPORTED_CHR_V21,
    /** Property: (Boolean) Are {@code CHR V2.2} resources supported? */
    IS_SUPPORTED_CHR_V22,
    /** Property: (Boolean) Are {@code CHU} resources supported? */
    IS_SUPPORTED_CHU,
    /** Property: (Boolean) Are {@code CRE V1.0} resources supported? */
    IS_SUPPORTED_CRE_V10,
    /** Property: (Boolean) Are {@code CRE V1.2} resources supported? */
    IS_SUPPORTED_CRE_V12,
    /** Property: (Boolean) Are {@code CRE V2.2} resources supported? */
    IS_SUPPORTED_CRE_V22,
    /** Property: (Boolean) Are {@code CRE V9.0} resources supported? */
    IS_SUPPORTED_CRE_V90,
    /** Property: (Boolean) Are {@code DLG} resources supported? */
    IS_SUPPORTED_DLG,
    /** Property: (Boolean) Are {@code EFF} resources supported? */
    IS_SUPPORTED_EFF,
    /** Property: (Boolean) Are {@code FNT} resources supported? */
    IS_SUPPORTED_FNT,
    /** Property: (Boolean) Are {@code GAM V1.1} resources supported? */
    IS_SUPPORTED_GAM_V11,
    /** Property: (Boolean) Are {@code GAM V2.0} resources supported? */
    IS_SUPPORTED_GAM_V20,
    /** Property: (Boolean) Are {@code GAM V2.1} resources supported? */
    IS_SUPPORTED_GAM_V21,
    /** Property: (Boolean) Are {@code GAM V2.2} resources supported? */
    IS_SUPPORTED_GAM_V22,
    /** Property: (Boolean) Are {@code GLSL} resources supported? */
    IS_SUPPORTED_GLSL,
    /** Property: (Boolean) Are {@code GUI} resources supported? */
    IS_SUPPORTED_GUI,
    /** Property: (Boolean) Are {@code IDS} resources supported? */
    IS_SUPPORTED_IDS,
    /** Property: (Boolean) Are {@code INI} resources supported? */
    IS_SUPPORTED_INI,
    /** Property: (Boolean) Are {@code ITM V1.0} resources supported? */
    IS_SUPPORTED_ITM_V10,
    /** Property: (Boolean) Are {@code ITM V1.1} resources supported? */
    IS_SUPPORTED_ITM_V11,
    /** Property: (Boolean) Are {@code ITM V2.0} resources supported? */
    IS_SUPPORTED_ITM_V20,
    /** Property: (Boolean) Are {@code KEY} resources supported? */
    IS_SUPPORTED_KEY,
    /** Property: (Boolean) Are {@code LUA} resources supported? */
    IS_SUPPORTED_LUA,
    /** Property: (Boolean) Are {@code MENU} resources supported? */
    IS_SUPPORTED_MENU,
    /** Property: (Boolean) Are {@code MOS V1} resources supported? */
    IS_SUPPORTED_MOS_V1,
    /** Property: (Boolean) Are {@code MOSC V1} resources supported? */
    IS_SUPPORTED_MOSC_V1,
    /** Property: (Boolean) Are {@code MOS V2} resources supported? */
    IS_SUPPORTED_MOS_V2,
    /** Property: (Boolean) Are {@code MUS} resources supported? */
    IS_SUPPORTED_MUS,
    /** Property: (Boolean) Are {@code MVE} resources supported? */
    IS_SUPPORTED_MVE,
    /** Property: (Boolean) Are {@code OGG} resources supported? */
    IS_SUPPORTED_OGG,
    /** Property: (Boolean) Are {@code PLT} resources supported? */
    IS_SUPPORTED_PLT,
    /** Property: (Boolean) Are {@code PNG} resources supported? */
    IS_SUPPORTED_PNG,
    /** Property: (Boolean) Are {@code PRO} resources supported? */
    IS_SUPPORTED_PRO,
    /** Property: (Boolean) Are {@code PVRZ} resources supported? */
    IS_SUPPORTED_PVRZ,
    /** Property: (Boolean) Are {@code RES} resources supported? */
    IS_SUPPORTED_RES,
    /** Property: (Boolean) Are {@code SAV} resources supported? */
    IS_SUPPORTED_SAV,
    /** Property: (Boolean) Are {@code SPL V1} resources supported? */
    IS_SUPPORTED_SPL_V1,
    /** Property: (Boolean) Are {@code SPL V2} resources supported? */
    IS_SUPPORTED_SPL_V2,
    /** Property: (Boolean) Are {@code SQL} resources supported? */
    IS_SUPPORTED_SQL,
    /** Property: (Boolean) Are (PST) {@code SRC} resources supported? */
    IS_SUPPORTED_SRC_PST,
    /** Property: (Boolean) Are (IWD2) {@code SRC} resources supported? */
    IS_SUPPORTED_SRC_IWD2,
    /** Property: (Boolean) Are {@code STO V1.0} resources supported? */
    IS_SUPPORTED_STO_V10,
    /** Property: (Boolean) Are {@code STO V1.1} resources supported? */
    IS_SUPPORTED_STO_V11,
    /** Property: (Boolean) Are {@code STO V9.0} resources supported? */
    IS_SUPPORTED_STO_V90,
    /** Property: (Boolean) Are (palette-based) {@code TIS V1} resources supported? */
    IS_SUPPORTED_TIS_V1,
    /** Property: (Boolean) Are (PVRZ-based) {@code TIS V2} resources supported? */
    IS_SUPPORTED_TIS_V2,
    /** Property: (Boolean) Are {@code TLK} resources supported? */
    IS_SUPPORTED_TLK,
    /** Property: (Boolean) Are {@code TO V1} (TOH/TOT) resources supported? */
    IS_SUPPORTED_TO_V1,
    /** Property: (Boolean) Are {@code TO V2} (TOH only) resources supported? */
    IS_SUPPORTED_TO_V2,
    /** Property: (Boolean) Are {@code TTF} resources supported? */
    IS_SUPPORTED_TTF,
    /** Property: (Boolean) Are {@code VAR} resources supported? */
    IS_SUPPORTED_VAR,
    /** Property: (Boolean) Are {@code VEF} resources supported? */
    IS_SUPPORTED_VEF,
    /** Property: (Boolean) Are {@code VVC} resources supported? */
    IS_SUPPORTED_VVC,
    /** Property: (Boolean) Are {@code WAV} resources supported? */
    IS_SUPPORTED_WAV,
    /** Property: (Boolean) Are {@code WAVC} resources supported? */
    IS_SUPPORTED_WAVC,
    /** Property: (Boolean) Are {@code WBM} resources supported? */
    IS_SUPPORTED_WBM,
    /** Property: (Boolean) Are {@code WED} resources supported? */
    IS_SUPPORTED_WED,
    /** Property: (Boolean) Are {@code WFX} resources supported? */
    IS_SUPPORTED_WFX,
    /** Property: (Boolean) Are {@code WMP} resources supported? */
    IS_SUPPORTED_WMP,

    /** Property: (Boolean) Are Kits supported? */
    IS_SUPPORTED_KITS,
    /** Property: (String) The name of the ALIGNMENT IDS resource. */
    GET_IDS_ALIGNMENT,
    /** Property: (Boolean) Indices whether overlays in tilesets are stenciled. */
    IS_TILESET_STENCILED,
  }

  // Container for Property entries
  private static final EnumMap<Key, Profile.Property> properties = new EnumMap<>(Key.class);
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
    GAME_TITLE.put(Game.BG1SoD, "Baldur's Gate: Siege of Dragonspear");
    GAME_TITLE.put(Game.BG2EE, "Baldur's Gate II: Enhanced Edition");
    GAME_TITLE.put(Game.IWDEE, "Icewind Dale: Enhanced Edition");
    GAME_TITLE.put(Game.EET, "Baldur's Gate - Enhanced Edition Trilogy");

    // initializing extra folders for each supported game
    final String[] PST_EXTRA_FOLDERS = { "Music", "Save", "Temp" };
    final String[] BG_EXTRA_FOLDERS = { "Characters", "MPSave", "Music", "Portraits", "Save", "Screenshots",
                                        "Scripts", "ScrnShot", "Sounds", "Temp", "TempSave" };
    final String[] EE_EXTRA_FOLDERS = { "BPSave", "Characters", "Fonts", "Movies", "MPSave", "MPBPSave",
                                        "MPSODSave", "Music", "Portraits", "Save", "Sounds", "ScrnShot",
                                        "Scripts", "SODSave", "Temp", "TempSave" };
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
    GAME_EXTRA_FOLDERS.put(Game.BG1EE, Arrays.asList(EE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG1SoD, Arrays.asList(EE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.BG2EE, Arrays.asList(EE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.IWDEE, Arrays.asList(EE_EXTRA_FOLDERS));
    GAME_EXTRA_FOLDERS.put(Game.EET, Arrays.asList(EE_EXTRA_FOLDERS));

    // initializing home folder names for Enhanced Edition games
    GAME_HOME_FOLDER.put(Game.BG1EE, "Baldur's Gate - Enhanced Edition");
    GAME_HOME_FOLDER.put(Game.BG1SoD, GAME_HOME_FOLDER.get(Game.BG1EE));
    GAME_HOME_FOLDER.put(Game.BG2EE, "Baldur's Gate II - Enhanced Edition");
    GAME_HOME_FOLDER.put(Game.EET, GAME_HOME_FOLDER.get(Game.BG2EE));
    GAME_HOME_FOLDER.put(Game.IWDEE, "Icewind Dale - Enhanced Edition");

    // static properties are always available
    initStaticProperties();
  }

  /**
   * Converts the string representation of a game type into a {@link Game} enum type.
   * Falls back to {@code Game.Unknown} on error.
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
   * @return {@code true} if the game has been initialized successfully, {@code false} otherwise.
   */
  public static boolean openGame(File keyFile)
  {
    return openGame(keyFile, null);
  }

  /**
   * Initializes properties of a new game.
   * @param keyFile Full path to the chitin.key of the opened game.
   * @param desc An optional description associated with the game.
   * @return {@code true} if the game has been initialized successfully, {@code false} otherwise.
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
   * @return {@code true} if a game has been initialized, {@code false} if not.
   */
  public static boolean isGameOpen()
  {
    return (instance != null);
  }

  /**
   * Returns whether a property of the specified key is available.
   * @param key A key to identify the property.
   * @return {@code true} if the key is pointing to an existing property, {@code false} otherwise.
   */
  public static boolean hasProperty(Key key)
  {
    return properties.containsKey(key);
  }

  /**
   * Returns the data type of the specified property.
   * @param key The unique key identifying the property.
   * @return The data type of the property, or {@code null} if not available.
   */
  public static Type getPropertyType(Key key)
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
   * @return The data of the property, or {@code null} if not available.
   */
  public static Object getProperty(Key key)
  {
    return getProperty(key, null);
  }

  /**
   * Returns the actual data of the specified property.
   * @param key The unique key identifying the property.
   * @param param An additional parameter required by a small number of properties.
   *              Specify {@code null} to always return the parent structure of the property.
   * @return The data of the property, or {@code null} if not available.
   */
  public static Object getProperty(Key key, Object param)
  {
    Property prop = getEntry(key);
    if (prop != null) {
      if (param == null) {
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
          default:
        }
      }
    }
    return null;
  }

  /**
   * Updates the value of the Property entry specified by {@code key} and returns
   * the previous Property value.
   * @param key The Property key.
   * @param data The new value of the Property instance.
   * @return the previous value of the specified Property, or {@code null} if not available or applicable.
   */
  public static Object updateProperty(Key key, Object data)
  {
    // Properties requiring extra parameters cannot be updated
    if (key != null) {
      Property prop = properties.get(key);
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
   * @return {@code true} if Property has been added or updated, {@code false} otherwise.
   */
  public static boolean addProperty(Key key, Type type, Object data)
  {
    if (key != null) {
      if (properties.containsKey(key)) {
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
    return (String)getProperty(Key.GET_GLOBAL_OVERRIDE_NAME);
  }

  /**
   * Returns the game type of the current game.
   * @return The {@link Game} type of the current game or {@code Game.Unknown} otherwise.
   */
  public static Game getGame()
  {
    Object ret = getProperty(Key.GET_GAME_TYPE);
    return (ret instanceof Game) ? (Game)ret : Game.Unknown;
  }

  /**
   * Returns the engine type of the current game.
   * @return The {@link Engine} type of the current game or {@code Engine.Unknown} otherwise.
   */
  public static Engine getEngine()
  {
    Object ret = getProperty(Key.GET_GAME_ENGINE);
    return (ret instanceof Engine) ? (Engine)ret : Engine.Unknown;
  }

  /**
   * Returns whether the current game is an Enhanced Edition game.
   * @return {@code true} if the current game is an Enhanced Edition Game, {@code false} otherwise.
   */
  public static boolean isEnhancedEdition()
  {
    Object ret = getProperty(Key.IS_ENHANCED_EDITION);
    return (ret instanceof Boolean) ? (Boolean)ret : false;
  }

  /**
   * Returns the game's root folder.
   * @return The game's root folder as File object.
   */
  public static File getGameRoot()
  {
    return (File)getProperty(Key.GET_GAME_ROOT_FOLDER);
  }

  /**
   * Returns the game's home folder. A Non-enhanced Edition game will always return the game's
   * root folder instead.
   * @return The game's home folder as File object.
   */
  public static File getHomeRoot()
  {
    Object ret = getProperty(Key.GET_GAME_HOME_FOLDER);
    return (ret instanceof File) ? (File)ret : getGameRoot();
  }

  /**
   * Returns the game's language folder (where the dialog.tlk is located).
   * A Non-enhanced Edition game will always return the game's root folder instead.
   * @return The game's language folder as File object.
   */
  public static File getLanguageRoot()
  {
    Object ret = getProperty(Key.GET_GAME_LANG_FOLDER);
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
    List<?> ret = (List<?>)getProperty(Key.GET_GAME_FOLDERS);
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
    Object ret = getProperty(Key.GET_GAME_CHITIN_KEY);
    return (ret instanceof File) ? (File)ret : null;
  }

  /**
   * Updates language-related Properties with the specified game language. (Enhanced Editions only)
   * @param language The name of the language subfolder.
   * @return {@code true} if the game language has been updated successfully, {@code false} otherwise.
   */
  public static boolean updateGameLanguage(String language)
  {
    if (isEnhancedEdition() && language != null) {
      List<?> languages = (List<?>)getProperty(Key.GET_GAME_LANG_FOLDER_NAMES_AVAILABLE);
      for (Iterator<?> iter = languages.iterator(); iter.hasNext();) {
        String curLang = (String)iter.next();
        if (curLang.equalsIgnoreCase(language)) {
          instance.initRootDirs();
          instance.initOverrides();
          // updating language names and folders
          updateProperty(Key.GET_GAME_LANG_FOLDER_NAME, curLang);
          File langPath = new FileNI((File)getProperty(Key.GET_GAME_LANG_FOLDER_BASE), curLang);
          updateProperty(Key.GET_GAME_LANG_FOLDER, langPath);
          // updating dialog.tlks
          updateProperty(Key.GET_GAME_DIALOG_FILE, new FileNI(langPath, (String)getProperty(Key.GET_GLOBAL_DIALOG_NAME)));
          File femaleTlkFile = new FileNI(langPath, (String)getProperty(Key.GET_GLOBAL_DIALOG_NAME_FEMALE));
          if (femaleTlkFile.isFile()) {
            addProperty(Key.GET_GAME_DIALOGF_FILE, Type.FILE, femaleTlkFile);
          } else {
            updateProperty(Key.GET_GAME_DIALOGF_FILE, null);
          }
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a list of supported resource types by the current game.
   * @return String array containing format extensions.
   */
  public static String[] getAvailableResourceTypes()
  {
    return getAvailableResourceTypes(false);
  }

  /**
   * Returns a list of known or supported resource types by the current game.
   * @param ignoreGame If {@code true}, returns all known resource types.
   *                   If {@code false}, returns resource types supported by the current game.
   * @return String array containing format extensions.
   */
  public static String[] getAvailableResourceTypes(boolean ignoreGame)
  {
    ArrayList<String> list = new ArrayList<String>();
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_2DA))     { list.add("2DA"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ACM))     { list.add("ACM"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ARE_V10) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ARE_V91)) { list.add("ARE"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BAM_V1) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BAM_V2) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BAMC_V1)) { list.add("BAM"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BCS))     { list.add("BCS"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BIK))     { list.add("BIK"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BIO))     { list.add("BIO"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BMP_ALPHA) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_BMP_PAL)) { list.add("BMP"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CHR_V10) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CHR_V20) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CHR_V21) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CHR_V22)) { list.add("CHR"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CHU))     { list.add("CHU"); }
    if ((Boolean)getProperty(Key.IS_SUPPORTED_CRE_V10) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CRE_V12) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CRE_V22) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_CRE_V90)) { list.add("CRE"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_DLG))     { list.add("DLG"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_EFF))     { list.add("EFF"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_FNT))     { list.add("FNT"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GAM_V11) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GAM_V20) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GAM_V21) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GAM_V22)) { list.add("GAM"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GLSL))    { list.add("GLSL"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_GUI))     { list.add("GUI"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_IDS))     { list.add("IDS"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_INI))     { list.add("INI"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ITM_V10) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ITM_V11) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_ITM_V20)) { list.add("ITM"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_LUA))     { list.add("LUA"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MENU))     { list.add("MENU"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MOS_V1) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MOS_V2) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MOSC_V1)) { list.add("MOS"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MUS))     { list.add("MUS"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_MVE))     { list.add("MVE"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_PLT))     { list.add("PLT"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_PNG))     { list.add("PNG"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_PRO))     { list.add("PRO"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_PVRZ))    { list.add("PVRZ"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_RES))     { list.add("RES"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SAV))     { list.add("SAV"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SPL_V1) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SPL_V2))  { list.add("SPL"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SQL))     { list.add("SQL"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SRC_IWD2) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_SRC_PST)) { list.add("SRC"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_STO_V10) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_STO_V11) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_STO_V90)) { list.add("STO"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_TIS_V1) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_TIS_V2))  { list.add("TIS"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_TO_V1))   { list.add("TOH"); list.add("TOT"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_TO_V2))   { list.add("TOH"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_TTF))     { list.add("TTF"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_VAR))     { list.add("VAR"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_VEF))     { list.add("VEF"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_VVC))     { list.add("VVC"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WAV) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WAVC) ||
        (Boolean)getProperty(Key.IS_SUPPORTED_OGG))     { list.add("WAV"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WBM))     { list.add("WBM"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WED))     { list.add("WED"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WFX))     { list.add("WFX"); }
    if (ignoreGame ||
        (Boolean)getProperty(Key.IS_SUPPORTED_WMP))     { list.add("WMP"); }

    String[] retVal = new String[list.size()];
    for (int i = 0; i < retVal.length; i++) {
      retVal[i] = list.get(i);
    }

    return retVal;
  }

  // Returns the Property object assigned to the given key.
  private static Property getEntry(Key  key)
  {
    return properties.get(key);
  }

  // Adds a new Property entry to the list.
  private static void addEntry(Key key, Type type, Object data)
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
    addEntry(Key.GET_GLOBAL_NEARINFINITY_VERSION, Type.STRING, NearInfinity.getVersion());

    // setting list of supported games and associated data
    List<Game> gameList = new ArrayList<Game>();
    for (Game game : Game.values()) {
      gameList.add(game);
    }
    addEntry(Key.GET_GLOBAL_GAMES, Type.LIST, gameList);
    addEntry(Key.GET_GLOBAL_GAME_TITLE, Type.STRING, GAME_TITLE);
    addEntry(Key.GET_GLOBAL_EXTRA_FOLDERS, Type.LIST, GAME_EXTRA_FOLDERS);
    addEntry(Key.GET_GLOBAL_HOME_FOLDER, Type.STRING, GAME_HOME_FOLDER);

    // setting default override folder name
    addEntry(Key.GET_GLOBAL_OVERRIDE_NAME, Type.STRING, "Override");

    // setting dialog.tlk file names
    addEntry(Key.GET_GLOBAL_DIALOG_NAME, Type.STRING, "dialog.tlk");
    addEntry(Key.GET_GLOBAL_DIALOG_NAME_FEMALE, Type.STRING, "dialogf.tlk");
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
      addEntry(Key.GET_GAME_DESC, Type.STRING, desc);
    }

    // adding chitin.key path
    addEntry(Key.GET_GAME_CHITIN_KEY, Type.FILE, keyFile);

    // adding game's root folder and name
    File rootDir = keyFile.getAbsoluteFile().getParentFile();
    addEntry(Key.GET_GAME_ROOT_FOLDER, Type.FILE, rootDir);
    addEntry(Key.GET_GAME_ROOT_FOLDER_NAME, Type.STRING, rootDir.getName());

    initGame();
  }

  private void initGame() throws Exception
  {
    // Main game detection
    Game game;

    File gameRoot = (File)getProperty(Key.GET_GAME_ROOT_FOLDER);
    if (new FileNI(gameRoot, "movies/howseer.wbm").isFile()) {
      game = Game.IWDEE;
      // Note: baldur.ini is initialized later
    } else if (new FileNI(gameRoot, "movies/pocketzz.wbm").isFile()) {
      if (new FileNI(gameRoot, "override/EET.flag").isFile() ||
          new FileNI(gameRoot, "data/eetTU00.bif").isFile()) {
        game = Game.EET;
      } else {
        game = Game.BG2EE;
      }
      // Note: baldur.ini is initialized later
    } else if (new FileNI(gameRoot, "movies/sodcin01.wbm").isFile()) {
      game = Game.BG1SoD;
      // Note: baldur.ini is initialized later
    } else if (new FileNI(gameRoot, "movies/bgenter.wbm").isFile()) {
      game = Game.BG1EE;
      // Note: baldur.ini is initialized later
    } else if (new FileNI(gameRoot, "torment.exe").isFile() &&
               !(new FileNI(gameRoot, "movies/sigil.wbm").isFile())) {
      game = Game.PST;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "torment.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "idmain.exe").isFile() &&
               !(new FileNI(gameRoot, "movies/howseer.wbm").isFile())) {
      game = Game.IWD;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "icewind.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "iwd2.exe").isFile() &&
               new FileNI(gameRoot, "Data/Credits.mve").isFile()) {
      game = Game.IWD2;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "icewind2.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "baldur.exe").isFile() &&
               new FileNI(gameRoot, "BGConfig.exe").isFile()) {
      game = Game.BG2SoA;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "baldur.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "movies/graphsim.mov").isFile() || // Mac BG1 detection hack
               (new FileNI(gameRoot, "baldur.exe").isFile() && new FileNI(gameRoot, "Config.exe").isFile())) {
      game = Game.BG1;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "baldur.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else if (new FileNI(gameRoot, "bg1tutu.exe").isFile()) {
      game = Game.Tutu;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "baldur.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    } else {
      // game == Game.Unknown
      game = Game.Unknown;
      addEntry(Key.GET_GAME_INI_NAME, Type.STRING, "baldur.ini");
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(gameRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    }
    // adding priliminary game type into storage
    addEntry(Key.GET_GAME_TYPE, Type.OBJECT, game);
    addEntry(Key.GET_GAME_EXTRA_FOLDERS, Type.LIST, GAME_EXTRA_FOLDERS.get(game));

    // determining game engine
    initGameEngine();

    // initializing method isEnhancedEdition()
    addEntry(Key.IS_ENHANCED_EDITION, Type.BOOLEAN, Boolean.valueOf(getEngine() == Engine.EE));

    if (isEnhancedEdition()) {
      File langDir = new FileNI(gameRoot, "lang");
      if (langDir.isDirectory()) {
        addEntry(Key.GET_GAME_LANG_FOLDER_BASE, Type.FILE, langDir);
      }
    }

    if (GAME_HOME_FOLDER.containsKey(game)) {
      addEntry(Key.GET_GAME_HOME_FOLDER_NAME, Type.STRING, GAME_HOME_FOLDER.get(game));
    }

    // delayed initialization of ini files (EE only)
    if (isEnhancedEdition() && getProperty(Key.GET_GAME_INI_FILE) == null) {
      initIniFile("baldur.lua", "baldur.ini");
    }

    initRootDirs();

    // initializing dialog.tlk and dialogf.tlk
    File tlk = FileNI.getFile((List<?>)getProperty(Key.GET_GAME_FOLDERS),
                              (String)getProperty(Key.GET_GLOBAL_DIALOG_NAME));
    if (tlk != null && tlk.isFile()) {
      addEntry(Key.GET_GAME_DIALOG_FILE, Type.FILE, tlk);
    }
    tlk = FileNI.getFile((List<?>)getProperty(Key.GET_GAME_FOLDERS),
                         (String)getProperty(Key.GET_GLOBAL_DIALOG_NAME_FEMALE));
    if (tlk != null && tlk.isFile()) {
      addEntry(Key.GET_GAME_DIALOGF_FILE, Type.FILE, tlk);
    }

    // Initializing resource structure
    ResourceFactory.openGame((File)getProperty(Key.GET_GAME_CHITIN_KEY));

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
    }

    // updating game type
    addEntry(Key.GET_GAME_TYPE, Type.OBJECT, game);
    addEntry(Key.GET_GAME_TITLE, Type.STRING, GAME_TITLE.get(game));

    // initializing list of folders containing BIFF archives
    List<File> biffDirs = ResourceFactory.getBIFFDirs();
    if (biffDirs != null && !biffDirs.isEmpty()) {
      addEntry(Key.GET_GAME_BIFF_FOLDERS, Type.LIST, biffDirs);
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
      case BG1SoD:
      case BG2EE:
      case IWDEE:
      case EET:
        engine = Engine.EE;
        break;
      default:
        engine = Engine.Unknown;
    }
    addEntry(Key.GET_GAME_ENGINE, Type.OBJECT, engine);
  }

  // Initializes the first available of the specified ini files
  private void initIniFile(String... iniFiles)
  {
    if (iniFiles != null) {
      File homeRoot = ResourceFactory.getHomeRoot();
      for (int i = 0; i < iniFiles.length; i++) {
        if (new FileNI(homeRoot, iniFiles[i]).isFile()) {
          addEntry(Key.GET_GAME_INI_NAME, Type.STRING, iniFiles[i]);
          break;
        }
      }
    }
  }

  // Initializes available root folders of the game
  private void initRootDirs()
  {
    // Considering three (or four) different root folders to locate game resources
    // Note: Order of the root directories is important. FileNI will take the first one available.
    File homeRoot = ResourceFactory.getHomeRoot();
    String language = ResourceFactory.fetchGameLanguage(new FileNI(homeRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
    File langRoot = FileNI.getFile((File)getProperty(Key.GET_GAME_LANG_FOLDER_BASE), language);
    if (!langRoot.isDirectory()) {
      langRoot = null;
    }
    // fallback language added if selected language is non-english
    String languageDef = ResourceFactory.fetchGameLanguage(null);
    File langRootDef = FileNI.getFile((File)getProperty(Key.GET_GAME_LANG_FOLDER_BASE), languageDef);
    if (languageDef.equals(language)) {
      langRootDef = null;
    }

    List<File> listRoots = new ArrayList<File>();
    if (langRoot != null) {
      addEntry(Key.GET_GAME_LANG_FOLDER_NAME, Type.STRING, language);
      addEntry(Key.GET_GAME_LANG_FOLDER, Type.FILE, langRoot);
      List<File> langPaths = ResourceFactory.getAvailableGameLanguages();
      addEntry(Key.GET_GAME_LANG_FOLDERS_AVAILABLE, Type.LIST, langPaths);
      List<String> languages = new ArrayList<String>(langPaths.size());
      for (Iterator<File> iter = langPaths.iterator(); iter.hasNext();) {
        languages.add(iter.next().getName());
      }
      addEntry(Key.GET_GAME_LANG_FOLDER_NAMES_AVAILABLE, Type.LIST, languages);
      listRoots.add(langRoot);
    }
    if (langRootDef != null) {
      listRoots.add(langRootDef);
    }
    if (homeRoot != null) {
      addEntry(Key.GET_GAME_HOME_FOLDER, Type.FILE, homeRoot);
      addEntry(Key.GET_GAME_INI_FILE, Type.FILE, new FileNI(homeRoot, (String)getProperty(Key.GET_GAME_INI_NAME)));
      listRoots.add(homeRoot);
    }
    listRoots.add((File)getProperty(Key.GET_GAME_ROOT_FOLDER));
    addEntry(Key.GET_GAME_FOLDERS, Type.FILE, listRoots);
  }

  // Initializes supported override folders used by specific games
  private void initOverrides()
  {
    List<File> list = new ArrayList<File>();
    if (isEnhancedEdition()) {
      File gameRoot = (File)getProperty(Key.GET_GAME_ROOT_FOLDER);
      File homeRoot = (File)getProperty(Key.GET_GAME_HOME_FOLDER);
      File langRoot = (File)getProperty(Key.GET_GAME_LANG_FOLDER);
      list.add(new FileNI(homeRoot, "Movies"));
      list.add(new FileNI(homeRoot, "Characters"));
      list.add(new FileNI(homeRoot, "Portraits"));
      list.add(new FileNI(homeRoot, "Sounds"));
      list.add(new FileNI(homeRoot, "Scripts"));
      list.add(new FileNI(homeRoot, "Override"));
      list.add(new FileNI(langRoot, "Override"));
      list.add(new FileNI(langRoot, "Movies"));
      list.add(new FileNI(gameRoot, "Movies"));
      list.add(new FileNI(gameRoot, "Characters"));
      list.add(new FileNI(gameRoot, "Portraits"));
      list.add(new FileNI(langRoot, "Sounds"));
      list.add(new FileNI(gameRoot, "Sounds"));
      list.add(new FileNI(gameRoot, "Scripts"));
      list.add(new FileNI(gameRoot, "Override"));
    } else {
      File gameRoot = (File)getProperty(Key.GET_GAME_ROOT_FOLDER);
      list.add(new FileNI(gameRoot, "Characters"));
      list.add(new FileNI(gameRoot, "Portraits"));
      list.add(new FileNI(gameRoot, "Sounds"));
      list.add(new FileNI(gameRoot, "Scripts"));
      list.add(new FileNI(gameRoot, "Override"));
    }
    addEntry(Key.GET_GAME_OVERRIDE_FOLDERS, Type.LIST, list);
  }

  // Initializes supported resource types
  private void initResourceTypes()
  {
    Game game = getGame();
    Engine engine = getEngine();

    addEntry(Key.IS_SUPPORTED_2DA, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_ACM, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_ARE_V10, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.IWD || engine == Engine.PST ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_ARE_V91, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_BAM_V1, Type.BOOLEAN, Boolean.valueOf(true));
    addEntry(Key.IS_SUPPORTED_BAMC_V1, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.IWD ||
                                                  engine == Engine.IWD2 || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_BAM_V2, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_BCS, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_BIFF, Type.BOOLEAN, Boolean.valueOf(true));
    addEntry(Key.IS_SUPPORTED_BIF, Type.BOOLEAN, (engine == Engine.IWD));
    addEntry(Key.IS_SUPPORTED_BIFC, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_BIK, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_BIO, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                              engine == Engine.EE || engine == Engine.Unknown));

    addEntry(Key.IS_SUPPORTED_BMP_PAL, Type.BOOLEAN, Boolean.valueOf(true));
    addEntry(Key.IS_SUPPORTED_BMP_ALPHA, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_CHR_V10, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_CHR_V20, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_CHR_V21, Type.BOOLEAN, (game == Game.BG2ToB || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_CHR_V22, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_CHU, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_CRE_V10, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_CRE_V12, Type.BOOLEAN, (engine == Engine.PST));
    addEntry(Key.IS_SUPPORTED_CRE_V22, Type.BOOLEAN, (engine == Engine.IWD2));
    addEntry(Key.IS_SUPPORTED_CRE_V90, Type.BOOLEAN, (engine == Engine.IWD));

    addEntry(Key.IS_SUPPORTED_DLG, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_EFF, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                              engine == Engine.IWD || engine == Engine.EE ||
                                              engine == Engine.Unknown));

    addEntry(Key.IS_SUPPORTED_FNT, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_GAM_V11, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.IWD ||
                                                  engine == Engine.PST || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_GAM_V20, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_GAM_V21, Type.BOOLEAN, (game == Game.BG2ToB || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_GAM_V22, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_GLSL, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_GUI, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_IDS, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_INI, Type.BOOLEAN, (engine == Engine.IWD || engine == Engine.IWD2 ||
                                              engine == Engine.PST || game == Game.IWDEE));

    addEntry(Key.IS_SUPPORTED_ITM_V10, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.IWD || engine == Engine.EE ||
                                                  engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_ITM_V11, Type.BOOLEAN, (engine == Engine.PST));
    addEntry(Key.IS_SUPPORTED_ITM_V20, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_KEY, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_LUA, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_MENU, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_MOS_V1, Type.BOOLEAN, Boolean.valueOf(true));
    addEntry(Key.IS_SUPPORTED_MOSC_V1, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.IWD ||
                                                  engine == Engine.IWD2 || engine == Engine.EE));
    addEntry(Key.IS_SUPPORTED_MOS_V2, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_MUS, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_MVE, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_OGG, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_PLT, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_PNG, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_PRO, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(Key.IS_SUPPORTED_PVRZ, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_RES, Type.BOOLEAN, (engine == Engine.IWD || engine == Engine.IWD2 ||
                                              game == Game.IWDEE));

    addEntry(Key.IS_SUPPORTED_SAV, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_SPL_V1, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                 engine == Engine.IWD || engine == Engine.PST ||
                                                 engine == Engine.EE || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_SPL_V2, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_SQL, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_SRC_PST, Type.BOOLEAN, (engine == Engine.PST));

    addEntry(Key.IS_SUPPORTED_SRC_IWD2, Type.BOOLEAN, (engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_STO_V10, Type.BOOLEAN, (engine == Engine.BG1 || engine == Engine.BG2 ||
                                                  engine == Engine.EE || engine == Engine.Unknown));
    addEntry(Key.IS_SUPPORTED_STO_V11, Type.BOOLEAN, (engine == Engine.PST));
    addEntry(Key.IS_SUPPORTED_STO_V90, Type.BOOLEAN, (engine == Engine.IWD || engine == Engine.IWD2));

    addEntry(Key.IS_SUPPORTED_TIS_V1, Type.BOOLEAN, Boolean.valueOf(true));
    addEntry(Key.IS_SUPPORTED_TIS_V2, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_TLK, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_TO_V1, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.IWD));
    addEntry(Key.IS_SUPPORTED_TO_V2, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_TTF, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_VAR, Type.BOOLEAN, (engine == Engine.PST));

    addEntry(Key.IS_SUPPORTED_VEF, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(Key.IS_SUPPORTED_VVC, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(Key.IS_SUPPORTED_WAV, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_WAVC, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_WBM, Type.BOOLEAN, isEnhancedEdition());

    addEntry(Key.IS_SUPPORTED_WED, Type.BOOLEAN, Boolean.valueOf(true));

    addEntry(Key.IS_SUPPORTED_WFX, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.EE));

    addEntry(Key.IS_SUPPORTED_WMP, Type.BOOLEAN, Boolean.valueOf(true));
  }

  // Initializes game-specific features
  private void initFeatures()
  {
    Game game = getGame();
    Engine engine = getEngine();

    // Are Kits supported?
    addEntry(Key.IS_SUPPORTED_KITS, Type.BOOLEAN, (engine == Engine.BG2 || engine == Engine.IWD2 ||
                                               engine == Engine.EE));

    // the actual name of the "Alignment" IDS resource
    addEntry(Key.GET_IDS_ALIGNMENT, Type.STRING, (engine == Engine.IWD2) ? "ALIGNMNT.IDS" : "ALIGNMEN.IDS");

    // display mode of overlays in tilesets
    addEntry(Key.IS_TILESET_STENCILED, Type.BOOLEAN, (engine == Engine.BG2 || game == Game.BG2EE));

    // Has TobEx been installed?
    if (engine == Engine.BG2) {
      File tobexIni = new FileNI((File)getProperty(Key.GET_GAME_ROOT_FOLDER), "TobEx_ini/TobExCore.ini");
      addEntry(Key.IS_GAME_TOBEX, Type.BOOLEAN, tobexIni.isFile());
    } else {
      addEntry(Key.IS_GAME_TOBEX, Type.BOOLEAN, Boolean.FALSE);
    }
  }

//-------------------------- INNER CLASSES --------------------------

  // Internal definition of a property entry
  private static class Property
  {
    private final Key key;
    private final Type type;

    private Object data;

    /**
     * Initialize a new property.
     * @param key A unique identifier for the property. Cannot be modified afterwards.
     * @param type The data type of the property. Cannot be modified afterwards.
     * @param data The actual data of the property. Can be modified afterwards.
     */
    public Property(Key key, Type type, Object data)
    {
      this.key = key;
      this.type = type;
      this.data = data;
    }

    /** Returns a unique key which identifies this property. */
    public Key getKey() { return key; }

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
