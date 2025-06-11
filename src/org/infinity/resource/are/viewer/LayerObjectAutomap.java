// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.nio.file.Path;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.to.TohResource;
import org.infinity.util.Logger;
import org.infinity.util.io.FileManager;

/**
 * Handles specific layer type: ARE/Automap Note (except for PST)
 */
public class LayerObjectAutomap extends LayerObject {
  private static final Image[] ICONS = { ViewerIcons.ICON_ITM_AUTOMAP_1.getIcon().getImage(),
                                         ViewerIcons.ICON_ITM_AUTOMAP_2.getIcon().getImage() };

  private static final Point CENTER = ViewerIcons.ICON_ITM_AUTOMAP_1.getCenter();

  private final AutomapNote note;
  private final Point location = new Point();

  private final IconLayerItem item;

  public LayerObjectAutomap(AreResource parent, AutomapNote note) {
    super("Automap", parent);
    this.note = note;
    String msg = null;
    try {
      location.x = ((IsNumeric) note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_X)).getValue();
      location.y = ((IsNumeric) note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_Y)).getValue();
      if (((IsNumeric) note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT_LOCATION)).getValue() == 1) {// 1 - Dialog.tlk
        // fetching string from dialog.tlk
        msg = note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT).toString();
      } else {
        // fetching string from talk override
        int srcStrref = ((IsNumeric) note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT)).getValue();
        msg = String.format("[Overridden string (Strref: %d)]", srcStrref);
        if (srcStrref > 0) {
          String path = parent.getResourceEntry().getActualPath().toString();
          path = path.replace(parent.getResourceEntry().getResourceName(), "");
          final Path tohFile = FileManager.resolve(path, "DEFAULT.TOH");
          final Path totFile = FileManager.resolve(path, "DEFAULT.TOT");
          final String result = TohResource.getOverrideString(new FileResourceEntry(tohFile),
              new FileResourceEntry(totFile), srcStrref);
          if (result != null) {
            msg = result;
          }
        }
      }
    } catch (Exception e) {
      Logger.error(e);
    }

    // Using cached icons
    final Image[] icons = getIcons(ICONS);

    item = new IconLayerItem(note, msg, icons[0], CENTER);
    item.setLabelEnabled(Settings.ShowLabelMapNotes);
    item.setName(getCategory());
    item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icons[1]);
    item.setVisible(isVisible());
  }

  @Override
  public Viewable getViewable() {
    return note;
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
    if (item != null) {
      item.setItemLocation((int) (location.x * zoomFactor + (zoomFactor / 2.0)),
          (int) (location.y * zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
