// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.image.AffineTransformOp;

/**
 * Definitions of constants and custom types used throughout the area viewer classes.
 * @author argent77
 */
public final class ViewerConstants
{
  // Lighting conditions to simulate different day times (AnimatedLayerItem, TilesetRenderer)
  public static final int LIGHTING_DAY      = 0;
  public static final int LIGHTING_TWILIGHT = 1;
  public static final int LIGHTING_NIGHT    = 2;

  // Interpolation types used in scaling (AnimatedLayerItem, TilesetRenderer)
  public static final int TYPE_NEAREST_NEIGHBOR = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
  public static final int TYPE_BILINEAR         = AffineTransformOp.TYPE_BILINEAR;
  public static final int TYPE_BICUBIC          = AffineTransformOp.TYPE_BICUBIC;

  // Specifies the item type for animation objects (LayerObjectAnimation)
  public static int ANIM_ICON = 0;
  public static int ANIM_REAL = 1;

  // Different states of showing background animations (AreaViewer)
  public static final int ANIM_SHOW_NONE      = 0;
  public static final int ANIM_SHOW_STILL     = 1;
  public static final int ANIM_SHOW_ANIMATED  = 2;

  // Door state indices (LayerObjectDoor)
  public static final int DOOR_OPEN   = 0;
  public static final int DOOR_CLOSED = 1;

  // The layer item types used (LayerObjectAmbient)
  public static final int AMBIENT_ICON  = 0;
  public static final int AMBIENT_RANGE = 1;

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
}
