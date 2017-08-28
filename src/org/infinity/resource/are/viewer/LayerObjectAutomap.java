// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextEdit;
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
import org.infinity.util.io.FileManager;

/**
 * Handles specific layer type: ARE/Automap Note (except for PST)
 */
public class LayerObjectAutomap extends LayerObject
{
  private static final Image[] Icon = {Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_1),
                                       Icons.getImage(ViewerIcons.class, ViewerIcons.ICON_ITM_AUTOMAP_2)};
  private static Point Center = new Point(26, 26);

  private final AutomapNote note;
  private final Point location = new Point();

  private IconLayerItem item;


  public LayerObjectAutomap(AreResource parent, AutomapNote note)
  {
    super(ViewerConstants.RESOURCE_ARE, "Automap", AutomapNote.class, parent);
    this.note = note;
    init();
  }

  @Override
  public Viewable getViewable()
  {
    return note;
  }

  @Override
  public Viewable[] getViewables()
  {
    return new Viewable[]{note};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
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
  public void reload()
  {
    init();
  }

  @Override
  public void update(double zoomFactor)
  {
    if (item != null) {
      item.setItemLocation((int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
    }
  }

  @Override
  public Point getMapLocation()
  {
    return location;
  }

  @Override
  public Point[] getMapLocations()
  {
    return new Point[]{location};
  }

  private void init()
  {
    if (note != null) {
      String msg = "";
      try {
        location.x = ((DecNumber)note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_X)).getValue();
        location.y = ((DecNumber)note.getAttribute(AutomapNote.ARE_AUTOMAP_LOCATION_Y)).getValue();
        if (((Bitmap)note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT_LOCATION)).getValue() == 1) {
          // fetching string from dialog.tlk
          msg = ((StringRef)note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT)).toString();
        } else {
          // fetching string from talk override
          msg = "[user-defined]";
          try {
            int srcStrref = ((StringRef)note.getAttribute(AutomapNote.ARE_AUTOMAP_TEXT)).getValue();
            if (srcStrref > 0) {
              String path = getParentStructure().getResourceEntry().getActualPath().toString();
              path = path.replace(getParentStructure().getResourceEntry().getResourceName(), "");
              if (Profile.isEnhancedEdition()) {
                // processing new TOH structure
                Path tohFile = FileManager.resolve(path, "DEFAULT.TOH");
                if (Files.exists(tohFile)) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  TohResource toh = new TohResource(tohEntry);
                  SectionOffset so = (SectionOffset)toh.getAttribute(TohResource.TOH_OFFSET_ENTRIES);
                  SectionCount sc = (SectionCount)toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
                  if (so != null && sc != null && sc.getValue() > 0) {
                    for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
                      StrRefEntry2 strref = (StrRefEntry2)toh.getAttribute(curOfs, false);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute(StrRefEntry2.TOH_STRREF_OVERRIDDEN)).getValue();
                        if (v == srcStrref) {
                          int sofs = ((HexNumber)strref.getAttribute(StrRefEntry2.TOH_STRREF_OFFSET_STRING)).getValue();
                          StringEntry2 se = (StringEntry2)toh.getAttribute(so.getValue() + sofs, false);
                          if (se != null) {
                            msg = ((TextEdit)se.getAttribute(StringEntry2.TOH_STRING_TEXT)).toString();
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
                if (Files.exists(tohFile) && Files.exists(totFile)) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  FileResourceEntry totEntry = new FileResourceEntry(totFile);
                  TohResource toh = new TohResource(tohEntry);
                  TotResource tot = new TotResource(totEntry);
                  SectionCount sc = (SectionCount)toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
                  if (sc != null && sc.getValue() > 0) {
                    for (int i = 0, count = sc.getValue(), curOfs = 0x14; i < count; i++) {
                      StrRefEntry strref = (StrRefEntry)toh.getAttribute(curOfs, false);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute(StrRefEntry.TOH_STRREF_OVERRIDDEN)).getValue();
                        if (v == srcStrref) {
                          int sofs = ((HexNumber)strref.getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING)).getValue();
                          StringEntry se = (StringEntry)tot.getAttribute(sofs, false);
                          if (se != null) {
                            msg = ((TextEdit)se.getAttribute(StringEntry.TOT_STRING_TEXT)).toString();
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
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      // Using cached icons
      Image[] icon;
      String keyIcon = String.format("%1$s%2$s", SharedResourceCache.createKey(Icon[0]),
                                                 SharedResourceCache.createKey(Icon[1]));
      if (SharedResourceCache.contains(SharedResourceCache.Type.ICON, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.ICON, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon);
      } else {
        icon = Icon;
        SharedResourceCache.add(SharedResourceCache.Type.ICON, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      item = new IconLayerItem(location, note, msg, msg, icon[0], Center);
      item.setLabelEnabled(Settings.ShowLabelMapNotes);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
