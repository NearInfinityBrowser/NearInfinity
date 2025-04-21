// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.Objects;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AutomapNotePST;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;
import org.infinity.util.StringTable;

/**
 * Handles specific layer type: ARE/Automap Note (PST-specific, predefined)
 */
public class LayerObjectAutomapPSTIni extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_AUTOMAP_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_AUTOMAP_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_AUTOMAP_1.getCenter();

  private static final double MAP_SCALE = 32.0 / 3.0; // scaling factor for MOS to TIS coordinates

  private static final String AUTONOTE_INI = "autonote.ini";

  private final Point location = new Point();

  private final PlainTextResource ini;
  private final IniMapSection areData;
  private final int noteIndex;
  private final IconLayerItem item;

  public LayerObjectAutomapPSTIni(IniMapSection areData, int noteIndex) throws IllegalArgumentException {
    super("Automap", AutomapNotePST.class, null);
    try {
      this.ini = new PlainTextResource(ResourceFactory.getResourceEntry(AUTONOTE_INI));
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not load autonote.ini");
    }
    this.areData = Objects.requireNonNull(areData);
    this.noteIndex = noteIndex;
    this.item = initLayerItem();
  }

  @Override
  public Viewable getViewable() {
    return ini;
  }

  @Override
  public AbstractLayerItem[] getLayerItems(int type) {
    if (type == 0 && item != null) {
      return new AbstractLayerItem[] { item };
    }
    return new AbstractLayerItem[0];
  }

  @Override
  public AbstractLayerItem[] getLayerItems() {
    return new AbstractLayerItem[] { item };
  }

  @Override
  public void update(double zoomFactor) {
    item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
        (int) (location.y * zoomFactor + (zoomFactor / 2.0)));
  }

  private IconLayerItem initLayerItem() throws IllegalArgumentException {
    IconLayerItem retVal;

    final int count = areData.getAsInteger("count", 0);
    if (noteIndex < 0 || noteIndex >= count) {
      throw new IllegalArgumentException("Automap note definition out of bounds: index=" + noteIndex + ", count=" + count);
    }

    final String keyStrref = "text" + (noteIndex + 1);
    final String keyX = "xPos" + (noteIndex + 1);
    final String keyY = "yPos" + (noteIndex + 1);
    final int strref = areData.getAsInteger(keyStrref, -1);
    final int x = (int) (areData.getAsInteger(keyX, -1) * MAP_SCALE);
    final int y = (int) (areData.getAsInteger(keyY, -1) * MAP_SCALE);
    if (x < 0 || y < 0) {
      throw new IllegalArgumentException("Invalid automap note location: x=" + x + ", y=" + y);
    }
    final String label = StringTable.getStringRef(strref);

    location.x = x;
    location.y = y;

    final IniMapEntry areDataEntry = areData.getEntry(keyStrref);
    if (areDataEntry != null) {
      ini.setHighlightedLine(areDataEntry.getLine() + 1);
    } else {
      ini.setHighlightedLine(areData.getLine() + 1);
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    retVal = new IconLayerItem(ini, label, icons[0], CENTER);
    retVal.setLabelEnabled(Settings.ShowLabelMapNotes);
    retVal.setName(getCategory());
    retVal.setToolTipText(label);
    retVal.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    retVal.setVisible(isVisible());

    return retVal;
  }
}
