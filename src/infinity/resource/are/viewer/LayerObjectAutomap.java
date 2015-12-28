// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.io.File;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.HexNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextEdit;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.Profile;
import infinity.resource.Viewable;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.are.viewer.icon.ViewerIcons;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.to.StrRefEntry;
import infinity.resource.to.StrRefEntry2;
import infinity.resource.to.StringEntry;
import infinity.resource.to.StringEntry2;
import infinity.resource.to.TohResource;
import infinity.resource.to.TotResource;
import infinity.util.io.FileNI;

/**
 * Handles specific layer type: ARE/Automap Note (except for PST)
 * @author argent77
 */
public class LayerObjectAutomap extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage(ViewerIcons.class, "itm_Automap1.png"),
                                                  Icons.getImage(ViewerIcons.class, "itm_Automap2.png")};
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
        location.x = ((DecNumber)note.getAttribute("Coordinate: X")).getValue();
        location.y = ((DecNumber)note.getAttribute("Coordinate: Y")).getValue();
        if (((Bitmap)note.getAttribute("Text location")).getValue() == 1) {
          // fetching string from dialog.tlk
          msg = ((StringRef)note.getAttribute("Text")).toString();
        } else {
          // fetching string from talk override
          msg = "[user-defined]";
          try {
            int srcStrref = ((StringRef)note.getAttribute("Text")).getValue();
            if (srcStrref > 0) {
              String path = getParentStructure().getResourceEntry().getActualFile().toString();
              path = path.replace(getParentStructure().getResourceEntry().getResourceName(), "");
              if (Profile.isEnhancedEdition()) {
                // processing new TOH structure
                File tohFile = new FileNI(path, "DEFAULT.TOH");
                if (tohFile.exists()) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  TohResource toh = new TohResource(tohEntry);
                  SectionOffset so = (SectionOffset)toh.getAttribute("Strref entries offset");
                  SectionCount sc = (SectionCount)toh.getAttribute("# strref entries");
                  if (so != null && sc != null && sc.getValue() > 0) {
                    for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
                      StrRefEntry2 strref = (StrRefEntry2)toh.getAttribute(curOfs, false);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute("Overridden strref")).getValue();
                        if (v == srcStrref) {
                          int sofs = ((HexNumber)strref.getAttribute("Relative override string offset")).getValue();
                          StringEntry2 se = (StringEntry2)toh.getAttribute(so.getValue() + sofs, false);
                          if (se != null) {
                            msg = ((TextEdit)se.getAttribute("Override string")).toString();
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
                File tohFile = new FileNI(path, "DEFAULT.TOH");
                File totFile = new FileNI(path, "DEFAULT.TOT");
                if (tohFile.exists() && totFile.exists()) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  FileResourceEntry totEntry = new FileResourceEntry(totFile);
                  TohResource toh = new TohResource(tohEntry);
                  TotResource tot = new TotResource(totEntry);
                  SectionCount sc = (SectionCount)toh.getAttribute("# strref entries");
                  if (sc != null && sc.getValue() > 0) {
                    for (int i = 0, count = sc.getValue(), curOfs = 0x14; i < count; i++) {
                      StrRefEntry strref = (StrRefEntry)toh.getAttribute(curOfs, false);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute("Overridden strref")).getValue();
                        if (v == srcStrref) {
                          int sofs = ((HexNumber)strref.getAttribute("TOT string offset")).getValue();
                          StringEntry se = (StringEntry)tot.getAttribute(sofs, false);
                          if (se != null) {
                            msg = ((TextEdit)se.getAttribute("String data")).toString();
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
      if (SharedResourceCache.contains(SharedResourceCache.Type.Icon, keyIcon)) {
        icon = ((ResourceIcon)SharedResourceCache.get(SharedResourceCache.Type.Icon, keyIcon)).getData();
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon);
      } else {
        icon = Icon;
        SharedResourceCache.add(SharedResourceCache.Type.Icon, keyIcon, new ResourceIcon(keyIcon, icon));
      }

      item = new IconLayerItem(location, note, msg, icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, icon[1]);
      item.setVisible(isVisible());
    }
  }
}
