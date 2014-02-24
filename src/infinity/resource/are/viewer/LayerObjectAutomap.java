// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import java.awt.Image;
import java.awt.Point;
import java.io.File;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.StringRef;
import infinity.datatype.TextEdit;
import infinity.gui.layeritem.AbstractLayerItem;
import infinity.gui.layeritem.IconLayerItem;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;
import infinity.resource.are.AutomapNote;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.to.StrRefEntry;
import infinity.resource.to.StrRefEntry2;
import infinity.resource.to.StringEntry;
import infinity.resource.to.StringEntry2;
import infinity.resource.to.TohResource;
import infinity.resource.to.TotResource;

/**
 * Handles specific layer type: ARE/Automap Note (except for PST)
 * @author argent77
 */
public class LayerObjectAutomap extends LayerObject
{
  private static final Image[] Icon = new Image[]{Icons.getImage("Automap.png"),
                                                  Icons.getImage("Automap_s.png")};
  private static Point Center = new Point(26, 26);

  private final AutomapNote note;
  private final Point location = new Point();

  private IconLayerItem item;


  public LayerObjectAutomap(AreResource parent, AutomapNote note)
  {
    super("Automap", AutomapNote.class, parent);
    this.note = note;
    init();
  }

  @Override
  public AbstractStruct getStructure()
  {
    return note;
  }

  @Override
  public AbstractStruct[] getStructures()
  {
    return new AbstractStruct[]{note};
  }

  @Override
  public AbstractLayerItem getLayerItem()
  {
    return item;
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
  public void update(Point mapOrigin, double zoomFactor)
  {
    if (item != null && mapOrigin != null) {
      item.setItemLocation(mapOrigin.x + (int)(location.x*zoomFactor + (zoomFactor / 2.0)),
                           mapOrigin.y + (int)(location.y*zoomFactor + (zoomFactor / 2.0)));
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
              if (ResourceFactory.getGameID() == ResourceFactory.ID_BGEE ||
                  ResourceFactory.getGameID() == ResourceFactory.ID_BG2EE) {
                // processing new TOH structure
                File tohFile = new File(path, "DEFAULT.TOH");
                if (tohFile.exists()) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  TohResource toh = new TohResource(tohEntry);
                  SectionCount sc = (SectionCount)toh.getAttribute("# strref entries");
                  if (sc != null && sc.getValue() > 0) {
                    for (int i = 0; i < sc.getValue(); i++) {
                      StrRefEntry2 strref = (StrRefEntry2)toh.getAttribute("StrRef entry " + i);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute("Overridden strref")).getValue();
                        if (v == srcStrref) {
                          StringEntry2 se = (StringEntry2)toh.getAttribute("String entry" + i);
                          if (se != null) {
                            TextEdit te = (TextEdit)se.getAttribute("Override string");
                            if (te != null) {
                              msg = te.toString();
                            }
                          }
                          break;
                        }
                      }
                    }
                  }
                }
              } else {
                // processing legacy TOH/TOT structures
                File tohFile = new File(path, "DEFAULT.TOH");
                File totFile = new File(path, "DEFAULT.TOT");
                if (tohFile.exists() && totFile.exists()) {
                  FileResourceEntry tohEntry = new FileResourceEntry(tohFile);
                  FileResourceEntry totEntry = new FileResourceEntry(totFile);
                  TohResource toh = new TohResource(tohEntry);
                  TotResource tot = new TotResource(totEntry);
                  SectionCount sc = (SectionCount)toh.getAttribute("# strref entries");
                  int totIndex = -1;
                  if (sc != null && sc.getValue() > 0) {
                    for (int i = 0; i < sc.getValue(); i++) {
                      StrRefEntry strref = (StrRefEntry)toh.getAttribute("StrRef entry " + i);
                      if (strref != null) {
                        int v = ((StringRef)strref.getAttribute("Overridden strref")).getValue();
                        if (v == srcStrref) {
                          totIndex = i;
                          break;
                        }
                      }
                    }
                    if (totIndex > 0) {
                      StringEntry se = (StringEntry)tot.getAttribute("String entry " + totIndex);
                      if (se != null) {
                        TextEdit te = (TextEdit)se.getAttribute("String data");
                        if (te != null) {
                          msg = te.toString();
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
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      item = new IconLayerItem(location, note, msg, Icon[0], Center);
      item.setName(getCategory());
      item.setToolTipText(msg);
      item.setImage(AbstractLayerItem.ItemState.HIGHLIGHTED, Icon[1]);
      item.setVisible(isVisible());
    }
  }
}
