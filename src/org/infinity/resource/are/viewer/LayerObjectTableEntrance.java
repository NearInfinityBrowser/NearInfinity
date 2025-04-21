// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.Table2da;

/**
 * Handles specific layer type: ENTRIES.2DA/Entrance
 */
public class LayerObjectTableEntrance extends LayerObjectEntranceBase {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_TABLE_ENTRANCE_1.getIcon().getImage(),
      ViewerIcons.ICON_ITM_TABLE_ENTRANCE_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_TABLE_ENTRANCE_1.getCenter();

  private final Point location = new Point();

  private final PlainTextResource entries;
  private final IconLayerItem item;
  private final Table2da table;
  private final int rowIndex;
  private final int columnIndex;

  public LayerObjectTableEntrance(Table2da table, int row, int column)
      throws IllegalArgumentException, UnsupportedOperationException {
    super(null);

    if (table == null) {
      throw new NullPointerException("table is null");
    }

    if (row < 0 || row >= table.getRowCount()) {
      throw new IllegalArgumentException("Entrance row out of bounds (" + row + ")");
    }

    final String areaName = table.get(row, 0).toUpperCase(Locale.ROOT);
    if (column < 1 || column > table.getColCount(row)) {
      throw new IllegalArgumentException(areaName + ": Entrance column out of bounds (" + column + ")");
    }

    this.table = Objects.requireNonNull(table);
    this.rowIndex = row;
    this.columnIndex = column;

    final String locationValue = this.table.get(this.rowIndex, this.columnIndex);
    final Pattern pattern = Pattern.compile("(-?\\d+)\\.(-?\\d+)");
    final Matcher matcher = pattern.matcher(locationValue);
    if (!matcher.find()) {
      throw new UnsupportedOperationException();
    }
    try {
      final int x = Integer.parseInt(matcher.group(1));
      final int y = Integer.parseInt(matcher.group(2));
      this.location.x = x;
      this.location.y = y;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(areaName + ": Invalid entrance definition (" + locationValue + ")");
    }

    try {
      this.entries = new PlainTextResource(this.table.getResourceEntry());

      // highlighting row with entrance coordinates in text resource
      final String[] lines = this.entries.getText().split("\r?\n");
      for (int i = 3; i < lines.length; i++) {
        if (lines[i].toUpperCase(Locale.ROOT).contains(areaName)) {
          this.entries.setHighlightedLine(i + 1);
          break;
        }
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }

    // using cached icons
    final Image[] icons = getIcons(ICONS);
    final String label = "Entry" +  this.columnIndex;
    final String tooltip = label + " [" + table.getResourceEntry().getResourceName() + "]";

    this.item = new IconLayerItem(entries, label, icons[0], CENTER);
    this.item.setLabelEnabled(Settings.ShowLabelEntrances);
    this.item.setName(getCategory());
    this.item.setToolTipText(tooltip);
    this.item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    this.item.setVisible(isVisible());
  }

  /** Returns the 2DA table containing the entrance definitions, as a {@link Table2da} object. */
  public Table2da getTable() {
    return table;
  }

  /** Returns the table row index of the entrance definition. */
  public int getTableRow() {
    return rowIndex;
  }

  /** Returns the table column index of the entrance definition. */
  public int getTableColumn() {
    return columnIndex;
  }

  @Override
  public Viewable getViewable() {
    return entries;
  }

  @Override
  protected IconLayerItem getLayerItem() {
    return item;
  }

  @Override
  protected Point getLocation() {
    return location;
  }
}
