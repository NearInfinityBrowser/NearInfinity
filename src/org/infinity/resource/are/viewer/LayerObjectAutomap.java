// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.nio.file.Path;

import org.infinity.datatype.IsNumeric;
import org.infinity.gui.layeritem.AbstractLayerItem;
import org.infinity.gui.layeritem.IconLayerItem;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.Viewable;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.are.viewer.icon.ViewerIcons;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.to.StrRefEntry;
import org.infinity.resource.to.StrRefEntry2;
import org.infinity.resource.to.StringEntry;
import org.infinity.resource.to.StringEntry2;
import org.infinity.resource.to.TohResource;
import org.infinity.resource.to.TotResource;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

/**
 * Handles specific layer type: ARE/Automap Note (except for PST)
 */
public class LayerObjectAutomap extends LayerObject
{
  private static final Image[] ICONS = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_1),
                                        Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_2)};
  private static final Point CENTER = new Point(26, 26);

  private final AutomapNote note;
  private final Point location = new Point();

  private final IconLayerItem item;


  public LayerObjectAutomap(AreResource parent, AutomapNote note)
  {
    super("Automap", AutomapNote.class, parent);
    this.note = note;
    String msg = null;
    try {
      location.x = ((IsNumeric)note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_X)).getValue();
      location.y = ((IsNumeric)note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_Y)).getValue();
      if (((IsNumeric)note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT_LOCATION)).getValue() == 1) {// 1 - Dialog.tlk
        // fetching string from dialog.tlk
        msg = note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT).toString();
      } else {
        // fetching string from talk override
        msg = "[user-defined]";
        int srcStrref = ((IsNumeric)note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT)).getValue();
        if (srcStrref > 0) {
          String path = parent.getResourceEntry().getActualPath().toString();
          path = path.replace(parent.getResourceEntry().getResourceName(), "");
          if (Profile.isEnhancedEdition()) {
            // processing new TOH structure
            Path tohFile = FileManager.resolve(path, "DEFAULT.TOH");
            if (FileEx.create(tohFile).exists()) {
              FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
              TohResource toh = new TohResource(tohEntry);
              IsNumeric so = (IsNumeric)toh.getAttribute(TohResource.TOH_OFFSET_ENTRIES);
              IsNumeric sc = (IsNumeric)toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
              if (so != null && sc != null && sc.getValue() > 0) {
                for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
                  StrRefEntry2 strref = (StrRefEntry2)toh.getAttribute(curOfs, false);
                  if (strref != null) {
                    int v = ((IsNumeric)strref.getAttribute(StrRefEntry2.TOH_STRREF_OVERRIDDEN)).getValue();
                    if (v == srcStrref) {
                      int sofs = ((IsNumeric)strref.getAttribute(StrRefEntry2.TOH_STRREF_OFFSET_STRING)).getValue();
                      StringEntry2 se = (StringEntry2)toh.getAttribute(so.getValue() + sofs, false);
                      if (se != null) {
                        msg = se.getAttribute(StringEntry2.TOH_STRING_TEXT).toString();
                      }
                      break;
                    }
                    curOfs += strref.getSize();
                  }
                }
              }
            }
          } else {
            // processing legacy TOH/TOT structures
            Path tohFile = FileManager.resolve(path, "DEFAULT.TOH");
            Path totFile = FileManager.resolve(path, "DEFAULT.TOT");
            if (FileEx.create(tohFile).exists() && FileEx.create(totFile).exists()) {
              FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
              FileResourceEntry totEntry = new FileResourceEntry(totFile);
              TohResource toh = new TohResource(tohEntry);
              TotResource tot = new TotResource(totEntry);
              IsNumeric sc = (IsNumeric)toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
              if (sc != null && sc.getValue() > 0) {
                for (int i = 0, count = sc.getValue(), curOfs = 0x14; i < count; i++) {
                  StrRefEntry strref = (StrRefEntry)toh.getAttribute(curOfs, false);
                  if (strref != null) {
                    int v = ((IsNumeric)strref.getAttribute(StrRefEntry.TOH_STRREF_OVERRIDDEN)).getValue();
                    if (v == srcStrref) {
                      int sofs = ((IsNumeric)strref.getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING)).getValue();
                      StringEntry se = (StringEntry)tot.getAttribute(sofs, false);
                      if (se != null) {
                        msg = se.getAttribute(StringEntry.TOT_STRING_TEXT).toString();
                      }
                      break;
                    }
                    curOfs += strref.getSize();
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
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
  public Viewable getViewable()
  {
    return note;
  }

  @Override
  public AbstractLayerItem getLayerItem(int type)
  {
    return (type == 0) ? item : null;
  }

  @Override
  public AbstractLayerItem[] getLayerItems()
  {
    return new AbstractLayerItem[]{item};
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }
}
