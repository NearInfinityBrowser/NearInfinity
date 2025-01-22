// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.resource.Closeable;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wmp.AreaEntry;
import org.infinity.resource.wmp.MapEntry;
import org.infinity.resource.wmp.WmpResource;

public class WmpMapInfo implements Closeable {
  private final List<WmpAreaInfo> areasList = new ArrayList<>();

  private final MapEntry mapEntry;
  private final boolean autoLoadVirtualMap;

  private VirtualMapEntry virtualMapEntry;
  private ResourceEntry backgroundResource;
  private ResourceEntry iconsResource;
  private BamDecoder mapIcons;
  private BamControl mapIconsControl;
  private int width;
  private int height;
  private int id;
  private int nameStrref;
  private int centerX;
  private int centerY;

  public WmpMapInfo(MapEntry mapEntry) throws Exception {
    this(mapEntry, true);
  }

  public WmpMapInfo(MapEntry mapEntry, boolean includeVirtualMap) throws Exception {
    this.mapEntry = Objects.requireNonNull(mapEntry);
    this.autoLoadVirtualMap = includeVirtualMap;
    init();
  }

  @Override
  public void close() throws Exception {
    if (mapIcons != null) {
      mapIcons.close();
    }
    mapIcons = null;
    mapIconsControl = null;
    virtualMapEntry = null;
    while (areasList.isEmpty()) {
      areasList.remove(areasList.size() - 1).close();
    }
  }

  /** Discards existing and reinitializes new map data. */
  public void reset() throws Exception {
    close();
    init();
  }

  /**
   * Imports definitions from a {@link VirtualMapEntry} instance.
   *
   * @param vme The {@link VirtualMapEntry} instance to import.
   */
  public void loadVirtualMap(VirtualMapEntry vme) throws Exception {
    if (vme != null && virtualMapEntry == null) {
      virtualMapEntry = vme;
      for (int i = 0, count = vme.getAreaCount(); i < count; i++) {
        final WmpAreaInfo wai = new WmpAreaInfo(this, vme.getAreaEntry(i));
        areasList.add(wai);
      }
    }
  }

  /**
   * Returns a list of available virtual area definitions.
   *
   * @return List of {@link VirtualAreaEntry} instances available for this map.
   */
  public List<VirtualAreaEntry> getVirtualAreas() {
    final List<VirtualAreaEntry> retVal = new ArrayList<>();
    if (virtualMapEntry != null) {
      for (int i = 0, count = virtualMapEntry.getAreaCount(); i < count; i++) {
        retVal.add(virtualMapEntry.getAreaEntry(i));
      }
    }
    return retVal;
  }

  public MapEntry getMapEntry() {
    return mapEntry;
  }

  public ResourceEntry getBackgroundResource() {
    return backgroundResource;
  }

  public ResourceEntry getMapIconsResource() {
    return iconsResource;
  }

  public BamDecoder getMapIcons() {
    return mapIcons;
  }

  public BamControl getMapIconsControl() {
    return mapIconsControl;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getMapId() {
    return id;
  }

  public int getMapNameStrref() {
    return nameStrref;
  }

  public int getCenterX() {
    return centerX;
  }

  public int getCenterY() {
    return centerY;
  }

  /** Returns a read-only list of the defined areas for this map. */
  public List<WmpAreaInfo> getAreaList() {
    return Collections.unmodifiableList(areasList);
  }

  /**
   * Returns the index of the area in the areas list.
   *
   * @param areaName Name of the area (with or without file extension).
   * @return Index of the matching area if available, -1 otherwise.
   */
  public int indexOfArea(String areaName) {
    if (areaName != null) {
      if (areaName.toUpperCase().endsWith(".ARE")) {
        areaName = areaName.substring(0, areaName.length() - 4);
      }
      for (int i = 0, count = areasList.size(); i < count; i++) {
        final ResourceEntry curArea = areasList.get(i).getCurrentArea();
        if (curArea != null && areaName.equalsIgnoreCase(areasList.get(i).getCurrentArea().getResourceRef())) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Returns the index of the area in the areas list.
   *
   * @param areaEntry {@link StructEntry} instance of the area definition.
   *                  Supported classes: {@link AreaEntry}, {@link VirtualAreaEntry}
   * @return Index of the matching area if available, -1 otherwise.
   */
  public int indexOfArea(StructEntry areaEntry) {
    int retVal = -1;
    if (areaEntry != null) {
      for (int i = 0, count = areasList.size(); i < count; i++) {
        final WmpAreaInfo wai = areasList.get(i);
        if (Objects.equals(wai.getAreaEntry(), areaEntry) ||
            Objects.equals(wai.getVirtualAreaEntry(), areaEntry)) {
          return i;
        }
      }
    }
    return retVal;
  }

  @Override
  public String toString() {
    return "WmpMapInfo [backgroundResource=" + backgroundResource + ", iconsResource=" + iconsResource + ", mapIcons="
        + mapIcons + ", mapIconsControl=" + mapIconsControl + ", width=" + width + ", height=" + height + ", id=" + id
        + ", nameStrref=" + nameStrref + ", centerX=" + centerX + ", centerY=" + centerY + ", autoLoadVirtualMap="
        + autoLoadVirtualMap + ", mapEntry=" + mapEntry + ", virtualMapEntry=" + virtualMapEntry + "]";
  }

  private void init() throws Exception {
    String resName = ((IsReference)mapEntry.getAttribute(MapEntry.WMP_MAP_RESREF)).getResourceName();
    backgroundResource = ResourceFactory.getResourceEntry(resName);

    resName = ((IsReference)mapEntry.getAttribute(MapEntry.WMP_MAP_ICONS)).getResourceName();
    iconsResource = ResourceFactory.getResourceEntry(resName);
    mapIcons = BamDecoder.loadBam(iconsResource);
    mapIconsControl = mapIcons.createControl();

    width = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_WIDTH)).getValue();
    height = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_HEIGHT)).getValue();
    id = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_ID)).getValue();
    nameStrref = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_NAME)).getValue();
    centerX = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_CENTER_X)).getValue();
    centerY = ((IsNumeric)mapEntry.getAttribute(MapEntry.WMP_MAP_CENTER_Y)).getValue();

    for (final StructEntry se : mapEntry.getFields(AreaEntry.class)) {
      if (se instanceof AreaEntry) {
        final AreaEntry ae = (AreaEntry)se;
        areasList.add(new WmpAreaInfo(this, ae));
      }
    }

    if (autoLoadVirtualMap && mapEntry.getParent() instanceof WmpResource) {
      final VirtualMapEntry vme = new VirtualMapEntry(mapEntry.getParent().getResourceEntry());
      loadVirtualMap(vme);
    }
  }
}