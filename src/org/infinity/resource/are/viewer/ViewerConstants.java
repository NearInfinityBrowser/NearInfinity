// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.RenderingHints;

/**
 * Definitions of constants and custom types used throughout the area viewer classes.
 */
public final class ViewerConstants
{
  /**
   * Supported layer types.
   */
  public static enum LayerType {ACTOR, REGION, ENTRANCE, CONTAINER, AMBIENT, DOOR, ANIMATION,
                                AUTOMAP, SPAWN_POINT, TRANSITION, PRO_TRAP, DOOR_POLY, WALL_POLY }

  // Used for setting stacking order on map
  public static enum LayerStackingType {ACTOR, REGION, ENTRANCE, CONTAINER, AMBIENT, AMBIENT_RANGE,
                                        DOOR, ANIMATION, AUTOMAP, SPAWN_POINT, TRANSITION,
                                        PRO_TRAP, DOOR_POLY, WALL_POLY }

  // Flags that identify the different control sections in the sidebar
  public static final int SIDEBAR_VISUALSTATE = 1 << 0;
  public static final int SIDEBAR_LAYERS      = 1 << 1;
  public static final int SIDEBAR_MINIMAPS    = 1 << 2;

  // Identifiers for specifying day/night versions of ARE/WED resources
  public static final int AREA_DAY    = 0;
  public static final int AREA_NIGHT  = 1;

  // Identifiers for specifying the search/light/height maps for the current area
  public static final int MAP_NONE    = -1;
  public static final int MAP_SEARCH  = 0;
  public static final int MAP_LIGHT   = 1;
  public static final int MAP_HEIGHT  = 2;

  // Filtering methods for graphics objects
  public static final int FILTERING_AUTO            = 0;
  public static final int FILTERING_NEARESTNEIGHBOR = 1;
  public static final int FILTERING_BILINEAR        = 2;

  // Frames around layer items
  public static final int FRAME_NEVER   = 0;  // never show frame
  public static final int FRAME_AUTO    = 1;  // show frame on mouse-over
  public static final int FRAME_ALWAYS  = 2;  // always show frame

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

  // Door state indices (LayerObjectDoor)
  public static final int DOOR_OPEN   = 0;
  public static final int DOOR_CLOSED = 1;

  // The layer item types used (LayerObjectAmbient)
  public static final int AMBIENT_ITEM_ICON  = 0;
  public static final int AMBIENT_ITEM_RANGE = 1;

  // The ambient sound type
  public static final int AMBIENT_TYPE_GLOBAL = 1 << 0;
  public static final int AMBIENT_TYPE_LOCAL  = 1 << 1;
  public static final int AMBIENT_TYPE_ALL    = AMBIENT_TYPE_GLOBAL | AMBIENT_TYPE_LOCAL;

  // Edge of the map transition (LayerObjectTransition)
  public static final int EDGE_NORTH  = 0;
  public static final int EDGE_EAST   = 1;
  public static final int EDGE_SOUTH  = 2;
  public static final int EDGE_WEST   = 3;

  // Day time definitions by hour
  public static final int TIME_0  = 0;    // 00:30 to 01:29
  public static final int TIME_1  = 1;    // 01:30 to 02:29
  public static final int TIME_2  = 2;    // 02:30 to 03:29
  public static final int TIME_3  = 3;    // 03:30 to 04:29
  public static final int TIME_4  = 4;    // 04:30 to 05:29
  public static final int TIME_5  = 5;    // 05:30 to 06:29
  public static final int TIME_6  = 6;    // 06:30 to 07:29
  public static final int TIME_7  = 7;    // 07:30 to 08:29
  public static final int TIME_8  = 8;    // 08:30 to 09:29
  public static final int TIME_9  = 9;    // 09:30 to 10:29
  public static final int TIME_10 = 10;   // 10:30 to 11:29
  public static final int TIME_11 = 11;   // 11:30 to 12:29
  public static final int TIME_12 = 12;   // 12:30 to 13:29
  public static final int TIME_13 = 13;   // 13:30 to 14:29
  public static final int TIME_14 = 14;   // 14:30 to 15:29
  public static final int TIME_15 = 15;   // 15:30 to 16:29
  public static final int TIME_16 = 16;   // 16:30 to 17:29
  public static final int TIME_17 = 17;   // 17:30 to 18:29
  public static final int TIME_18 = 18;   // 18:30 to 19:29
  public static final int TIME_19 = 19;   // 19:30 to 20:29
  public static final int TIME_20 = 20;   // 20:30 to 21:29
  public static final int TIME_21 = 21;   // 21:30 to 22:29
  public static final int TIME_22 = 22;   // 22:30 to 23:29
  public static final int TIME_23 = 23;   // 23:30 to 00:29

  // symbolic day times
  public static final int TIME_DAY      = 12;
  public static final int TIME_TWILIGHT = 21;
  public static final int TIME_NIGHT    = 1;


  /**
   * Returns the general day time (day/twilight/night) of the specified hour.
   * @param hour The hour in range [0..23].
   * @return Either of {@link #LIGHTING_DAY}, {@link #LIGHTING_TWILIGHT} or {@link #LIGHTING_NIGHT}.
   */
  public static int getDayTime(int hour)
  {
    while (hour < 0) { hour += 24; }
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
        return ViewerConstants.LIGHTING_NIGHT;
      case 6:
      case 21:
        return ViewerConstants.LIGHTING_TWILIGHT;
      default:
        return ViewerConstants.LIGHTING_DAY;
    }
  }

  /**
   * Returns the default hour of the specified day time (day/twilight/night)
   * @param dayTime Either of {@link #LIGHTING_DAY}, {@link #LIGHTING_TWILIGHT} or {@link #LIGHTING_NIGHT}.
   * @return The default hour of the specified day time.
   */
  public static int getHourOf(int dayTime)
  {
    switch (dayTime) {
      case ViewerConstants.LIGHTING_TWILIGHT:
        return ViewerConstants.TIME_TWILIGHT;
      case ViewerConstants.LIGHTING_NIGHT:
        return ViewerConstants.TIME_NIGHT;
      default:
        return ViewerConstants.TIME_DAY;
    }
  }
}
