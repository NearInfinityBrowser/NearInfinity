// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are.viewer;

import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.RemovableDecNumber;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionOffset;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.are.AreResource;
import infinity.resource.are.viewer.AreaStructures.Structure;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.key.ResourceEntry;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.Tilemap;
import infinity.resource.wed.WedResource;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.JRadioButton;

/**
 * Manages tileset related functions.
 * @author argent77
 */
class AreaMap
{
  // identifies the respective WED resources
  public static enum DayNight { DAY, NIGHT }

  private static EnumMap<DayNight, String> DefaultMapName = new EnumMap<DayNight, String>(DayNight.class);
  static {
    DefaultMapName.put(DayNight.DAY, "Day");
    DefaultMapName.put(DayNight.NIGHT, "Night");
  }

  private final AreaViewer viewer;
  private final TisDecoder tisDecoder;
  private final List<TileInfo> currentTiles;
  private final List<Integer> currentDoorIndices;

  private DayNight currentMap;
  private WedResource currentWed;

  /**
   * Returns a specific WED resource from the specified ARE resource.
   * @param are The ARE resource containing  a link to the WED.
   * @param map The day or night WED resource.
   * @return The WED resource matching the specified parameters, or <code>null</code> if no WED available.
   */
  static WedResource getWedResource(AreResource are, DayNight map)
  {
    if (map != null && are != null) {
      ResourceRef wedRef = (ResourceRef)are.getAttribute("WED resource");
      if (wedRef != null) {
        String resName = wedRef.getResourceName();
        if (map == DayNight.NIGHT) {
          if (hasExtendedNight(are)) {
            if (resName.lastIndexOf('.') > 0) {
              String resExt = resName.substring(resName.lastIndexOf('.'));
              resName = resName.substring(0, resName.lastIndexOf('.')) + "N" + resExt;
            } else {
              resName += "N.WED";
            }
          } else {
            return null;
          }
        }
        ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(resName);
        if (entry != null) {
          try {
            return new WedResource(entry);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
    return null;
  }

  /**
   * Indicates whether the specified map supports a separate 'night' tileset.
   * @param are The map to check.
   * @return <code>true</code> if the map has a separate 'night' tileset.
   */
  static boolean hasExtendedNight(AreResource are)
  {
    if (are != null) {
      Flag flags = (Flag)are.getAttribute("Location");
      if (flags != null) {
        return flags.isFlagSet(6);
      }
    }
    return false;
  }

  /**
   * Creates a new JRadioButton component and initializes it with the data specified as parameters.
   * @param map Needed to set the proper text for the radio button.
   * @param are Needed to determine extended night WED support.
   * @param listener An optional ActionListener object that will be added to the radio button.
   * @return The radio button object.
   */
  static JRadioButton createRadioButton(DayNight map, AreResource are, ActionListener listener)
  {
    JRadioButton rb = null;
    if (map != null && are != null) {
      rb = new JRadioButton();
      rb.setText(DefaultMapName.get(map));
      rb.setEnabled(map == DayNight.DAY ||
                    (map == DayNight.NIGHT && hasExtendedNight(are)));
      if (listener != null) {
        rb.addActionListener(listener);
      }
    }
    return rb;
  }

  /**
   * Initializes a new AreaMap object.
   * @param viewer
   */
  AreaMap(AreaViewer viewer)
  {
    this.viewer = viewer;
    this.tisDecoder = new TisDecoder();
    this.currentTiles = new ArrayList<TileInfo>();
    this.currentDoorIndices = new ArrayList<Integer>();
  }

  /**
   * Closes the current map and frees temporary data associated with the AreaMap object.
   */
  void close()
  {
    tisDecoder.close();
    removeMap();
  }

  /**
   * Returns the currently active WED resource.
   * @return The currently active WED resource.
   */
  WedResource getCurrentWed()
  {
    return currentWed;
  }

  /**
   * Returns the currently loaded WED map.
   * @return
   */
  DayNight getCurrentMap()
  {
    return currentMap;
  }

  /**
   * Loads a new map.
   * @param map The map to load.
   * @return <code>true</code> if a new map has been loaded, <code>false</code> otherwise.
   */
  boolean setCurrentMap(DayNight map)
  {
    if (map != null && map != currentMap) {
      if (initMap(map)) {
        Dimension mapTilesDim = getMapSize();
        Dimension mapDim = new Dimension(mapTilesDim.width*tisDecoder.info().tileWidth(),
                                         mapTilesDim.height*tisDecoder.info().tileHeight());
        BufferedImage img = ColorConvert.toBufferedImage(viewer.getCanvas().getImage(), false);
        if (img == null || img.getWidth() != mapDim.width || img.getHeight() != mapDim.height) {
          img = ColorConvert.createCompatibleImage(mapDim.width, mapDim.height, false);
          viewer.getCanvas().setSize(mapDim);
          viewer.getCanvas().setImage(img);
        } else {
          img.flush();
        }

        // drawing map tiles
        drawTiles(img, tisDecoder, mapTilesDim.width, mapTilesDim.height,
                  currentTiles);

        // drawing opened/closed door tiles
        drawDoorTiles(img, tisDecoder, mapTilesDim.width, mapTilesDim.height,
                      currentTiles, currentDoorIndices,
                      viewer.isDoorStateClosed());

        img = null;
        viewer.getCanvas().repaint();
        return true;
      }
    }
    return false;
  }

  /**
   * Draws the doors onto the current map image in the specified opened/closed state.
   * @param drawClosed The door state to draw.
   * @return Returns whether the action has been processed successfully.
   */
  boolean setDoorState(boolean drawClosed)
  {
    try {
      Dimension mapTilesDim = getMapSize();
      Dimension mapDim = new Dimension(mapTilesDim.width*tisDecoder.info().tileWidth(),
                                       mapTilesDim.height*tisDecoder.info().tileHeight());

      BufferedImage img = ColorConvert.toBufferedImage(viewer.getCanvas().getImage(), false);
      if (img != null) {
        if (img.getWidth() >= mapDim.width && img.getHeight() >= mapDim.height) {
          // drawing opened/closed door tiles
          drawDoorTiles(img, tisDecoder, mapTilesDim.width, mapTilesDim.height,
                        currentTiles, currentDoorIndices,
                        drawClosed);

          img = null;
          viewer.getCanvas().repaint();
          return true;
        }
        img = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  // initialize tilesets
  private boolean initMap(DayNight map)
  {
    if (map != null) {
      WedResource wed = getWedResource(viewer.getAre(), map);
      if (wed == null)
        return false;

      removeMap();
      currentMap = map;
      currentWed = wed;
      viewer.getAreaStructures().initWed(getWedResource(viewer.getAre(), map));
      setTisDecoder(currentMap);

      AreaStructures as = viewer.getAreaStructures();
      Overlay ovl = (Overlay)as.getEntryByIndex(Structure.WED, Structure.OVERLAY, 0);
      ResourceRef tisRef = (ResourceRef)ovl.getAttribute("Tileset");
      ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
      if (tisEntry == null)
        return false;

      Dimension mapDim = getMapSize();
      int tileCount = mapDim.width*mapDim.height;

      int tileMapOfs = ((SectionOffset)ovl.getAttribute("Tilemap offset")).getValue();
      int tileMapBase = as.getIndexByOffset(Structure.WED, Structure.TILEMAP, tileMapOfs);
      if (tileMapBase < 0)
        return false;
      List<StructEntry> listTileMap = as.getStructureList(Structure.WED, Structure.TILEMAP);

      int tileMapIndexOfs = ((SectionOffset)ovl.getAttribute("Tilemap lookup offset")).getValue();
      int tileMapIndexBase = as.getIndexByOffset(Structure.WED, Structure.TILEINDEX, tileMapIndexOfs);
      if (tileMapIndexBase < 0)
        return false;
      List<StructEntry> listTileMapIndex = as.getStructureList(Structure.WED, Structure.TILEINDEX);

      // loading tile indices
      for (int idx = 0; idx < tileCount; idx++) {
        int mask, tileIdx, tileIdxAlt;
        try {
          Tilemap tm = (Tilemap)listTileMap.get(tileMapBase + idx);
          int pti = ((DecNumber)tm.getAttribute("Primary tile index")).getValue();
          int sti = ((DecNumber)tm.getAttribute("Secondary tile index")).getValue();
          Flag f = (Flag)tm.getAttribute("Draw Overlays");
          mask = 0;
          for (int i = 0; i < 8; i++) {
            mask |= f.isFlagSet(i) ? (1 << i) : 0;
          }
          tileIdx = ((DecNumber)listTileMapIndex.get(tileMapIndexBase + pti)).getValue();
          tileIdxAlt = sti;
        } catch (Exception e) {
          tileIdx = idx;
          tileIdxAlt = -1;
          mask = 0;
        }
        TileInfo info = new TileInfo(idx % mapDim.width, idx / mapDim.width, tileIdx, tileIdxAlt, mask);
        currentTiles.add(info);
      }

      // loading door tile indices
      for (final StructEntry doorTileEntry: as.getStructureList(Structure.WED, Structure.DOORTILE)) {
        currentDoorIndices.add(((RemovableDecNumber)doorTileEntry).getValue());
      }

      return true;
    }
    return false;
  }

  // properly closes and removes old Wed resource
  private void removeMap()
  {
    if (currentWed != null) {
      try {
        currentWed.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
      currentWed = null;
    }
    currentTiles.clear();
    currentDoorIndices.clear();
  }

  private ResourceEntry getTisResource(DayNight map)
  {
    if (map != null) {
      AreaStructures as = viewer.getAreaStructures();
      Overlay ovl = (Overlay)as.getEntryByIndex(Structure.WED, Structure.OVERLAY, 0);
      if (ovl != null) {
        ResourceRef tisRef = (ResourceRef)ovl.getAttribute("Tileset");
        if (tisRef != null) {
          return ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
        }
      }
    }
    return null;
  }

  // re-initializes the TIS decoder object with the TIS associated with the Day/Night WED
  private void setTisDecoder(DayNight map)
  {
    try {
      tisDecoder.open(getTisResource(map));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Returns the width and height of the specified map in tiles
  private Dimension getMapSize()
  {
    Dimension d = new Dimension();
    if (currentWed != null) {
      AreaStructures as = viewer.getAreaStructures();
      Overlay ovl = (Overlay)as.getEntryByIndex(Structure.WED, Structure.OVERLAY, 0);
      if (ovl != null) {
        d.width = ((DecNumber)ovl.getAttribute("Width")).getValue();
        d.height = ((DecNumber)ovl.getAttribute("Height")).getValue();
      }
    }
    return d;
  }


  /**
   * Draws a list of map tiles into the specified image object.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo A list of info objects needed to draw the right tiles
   * @return true if successful, false otherwise
   */
  private static boolean drawTiles(BufferedImage image, TisDecoder decoder,
                                   int tilesX, int tilesY, List<TileInfo> tileInfo)
  {
    if (image != null && decoder != null && tileInfo != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final TileInfo tile: tileInfo) {
          try {
            if (decoder.decodeTile(imgTile, tile.tilenum)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tile.tilenum);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }

  /**
   * Draws a specific list of primary or secondary tiles, depending on the specified opened/closed state.
   * @param image The image to draw the tiles into
   * @param decoder The TIS decoder needed to decode the tiles
   * @param tilesX Number of tiles per row
   * @param tilesY Number of tile rows
   * @param tileInfo List of info objects needed to draw the right tiles
   * @param doorIndices List of info objects of specific door tiles
   * @param drawClosed Indicates whether the primary or secondary tile has to be drawn
   * @return true if successful, false otherwise
   */
  private static boolean drawDoorTiles(BufferedImage image, TisDecoder decoder,
                                       int tilesX, int tilesY, List<TileInfo> tileInfo,
                                       List<Integer> doorIndices, boolean drawClosed)
  {
    if (image != null && decoder != null && tileInfo != null && doorIndices != null) {
      int tileWidth = decoder.info().tileWidth();
      int tileHeight = decoder.info().tileHeight();
      int width = tilesX * tileWidth;
      int height = tilesY * tileHeight;
      if (image.getWidth() >= width && image.getHeight() >= height) {
        final BufferedImage imgTile = ColorConvert.createCompatibleImage(tileWidth, tileHeight, false);
        final Graphics2D g = (Graphics2D)image.getGraphics();
        for (final int index: doorIndices) {
          // searching for correct tileinfo object
          TileInfo tile = tileInfo.get(index);
          if (tile.tilenum != index) {
            for (TileInfo ti: tileInfo) {
              if (ti.tilenum == index) {
                tile = ti;
                break;
              }
            }
          }

          // decoding tile
          int tileIdx = (drawClosed && tile.tilenumAlt != -1) ? tile.tilenumAlt : tile.tilenum;
          try {
            if (decoder.decodeTile(imgTile, tileIdx)) {
              g.drawImage(imgTile, tile.xpos*tileWidth, tile.ypos*tileHeight, null);
            }
          } catch (Exception e) {
            System.err.println("Error drawing tile #" + tileIdx);
          }
        }
        g.dispose();
        return true;
      }
    }
    return false;
  }

//----------------------------- INNER CLASSES -----------------------------

  // Stores information about a single TIS tile
  public static final class TileInfo
  {
    public final int xpos, ypos;      // coordinate in tile grid
    public final int tilenum;         // primary tile index from WED
    public final int tilenumAlt;      // secondary tile index from WED
    public final int overlayIndex;    // index of an additional overlay to address

    public TileInfo(int xpos, int ypos, int tilenum)
    {
      this(xpos, ypos, tilenum, -1, 0);
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt)
    {
      this(xpos, ypos, tilenum, tilenumAlt, 0);
    }

    public TileInfo(int xpos, int ypos, int tilenum, int tilenumAlt, int overlayMask)
    {
      this.xpos = xpos;
      this.ypos = ypos;
      this.tilenum = tilenum;
      this.tilenumAlt = tilenumAlt;

      if (overlayMask != 0) {
        int index = 0;
        for (; index < 8; index++) {
          if ((overlayMask & (1 << index)) != 0)
            break;
        }
        overlayIndex = index;
      } else {
        overlayIndex = 0;
      }
    }
  }
}
