// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.RenderingHints;
import java.util.Objects;

import org.infinity.resource.Profile;

/**
 * Definitions of constants and custom types used throughout the area viewer classes.
 */
public final class ViewerConstants {
  /**
   * Supported layer types.
   */
  public enum LayerType {
    ACTOR("Actors", true),
    REGION("Regions", true),
    ENTRANCE("Entrances", true),
    CONTAINER("Containers", true),
    AMBIENT("Ambient Sounds", true),
    DOOR("Doors", true),
    DOOR_CELLS("Impeded Door Cells", true),
    ANIMATION("Background Animations", true),
    AUTOMAP("Automap Notes", true),
    SPAWN_POINT("Spawn Points", true),
    TRANSITION("Map Transitions", true),
    PRO_TRAP("Projectile Traps", true),
    DOOR_POLY("Door Polygons", false),
    WALL_POLY("Wall Polygons", false),
    ;

    private final boolean isAre;
    private final String label;
    LayerType(String label, boolean isAre) {
      this.label = label;
      this.isAre = isAre;
    }

    /** Returns a label associated with the layer type. */
    public String getLabel() {
      return label;
    }

    /** Returns whether the layer type is defined in ARE resources. */
    public boolean isAre() {
      return isAre;
    }

    /** Returns whether the layer type is defined in WED resources. */
    public boolean isWed() {
      return !isAre;
    }
  }

  // Used for setting stacking order on map
  public enum LayerStackingType {
    ACTOR("Actors"),
    REGION_TARGET("Region targets"),
    CONTAINER_TARGET("Container targets"),
    DOOR_TARGET("Door targets"),
    REGION("Regions"),
    ENTRANCE("Entrances"),
    CONTAINER("Containers"),
    AMBIENT("Ambient Sounds"),
    AMBIENT_RANGE("Ambient Sound Ranges"),
    DOOR("Doors"),
    DOOR_CELLS("Impeded Door Cells"),
    ANIMATION("Background Animations"),
    AUTOMAP("Automap Notes"),
    SPAWN_POINT("Spawn Points"),
    TRANSITION("Map Transitions"),
    PRO_TRAP("Projectile Traps"),
    DOOR_POLY("Door Polygons"),
    WALL_POLY("Wall Polygons"),
    ;

    private final String label;
    LayerStackingType(String label) {
      this.label = label;
    }

    /** Returns a label associated with the layer stacking type. */
    public String getLabel() {
      return label;
    }
  }

  /** Array of search map information objects. One entry per type. */
  public static final SearchMapInfo[] SEARCH_MAP_INFO = {
      // Parameters:    DescBG, DescIWD, DescPST,                           SoundBG,  SoundIWD,     Walkable, SeeThrough
      new SearchMapInfo("Obstacle", "Obstacle", "Obstacle",                 null,     null,         false,    false),  //  0
      new SearchMapInfo("Dirt", "Dirt", "Dirt",                             "WAL_04", "FS_DIRT*",   true,     true),   //  1
      new SearchMapInfo("Metal", "Snowy Wood", "Clutter",                   "WAL_MT", "FS_WDSN*",   true,     true),   //  2
      new SearchMapInfo("Wood", "Wood", "Wood",                             "WAL_02", "FS_WOOD*",   true,     true),   //  3
      new SearchMapInfo("Stone (echo)", "Stone (light)", "Stone (light)",   "WAL_05", "FS_TOMB*",   true,     true),   //  4
      new SearchMapInfo("Carpet", "Carpet", "Metal",                        "WAL_06", null,         true,     true),   //  5
      new SearchMapInfo("Water", "Snow", "Water",                           "WAL_01", null,         true,     true),   //  6
      new SearchMapInfo("Stone", "Stone", "Stone",                          "WAL_03", "FS_STON*",   true,     true),   //  7
      new SearchMapInfo("Obstacle", "Obstacle", "Obstacle",                 null,     null,         false,    true),   //  8
      new SearchMapInfo("Wood", "Wood", "Wood",                             "WAL_02", "FS_WOOD*",   true,     true),   //  9
      new SearchMapInfo("Wall", "Wall", "Wall",                             null,     null,         false,    false),  // 10
      new SearchMapInfo("Water", "Snow", "Water",                           "WAL_01", "FS_SNOW*",   true,     true),   // 11
      new SearchMapInfo("Water", "Water", "Water",                          null,     null,         false,    true),   // 12
      new SearchMapInfo("Roof/Wall", "Roof/Wall", "Roof/Wall",              null,     null,         false,    false),  // 13
      new SearchMapInfo("Worldmap Exit", "Worldmap Exit", "Worldmap Exit",  null,     null,         true,     true),   // 14
      new SearchMapInfo("Grass", "Grass", "Carpet",                         "WAL_04", "FS_GRAS*",   true,     true),   // 15
  };

  // Flags that identify the different control sections in the sidebar
  public static final int SIDEBAR_VISUALSTATE = 1;
  public static final int SIDEBAR_LAYERS      = 1 << 1;
  public static final int SIDEBAR_MINIMAPS    = 1 << 2;

  // Identifiers for specifying day/night versions of ARE/WED resources
  public static final int AREA_DAY    = 0;
  public static final int AREA_NIGHT  = 1;

  // Identifiers for specifying various minimaps for the current area
  public static final int MAP_NONE      = -1;
  public static final int MAP_SEARCH    = 0;
  public static final int MAP_LIGHT     = 1;
  public static final int MAP_HEIGHT    = 2;
  public static final int MAP_EXPLORED  = 3;

  // Filtering methods for graphics objects
  public static final int FILTERING_AUTO            = 0;
  public static final int FILTERING_NEARESTNEIGHBOR = 1;
  public static final int FILTERING_BILINEAR        = 2;

  // Frames around layer items
  public static final int FRAME_NEVER   = 0; // never show frame
  public static final int FRAME_AUTO    = 1; // show frame on mouse-over
  public static final int FRAME_ALWAYS  = 2; // always show frame

  // Lighting conditions to simulate different day times (AnimatedLayerItem, TilesetRenderer)
  public static final int LIGHTING_DAY      = 0;
  public static final int LIGHTING_TWILIGHT = 1;
  public static final int LIGHTING_NIGHT    = 2;

  // Interpolation types used in scaling (AnimatedLayerItem, TilesetRenderer)
  public static final Object TYPE_NEAREST_NEIGHBOR  = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
  public static final Object TYPE_BILINEAR          = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
  public static final Object TYPE_BICUBIC           = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

  // Specifies the item type for objects with icons and real animations
  public static final int ITEM_ICON = 0;
  public static final int ITEM_REAL = 1;

  // Different states of showing real animations (AreaViewer)
  public static final int ANIM_SHOW_NONE      = 0;
  public static final int ANIM_SHOW_STILL     = 1;
  public static final int ANIM_SHOW_ANIMATED  = 2;

  // The layer item types used (LayerObjectContainer, LayerObjectDoor, LayerObjectRegion)
  public static final int LAYER_ITEM_POLY = 1;
  public static final int LAYER_ITEM_ICON = 1 << 1;
  public static final int LAYER_ITEM_ANY  = LAYER_ITEM_POLY | LAYER_ITEM_ICON;

  // Door state indices (LayerObjectDoor, LayerObjectDoorPoly)
  public static final int DOOR_OPEN   = 1 << 4;
  public static final int DOOR_CLOSED = 1 << 5;
  public static final int DOOR_ANY    = DOOR_OPEN | DOOR_CLOSED;

  // The layer item types used (LayerObjectAmbient)
  public static final int AMBIENT_ITEM_ICON   = 1;
  public static final int AMBIENT_ITEM_RANGE  = 1 << 1;
  public static final int AMBIENT_ITEM_ANY    = AMBIENT_ITEM_ICON | AMBIENT_ITEM_RANGE;

  // The ambient sound type
  public static final int AMBIENT_TYPE_GLOBAL = 1;
  public static final int AMBIENT_TYPE_LOCAL  = 1 << 1;
  public static final int AMBIENT_TYPE_ALL    = AMBIENT_TYPE_GLOBAL | AMBIENT_TYPE_LOCAL;

  // Edge of the map transition (LayerObjectTransition)
  public static final int EDGE_NORTH  = 0;
  public static final int EDGE_EAST   = 1;
  public static final int EDGE_SOUTH  = 2;
  public static final int EDGE_WEST   = 3;

  // Day time definitions by hour
  public static final int TIME_0  = 0; // 00:30 to 01:29
  public static final int TIME_1  = 1; // 01:30 to 02:29
  public static final int TIME_2  = 2; // 02:30 to 03:29
  public static final int TIME_3  = 3; // 03:30 to 04:29
  public static final int TIME_4  = 4; // 04:30 to 05:29
  public static final int TIME_5  = 5; // 05:30 to 06:29
  public static final int TIME_6  = 6; // 06:30 to 07:29
  public static final int TIME_7  = 7; // 07:30 to 08:29
  public static final int TIME_8  = 8; // 08:30 to 09:29
  public static final int TIME_9  = 9; // 09:30 to 10:29
  public static final int TIME_10 = 10; // 10:30 to 11:29
  public static final int TIME_11 = 11; // 11:30 to 12:29
  public static final int TIME_12 = 12; // 12:30 to 13:29
  public static final int TIME_13 = 13; // 13:30 to 14:29
  public static final int TIME_14 = 14; // 14:30 to 15:29
  public static final int TIME_15 = 15; // 15:30 to 16:29
  public static final int TIME_16 = 16; // 16:30 to 17:29
  public static final int TIME_17 = 17; // 17:30 to 18:29
  public static final int TIME_18 = 18; // 18:30 to 19:29
  public static final int TIME_19 = 19; // 19:30 to 20:29
  public static final int TIME_20 = 20; // 20:30 to 21:29
  public static final int TIME_21 = 21; // 21:30 to 22:29
  public static final int TIME_22 = 22; // 22:30 to 23:29
  public static final int TIME_23 = 23; // 23:30 to 00:29

  // symbolic day times
  public static final int TIME_DAY      = 12;
  public static final int TIME_TWILIGHT = 21;
  public static final int TIME_NIGHT    = 1;

  /**
   * Returns the general day time (day/twilight/night) of the specified hour.
   *
   * @param hour The hour in range [0..23].
   * @return Either of {@link #LIGHTING_DAY}, {@link #LIGHTING_TWILIGHT} or {@link #LIGHTING_NIGHT}.
   */
  public static int getDayTime(int hour) {
    while (hour < 0) {
      hour += 24;
    }
    hour %= 24;

    switch (hour) {
      case 0:
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
      case 22:
      case 23:
        return LIGHTING_NIGHT;
      case 6:
      case 21:
        return LIGHTING_TWILIGHT;
      default:
        return LIGHTING_DAY;
    }
  }

  /**
   * Returns the default hour of the specified day time (day/twilight/night)
   *
   * @param dayTime Either of {@link #LIGHTING_DAY}, {@link #LIGHTING_TWILIGHT} or {@link #LIGHTING_NIGHT}.
   * @return The default hour of the specified day time.
   */
  public static int getHourOf(int dayTime) {
    switch (dayTime) {
      case LIGHTING_TWILIGHT:
        return TIME_TWILIGHT;
      case LIGHTING_NIGHT:
        return TIME_NIGHT;
      default:
        return TIME_DAY;
    }
  }

  // ----------------------------- INNER CLASSES -----------------------------

  /**
   * A helper class that provides detailed information about a specific search map type.
   */
  public static class SearchMapInfo {
    private final String[] description;   // 0=BG, 1=IWD, 2=PST
    private final String[] walkSound;     // 0=BG, 1=IWD
    private final boolean walkable;
    private final boolean seeThrough;

    /**
     * Creates a new search map info structure.
     *
     * @param descBG       Description string for BG/BG2/EE.
     * @param descIWD      Description string for IWD/IWD2. Specify {@code null} to inherit the BG description.
     * @param descPST      Description string for PST/PSTEE. Specify {@code null} to inherit the BG description.
     * @param walkSoundBG  Walk sound resref for BG/BG2/EE.
     * @param walkSoundIWD Walk sound resref for IWD/IWD2.
     * @param walkable     Whether the search map type can be passed by walking creatures.
     * @param seeThrough   Whether the search map type blocks sight nor not.
     */
    private SearchMapInfo(String descBG, String descIWD, String descPST, String walkSoundBG, String walkSoundIWD,
        boolean walkable, boolean seeThrough) {
      this.description = new String[3];
      this.description[0] = (descBG != null) ? descBG : "n/a";
      this.description[1] = (descIWD != null) ? descIWD : "n/a";
      this.description[2] = (descPST != null) ? descPST : "n/a";

      this.walkSound = new String[2];
      this.walkSound[0] = (walkSoundBG != null) ? walkSoundBG + ".WAV" : null;
      this.walkSound[1] = (walkSoundIWD != null) ? walkSoundIWD + ".WAV" : null;

      this.walkable = walkable;
      this.seeThrough = seeThrough;
    }

    /** Returns a short description for this search map type. */
    public String getDescription() {
      switch (Profile.getGame()) {
        case PST:
        case PSTEE:
          return description[2];
        case IWD:
        case IWDHoW:
        case IWDHowTotLM:
        case IWD2:
        case IWD2EE:
        case IWDEE:
          return description[1];
        default:
          return description[0];
      }
    }

    /**
     * Returns the filename of the walk sound resource associated with this search map type. Return {@code null} if no
     * sound is associated.
     */
    public String getWalkSound() {
      switch (Profile.getEngine()) {
        case PST:
          return null;
        case IWD:
        case IWD2:
          return walkSound[1];
        default:
          return (Profile.getGame() == Profile.Game.PSTEE) ? null : walkSound[0];
      }
    }

    /** Returns whether walking creatures can pass this search map type. */
    public boolean canWalk() {
      return walkable;
    }

    /** Returns whether this search map type blocks sight or not. */
    public boolean canSeeThrough() {
      return seeThrough;
    }

    /** Returns a formated string with all available attributes for this search map type. */
    public String getText() {
      return getText(true, true, true, true);
    }

    /**
     * Returns a formated string with the specified attributes for this search map type.
     *
     * @param desc          Whether to include the search index description in the output string.
     * @param walkSound     Whether to include the associated walk sound in the output string.
     * @param canWalk       Whether to include the {@code walkable} attribute in the output string.
     * @param canSeeThrough Whether to include the {@code seeThrough} attribute in the output string.
     * @return a formatted string.
     */
    public String getText(boolean desc, boolean walkSound, boolean canWalk, boolean canSeeThrough) {
      final StringBuilder sb = new StringBuilder();
      if (desc) {
        sb.append(getDescription());
      }

      if (walkSound && getWalkSound() != null) {
        if (sb.length() > 0) {
          sb.append(' ');
        }
        sb.append('[').append(getWalkSound()).append(']');
      }

      int flags = 0;
      if (canWalk) {
        if (sb.length() > 0) {
          sb.append(' ');
        }
        sb.append('(').append("can walk: ").append(Boolean.toString(canWalk()));
        flags++;
      }
      if (canSeeThrough) {
        if (flags == 0 && sb.length() > 0) {
          sb.append(' ');
        }
        if (flags > 0) {
          sb.append(", ");
        } else {
          sb.append('(');
        }
        sb.append("can see through: ").append(Boolean.toString(canSeeThrough()));
        flags++;
      }
      if (flags > 0) {
        sb.append(')');
      }

      return sb.toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(description, seeThrough, walkSound, walkable);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SearchMapInfo other = (SearchMapInfo)obj;
      return Objects.equals(description, other.description) && seeThrough == other.seeThrough
          && Objects.equals(walkSound, other.walkSound) && walkable == other.walkable;
    }

    @Override
    public String toString() {
      return "SearchMapInfo [description=" + description + ", walkSound=" + walkSound + ", walkable=" + walkable
          + ", seeThrough=" + seeThrough + "]";
    }
  }
}
