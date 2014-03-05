// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.resource.are.viewer.ViewerConstants.LayerStackingType;
import infinity.resource.are.viewer.ViewerConstants.LayerType;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages global area viewer settings.
 * @author argent77
 */
public class Settings
{
  // Default layer order on map
  public static final ViewerConstants.LayerStackingType[] DefaultLayerOrder =
      new ViewerConstants.LayerStackingType[]{ViewerConstants.LayerStackingType.Actor,
                                              ViewerConstants.LayerStackingType.Entrance,
                                              ViewerConstants.LayerStackingType.Ambient,
                                              ViewerConstants.LayerStackingType.Animation,
                                              ViewerConstants.LayerStackingType.Automap,
                                              ViewerConstants.LayerStackingType.SpawnPoint,
                                              ViewerConstants.LayerStackingType.ProTrap,
                                              ViewerConstants.LayerStackingType.Container,
                                              ViewerConstants.LayerStackingType.Region,
                                              ViewerConstants.LayerStackingType.Door,
                                              ViewerConstants.LayerStackingType.DoorPoly,
                                              ViewerConstants.LayerStackingType.WallPoly,
                                              ViewerConstants.LayerStackingType.AmbientRange,
                                              ViewerConstants.LayerStackingType.Transition };
  public static final String[] LabelZoomFactor = new String[]{"Auto-fit", "25%", "33%", "50%", "100%", "200%", "300%", "400%"};
  public static final double[] ItemZoomFactor = new double[]{0.0, 0.25, 1.0/3.0, 0.5, 1.0, 2.0, 3.0, 4.0};
  public static final int ZoomFactorIndexAuto = 0;       // points to the auto-fit zoom factor
  public static final int ZoomFactorIndexDefault = 4;    // points to the default zoom factor (1x)

  // Defines stacking order of layer items on the map
  public static final List<ViewerConstants.LayerStackingType> ListLayerOrder = getDefaultLayerOrder();
  // Indicates whether to store settings on disk
  public static boolean StoreVisualSettings = getDefaultStoreVisualSettings();
  // Current open/closed state of door tiles and structures
  public static boolean DrawClosed = getDefaultDrawClosed();
  // Current visibility state of overlays
  public static boolean DrawOverlays = getDefaultDrawOverlays();
  // Current visibility state of the tile grid
  public static boolean DrawGrid = getDefaultDrawGrid();
  // Current visibility state of ambient range items
  public static boolean ShowAmbientRanges = getDefaultAmbientRanges();
  // Defines whether to ignore time schedules on layer items
  public static boolean IgnoreSchedules = getDefaultIgnoreSchedules();
  // Indicates whether to show frames around real background animations all the time
  public static int ShowFrame = getDefaultShowFrame();
  // Interpolation state of map tileset
  public static int InterpolationMap = getDefaultInterpolationMap();
  // Interpolation state of real background animations
  public static int InterpolationAnim = getDefaultInterpolationAnim();
  // Bitmask defining the enabled state of layer items
  public static int LayerFlags = getDefaultLayerFlags();
  // The visibility state of real background animations (icons/still/animated)
  public static int ShowRealAnimations = getDefaultShowRealAnimations();
  // The current time of day (in hours)
  public static int TimeOfDay = getDefaultTimeOfDay();
  // The current zoom level of the map (as combobox item index)
  public static int ZoomLevel = getDefaultZoomLevel();

  // Preferences keys for specific settings
  private static final String PREFS_STORESETTINGS = "StoreSettings";
  private static final String PREFS_DRAWCLOSED = "DrawClosed";
  private static final String PREFS_DRAWOVERLAYS = "DrawOverlays";
  private static final String PREFS_DRAWGRID = "DrawGrid";
  private static final String PREFS_SHOWFRAME = "ShowFrame";
  private static final String PREFS_SHOWAMBIENT = "ShowAmbientRanges";
  private static final String PREFS_IGNORESCHEDULES = "IgnoreSchedules";
  private static final String PREFS_LAYERFLAGS = "LayerFlags";
  private static final String PREFS_SHOWREALANIMS = "ShowRealAnimations";
  private static final String PREFS_TIMEOFDAY = "TimeOfDay";
  private static final String PREFS_ZOOMLEVEL = "ZoomLevel";
  private static final String PREFS_LAYERZORDER_FMT = "LayerZOrder%1$d";
  private static final String PREFS_INTERPOLATION_MAP = "InterpolationMap";
  private static final String PREFS_INTERPOLATION_ANIMS = "InterpolationAnims";

  private static boolean SettingsLoaded = false;

  /**
   * Loads stored viewer settings from disk if available and the store settings flag is enabled.
   * @param force If true, overrides the store settings flag and always loads settings from disk.
   */
  public static void loadSettings(boolean force)
  {
    if (!SettingsLoaded || force) {
      Preferences prefs = Preferences.userNodeForPackage(AreaViewer.class);

      // loading required settings
      StoreVisualSettings = prefs.getBoolean(PREFS_STORESETTINGS, getDefaultStoreVisualSettings());
      IgnoreSchedules = prefs.getBoolean(PREFS_IGNORESCHEDULES, getDefaultIgnoreSchedules());
      ShowFrame = prefs.getInt(PREFS_SHOWFRAME, getDefaultShowFrame());
      InterpolationMap = prefs.getInt(PREFS_INTERPOLATION_MAP, getDefaultInterpolationMap());
      InterpolationAnim = prefs.getInt(PREFS_INTERPOLATION_ANIMS, getDefaultInterpolationAnim());

      // loading layer z-order
      ListLayerOrder.clear();
      for (int i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
        int idx = prefs.getInt(String.format(PREFS_LAYERZORDER_FMT, i), -1);
        if (idx >= 0 && idx < ViewerConstants.LayerStackingType.values().length) {
          ListLayerOrder.add(ViewerConstants.LayerStackingType.values()[idx]);
        } else {
          ListLayerOrder.add(DefaultLayerOrder[i]);
        }
      }

      // loading optional settings
      if (StoreVisualSettings || force) {
        DrawClosed = prefs.getBoolean(PREFS_DRAWCLOSED, getDefaultDrawClosed());
        DrawOverlays = prefs.getBoolean(PREFS_DRAWOVERLAYS, getDefaultDrawOverlays());
        DrawGrid = prefs.getBoolean(PREFS_DRAWGRID, getDefaultDrawGrid());
        ShowAmbientRanges = prefs.getBoolean(PREFS_SHOWAMBIENT, getDefaultAmbientRanges());
        LayerFlags = prefs.getInt(PREFS_LAYERFLAGS, getDefaultLayerFlags());
        ShowRealAnimations = prefs.getInt(PREFS_SHOWREALANIMS, getDefaultShowRealAnimations());
        TimeOfDay = prefs.getInt(PREFS_TIMEOFDAY, getDefaultTimeOfDay());
        ZoomLevel = prefs.getInt(PREFS_ZOOMLEVEL, getDefaultZoomLevel());
      }
      validateSettings();
      SettingsLoaded = true;
    }
  }

  /**
   * Stores current global viewer settings on disk only if storing settings is enabled.
   * @param force If true, always stores settings to disk, ignoring the store settings flag.
   */
  public static void storeSettings(boolean force)
  {
    validateSettings();
    Preferences prefs = Preferences.userNodeForPackage(AreaViewer.class);

    // storing basic settings
    prefs.putBoolean(PREFS_STORESETTINGS, StoreVisualSettings);
    prefs.putBoolean(PREFS_IGNORESCHEDULES, IgnoreSchedules);
    prefs.putInt(PREFS_SHOWFRAME, ShowFrame);
    prefs.putInt(PREFS_INTERPOLATION_MAP, InterpolationMap);
    prefs.putInt(PREFS_INTERPOLATION_ANIMS, InterpolationAnim);

    // storing layer z-order
    for (int i = 0; i < ListLayerOrder.size(); i++) {
      prefs.putInt(String.format(PREFS_LAYERZORDER_FMT, i), getLayerStackingTypeIndex(ListLayerOrder.get(i)));
    }

    // storing optional settings
    if (StoreVisualSettings || force) {
      prefs.putBoolean(PREFS_DRAWCLOSED, DrawClosed);
      prefs.putBoolean(PREFS_DRAWOVERLAYS, DrawOverlays);
      prefs.putBoolean(PREFS_DRAWGRID, DrawGrid);
      prefs.putBoolean(PREFS_SHOWAMBIENT, ShowAmbientRanges);
      prefs.putInt(PREFS_LAYERFLAGS, LayerFlags);
      prefs.putInt(PREFS_SHOWREALANIMS, ShowRealAnimations);
      prefs.putInt(PREFS_TIMEOFDAY, TimeOfDay);
      prefs.putInt(PREFS_ZOOMLEVEL, ZoomLevel);
    }
    try {
      prefs.flush();
    } catch (BackingStoreException e) {
      e.printStackTrace();
    }
  }

  // Makes sure that all settings are valid
  private static void validateSettings()
  {
    int mask = (1 << LayerManager.getLayerTypeCount()) - 1;
    LayerFlags &= mask;

    ShowRealAnimations = Math.min(Math.max(ShowRealAnimations, ViewerConstants.ANIM_SHOW_NONE),
                                  ViewerConstants.ANIM_SHOW_ANIMATED);
    TimeOfDay = Math.min(Math.max(TimeOfDay, ViewerConstants.TIME_0), ViewerConstants.TIME_23);
    ZoomLevel = Math.min(Math.max(ZoomLevel, 0), ItemZoomFactor.length - 1);
    InterpolationMap = Math.min(Math.max(InterpolationMap, ViewerConstants.INTERPOLATION_AUTO),
                                ViewerConstants.INTERPOLATION_BILINEAR);
    InterpolationAnim = Math.min(Math.max(InterpolationAnim, ViewerConstants.INTERPOLATION_AUTO),
                                 ViewerConstants.INTERPOLATION_BILINEAR);

    // validating layers z-order
    mask = 0;
    // 1. checking for duplicates
    int i = 0;
    while (i < ListLayerOrder.size()) {
      int bit = 1 << getLayerStackingTypeIndex(ListLayerOrder.get(i));
      if ((mask & bit) != 0) {
        ListLayerOrder.remove(i);
        continue;
      } else {
        mask |= bit;
      }
      i++;
    }
    // 2. adding missing layers
    for (i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
      int bit = 1 << i;
      if ((mask & bit) == 0) {
        ListLayerOrder.add(ViewerConstants.LayerStackingType.values()[i]);
      }
    }
  }

  public static List<ViewerConstants.LayerStackingType> getDefaultLayerOrder()
  {
    List<ViewerConstants.LayerStackingType> list = new ArrayList<ViewerConstants.LayerStackingType>();
    for (int i = 0; i < DefaultLayerOrder.length; i++) {
      list.add(DefaultLayerOrder[i]);
    }
    return list;
  }

  public static boolean getDefaultStoreVisualSettings()
  {
    return false;
  }

  public static boolean getDefaultDrawClosed()
  {
    return false;
  }

  public static boolean getDefaultDrawOverlays()
  {
    return true;
  }

  public static boolean getDefaultDrawGrid()
  {
    return false;
  }

  public static boolean getDefaultAmbientRanges()
  {
    return false;
  }

  public static boolean getDefaultIgnoreSchedules()
  {
    return false;
  }

  public static int getDefaultShowFrame()
  {
    return ViewerConstants.FRAME_AUTO;
  }

  public static int getDefaultInterpolationMap()
  {
    return ViewerConstants.INTERPOLATION_AUTO;
  }

  public static int getDefaultInterpolationAnim()
  {
    return ViewerConstants.INTERPOLATION_AUTO;
  }

  public static int getDefaultLayerFlags()
  {
    return 0;
  }

  public static int getDefaultShowRealAnimations()
  {
    return ViewerConstants.ANIM_SHOW_NONE;
  }

  public static int getDefaultTimeOfDay()
  {
    return ViewerConstants.getHourOf(ViewerConstants.LIGHTING_DAY);
  }

  public static int getDefaultZoomLevel()
  {
    return ZoomFactorIndexDefault;
  }

  // Converts values from LayerStackingType to LayerType
  public static LayerType stackingToLayer(LayerStackingType type)
  {
    switch (type) {
      case Actor:
        return LayerType.Actor;
      case Ambient:
      case AmbientRange:
        return LayerType.Ambient;
      case Animation:
        return LayerType.Animation;
      case Automap:
        return LayerType.Automap;
      case Container:
        return LayerType.Container;
      case Door:
        return LayerType.Door;
      case DoorPoly:
        return LayerType.DoorPoly;
      case Entrance:
        return LayerType.Entrance;
      case ProTrap:
        return LayerType.ProTrap;
      case Region:
        return LayerType.Region;
      case SpawnPoint:
        return LayerType.SpawnPoint;
      case Transition:
        return LayerType.Transition;
      case WallPoly:
        return LayerType.WallPoly;
      default:
        return null;
    }
  }

  // Converts values from LayerType to LayerStackingType (ignoring AmbientRange)
  public static LayerStackingType layerToStacking(LayerType type)
  {
    switch (type)
    {
      case Actor:
        return LayerStackingType.Actor;
      case Ambient:
        return LayerStackingType.Ambient;
      case Animation:
        return LayerStackingType.Animation;
      case Automap:
        return LayerStackingType.Automap;
      case Container:
        return LayerStackingType.Container;
      case Door:
        return LayerStackingType.Door;
      case DoorPoly:
        return LayerStackingType.DoorPoly;
      case Entrance:
        return LayerStackingType.Entrance;
      case ProTrap:
        return LayerStackingType.ProTrap;
      case Region:
        return LayerStackingType.Region;
      case SpawnPoint:
        return LayerStackingType.SpawnPoint;
      case Transition:
        return LayerStackingType.Transition;
      case WallPoly:
        return LayerStackingType.WallPoly;
      default:
        return null;
    }
  }

  // Returns the index of the specified enum type
  public static int getLayerStackingTypeIndex(ViewerConstants.LayerStackingType type)
  {
    for (int i = 0; i < ViewerConstants.LayerStackingType.values().length; i++) {
      if (type == ViewerConstants.LayerStackingType.values()[i]) {
        return i;
      }
    }
    return -1;
  }
}
