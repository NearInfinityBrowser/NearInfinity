// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.util.EventObject;

public class TilesetChangeEvent extends EventObject
{
  private boolean mapChanged, appearanceChanged, overlayChanged, doorStatechanged;

  /**
   * Constructs a TilesetChangeEvent object.
   * @param source The object that is the source of the event.
   * @param mapChanged If true, the whole map has been changed.
   * @param overlayChanged If true, the overlay tiles have been changed.
   * @param doorStatechanged If true, the door tiles have been changed.
   */
  public TilesetChangeEvent(Object source, boolean mapChanged, boolean appearanceChanged,
                            boolean overlayChanged, boolean doorStatechanged)
  {
    super(source);
    this.mapChanged = mapChanged;
    this.appearanceChanged = appearanceChanged;
    this.overlayChanged = overlayChanged;
    this.doorStatechanged = doorStatechanged;
  }

  /**
   * Returns true if the layout of the whole map has been changed (e.g. after loading a new tileset
   * or changing the zoom level).
   */
  public boolean hasChangedMap()
  {
    return mapChanged;
  }

  /**
   * Returns true if only the appearance of the map tiles has changed (e.g. after changing lighting
   * conditions or showing a grid).
   */
  public boolean hasChangedAppearance()
  {
    return appearanceChanged;
  }

  /**
   * Returns true if overlay tiles have been changed (e.g. after setting a different frame in
   * animated overlays).
   */
  public boolean hasChangedOverlay()
  {
    return overlayChanged;
  }

  /**
   * Returns true if door tiles have been changed after setting the opened/closed state of doors.
   */
  public boolean hasChangedDoorState()
  {
    return doorStatechanged;
  }
}
