// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.RenderCanvas;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.TisDecoder;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wed.Door;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.Tilemap;
import org.infinity.resource.wed.WedResource;

/**
 * Specialized renderer for drawing tileset-based graphics data.
 */
public class TilesetRenderer extends RenderCanvas
{
  public static final String[] LabelVisualStates = {"Day", "Twilight", "Night"};

  /** Available rendering modes for tiles. Affects how overlays are rendered. */
  public enum RenderMode {
    /** Determine rendering mode based on detected game engine. */
    Auto,
    /** Masked overlays supported by original BG1 engine. */
    Masked,
    /** Blended overlays supported by IWD, BG2 and EE engines. */
    Blended,
  }

  private static final int MaxOverlays = 8;   // max. supported overlay entries
  private static final double MinZoomFactor = 1.0/64.0;   // lower zoom factor limit
  private static final double MaxZoomFactor = 16.0;       // upper zoom factor limit

  // Placeholder for missing tile data
  private static final int[] DEFAULT_TILE_DATA = createDefaultTile();

  // Lighting adjustment for day/twilight/night times (multiplied by 10.24 for faster calculations)
  // Formula:
  // red   = (red   * LightingAdjustment[lighting][0]) >>> LightingAdjustmentShift;
  // green = (green * LightingAdjustment[lighting][1]) >>> LightingAdjustmentShift;
  // blue  = (blue  * LightingAdjustment[lighting][2]) >>> LightingAdjustmentShift;
  public static final int[][] LightingAdjustment = new int[][]
      // (100%, 100%, 100%),   (100%, 85%, 80%),      (45%, 45%, 85%)
      { {0x400, 0x400, 0x400}, {0x400, 0x366, 0x333}, {0x1cd, 0x1cd, 0x366} };
  public static final int LightingAdjustmentShift = 10;   // use in place of division

  // keeps track of registered listener objects
  private final List<TilesetChangeListener> listChangeListener = new ArrayList<>();
  // graphics data for all tiles of each overlay
  private final List<Tileset> listTilesets = new ArrayList<>(MaxOverlays);
  // array of tile indices used for closed door states for each door structure
  private final List<DoorInfo> listDoorTileIndices = new ArrayList<>();

  private final BufferedImage workingTile = ColorConvert.createCompatibleImage(64, 64, true); // internally used for drawing tile graphics
  private WedResource wed;                // current wed resource
  private int overlayTransparency;        // overlay transparency strength from 0 (opaque) to 255 (transparent)
  private RenderMode renderingMode = RenderMode.Auto; // the rendering mode to use for processing overlayed tiles
  private boolean overlaysEnabled = true; // indicates whether to draw overlays
  private boolean blendedOverlays;        // indicates whether to blend overlays with tile graphics
  private boolean hasChangedMap, hasChangedAppearance, hasChangedOverlays, hasChangedDoorState;
  private boolean isClosed = false;       // opened/closed state of door tiles
  private boolean showGrid = false;       // indicates whether to draw a grid on the tiles
  private boolean forcedInterpolation = false;  // indicates whether to use a pre-defined interpolation type or set one based on zoom factor
  private double zoomFactor = 1.0;        // zoom factor for drawing the map
  private int lighting = ViewerConstants.LIGHTING_DAY;    // the lighting condition to be used (day/twilight/night)
  private int miniMapType = ViewerConstants.MAP_NONE;     // the currently overlayed mini map (one of the MAP_XXX constants)
  private int miniMapAlpha = 128;                         // alpha transparency for overlayed mini maps
  private GraphicsResource miniMap = null;                     // the current mini map resource

  /**
   * Returns the number of supported lighting modes.
   */
  public static int getLightingModesCount()
  {
    return LabelVisualStates.length;
  }

  public TilesetRenderer(int overlayTransparency)
  {
    this(overlayTransparency, null);
  }

  public TilesetRenderer(int overlayTransparency, WedResource wed)
  {
    super();
    init(overlayTransparency, wed);
  }

  /**
   * Adds a ChangeListener to the component. A change event will be triggered on changing map dimensions
   * or setting up a new map.
   * @param listener The listener to add.
   */
  public void addChangeListener(TilesetChangeListener listener)
  {
    if (listener != null) {
      if (listChangeListener.indexOf(listener) < 0) {
        listChangeListener.add(listener);
      }
    }
  }

  /**
   * Returns an array of all the ChangeListeners added to this component.
   * @return All ChangeListeners added or an empty array.
   */
  public TilesetChangeListener[] getChangeListeners()
  {
    return (TilesetChangeListener[])listChangeListener.toArray();
  }

  /**
   * Removes a ChangeListener from the component.
   * @param listener The listener to remove.
   */
  public void removeChangeListener(TilesetChangeListener listener)
  {
    if (listener != null) {
      listChangeListener.remove(listener);
    }
  }

  /**
   * Initializes and displays the specified map. The current map will be discarded.
   * Triggers a change event.
   * @param wed WED resource structure used to construct a map.
   * @return true if map has been initialized successfully, false otherwise.
   */
  public boolean loadMap(int defaultTransparency, WedResource wed)
  {
    if (this.wed != wed) {
      return init(defaultTransparency, wed);
    } else {
      return true;
    }
  }

  /**
   * Returns whether a map has been loaded.
   */
  public boolean isMapLoaded()
  {
    return isInitialized();
  }

  /**
   * Returns the currently loaded WED resources.
   */
  public WedResource getWed()
  {
    return wed;
  }

  /**
   * Removes the current map and all associated data from memory.
   */
  public void dispose()
  {
    release(true);
    if (getImage() instanceof VolatileImage) {
      ((VolatileImage)getImage()).flush();
    }
  }

  /**
   * Returns the current mode for processing overlays.
   */
  public RenderMode getRenderingMode()
  {
    return renderingMode;
  }

  /**
   * Specify how to draw overlayed tiles. Possible choices are MODE_AUTO, MODE_BG1 and MODE_BG2.
   * @param mode The new rendering mode
   */
  public void setRenderingMode(RenderMode mode)
  {
    if (mode == null) mode = RenderMode.Auto;
    if (mode != renderingMode) {
      renderingMode = mode;
      hasChangedOverlays = true;
      updateDisplay();
    }
  }

  /**
   * Returns whether overlays are drawn.
   */
  public boolean isOverlaysEnabled()
  {
    return overlaysEnabled;
  }

  /**
   * Enable or disable the display of overlays.
   */
  public void setOverlaysEnabled(boolean enable)
  {
    if (overlaysEnabled != enable) {
      overlaysEnabled = enable;
      hasChangedOverlays = true;
      updateDisplay();
    }
  }

  /**
   * Returns whether the current map contains overlays
   */
  public boolean hasOverlays()
  {
    if (isInitialized()) {
      return !listTilesets.get(0).listOverlayTiles.isEmpty();
    }
    return false;
  }

  /**
   * Returns the current zoom factor.
   * @return The currently used zoom factor.
   */
  public double getZoomFactor()
  {
    return zoomFactor;
  }

  /**
   * Sets a new zoom factor for display. Clamped to range [0.25, 4.0].
   * Triggers a change event if the zoom factor changes.
   * @param factor The new zoom factor to use.
   */
  public void setZoomFactor(double factor)
  {
    if (factor < MinZoomFactor) factor = MinZoomFactor; else if (factor > MaxZoomFactor) factor = MaxZoomFactor;
    if (factor != zoomFactor) {
      zoomFactor = factor;
      hasChangedMap = true;
      updateDisplay();
    }
  }

  /**
   * Returns whether the renderer is forced to use the predefined interpolation type on scaling.
   */
  public boolean isForcedInterpolation()
  {
    return forcedInterpolation;
  }

  /**
   * Specifies whether the renderer uses the best interpolation type based on the current zoom factor
   * or uses a predefined interpolation type only.
   * @param set If {@code true}, uses a predefined interpolation type only.
   *            If {@code false}, chooses an interpolation type automatically.
   */
  public void setForcedInterpolation(boolean set)
  {
    if (set != forcedInterpolation) {
      forcedInterpolation = set;
      hasChangedAppearance = true;
      updateDisplay();
    }
  }

  /**
   * Returns the opened/closed state of door tiles.
   * @return The opened/closed state of door tiles.
   */
  public boolean isDoorsClosed()
  {
    return isClosed;
  }

  /**
   * Sets the opened/closed state of door tiles. Triggers a change event if the state changes.
   * @param isClosed The new opened/closed state of door tiles.
   */
  public void setDoorsClosed(boolean isClosed)
  {
    if (this.isClosed != isClosed) {
      this.isClosed = isClosed;
      hasChangedDoorState = true;
      updateDisplay();
    }
  }

  /**
   * Returns whether the current map contains closeable doors.
   */
  public boolean hasDoors()
  {
    if (isInitialized()) {
      return !listDoorTileIndices.isEmpty();
    }
    return false;
  }

  /**
   * Returns the currently used lighting condition.
   * @return The currently used lighting condition.
   */
  public int getLighting()
  {
    return lighting;
  }

  /**
   * Sets a new lighting condition to be used to draw the map. Only meaningful for day maps.
   * @param lighting The lighting condition to use. (One of the constants {@code LIGHTING_DAY},
   *                 {@code LIGHTING_DUSK} or {@code LIGHTING_NIGHT})
   */
  public void setLighting(int lighting)
  {
    if (lighting < ViewerConstants.LIGHTING_DAY) lighting = ViewerConstants.LIGHTING_DAY;
    else if (lighting > ViewerConstants.LIGHTING_NIGHT) lighting = ViewerConstants.LIGHTING_NIGHT;

    if (lighting != this.lighting) {
      this.lighting = lighting;
      hasChangedAppearance = true;
      updateDisplay();
    }
  }

  public boolean isGridEnabled()
  {
    return showGrid;
  }

  public void setGridEnabled(boolean enable)
  {
    if (enable != showGrid) {
      showGrid = enable;
      hasChangedAppearance = true;
      updateDisplay();
    }
  }

  /**
   * Returns the width of the current map in pixels. Zoom factor is not taken into account.
   * @return Map width in pixels.
   */
  public int getMapWidth(boolean scaled)
  {
    if (isInitialized()) {
      int w = listTilesets.get(0).tilesX * 64;
      if (scaled) {
        return (int)Math.ceil((double)w * zoomFactor);
      } else {
        return w;
      }
    } else {
      return 0;
    }
  }

  /**
   * Returns the height of the current map in pixels. Zoom factor is not taken into account.
   * @return Map height in pixels.
   */
  public int getMapHeight(boolean scaled)
  {
    if (isInitialized()) {
      int h = listTilesets.get(0).tilesY * 64;
      if (scaled) {
        return (int)Math.ceil((double)h * zoomFactor);
      } else {
        return h;
      }
    } else {
      return 0;
    }
  }

  /**
   * Advances the frame index by one for animated overlays.
   */
  public void advanceTileFrame()
  {
    for (int i = 1, size = listTilesets.size(); i < size; i++) {
      listTilesets.get(i).advanceTileFrame();
      hasChangedOverlays = true;
    }
    if (hasChangedOverlays) {
      updateDisplay();
    }
  }

  /**
   * Sets the frame index for animated overlay tiles.
   * @param index The frame index to set.
   */
  public void setTileFrame(int index)
  {
    for (int i = 1, size = listTilesets.size(); i < size; i++) {
      listTilesets.get(i).setTileFrame(index);
      hasChangedOverlays = true;
    }
    if (hasChangedOverlays) {
      updateDisplay();
    }
  }

  /**
   * Returns the type of the current mini map.
   * @return One of the MAP_XXX constants.
   */
  public int getMiniMapType()
  {
    return miniMapType;
  }

  /**
   * Returns the BmpResource instance of the current mini map.
   * @return BmpResource instance of the current mini map, or {@code null} if not available.
   */
  public GraphicsResource getMiniMap()
  {
    return miniMap;
  }

  /**
   * Specify a new mini map to be overlayed.
   * @param mapType The type of the mini map.
   * @param bmp The mini map resource.
   */
  public void setMiniMap(int mapType, GraphicsResource bmp)
  {
    if (mapType != miniMapType || bmp != miniMap) {
      switch (mapType) {
        case ViewerConstants.MAP_SEARCH:
        case ViewerConstants.MAP_HEIGHT:
        case ViewerConstants.MAP_LIGHT:
          miniMap = (bmp.getImage() != null) ? bmp : null;
          miniMapType = (miniMap != null) ? mapType : ViewerConstants.MAP_NONE;
          break;
        default:
          miniMap = null;
          miniMapType = ViewerConstants.MAP_NONE;
      }
      hasChangedAppearance = true;
      updateDisplay();
    }
  }

  /**
   * Returns the currently set transparency for overlayed mini maps.
   * @return The alpha transparency of mini maps. Range: [0..255]
   */
  public int getMiniMapTransparency()
  {
    return miniMapAlpha;
  }

  /**
   * Specify the alpha transparency for overlayed mini maps.
   * @param alpha Alpha transparency in range [0..255] for overlayed mini maps.
   */
  public void setMiniMapTransparency(int alpha)
  {
    alpha = Math.min(Math.max(alpha, 0), 255);
    if (miniMapAlpha != alpha) {
      miniMapAlpha = alpha;
      hasChangedAppearance = true;
      updateDisplay();
    }
  }

  /**
   * Redraw all tiles of the current map if needed.
   * @param force If {@code true}, the map will be redrawn regardless of the current map state.
   */
  public void reload(boolean force)
  {
    boolean b = false;
    if (getImage() != null && getImage() instanceof VolatileImage) {
      b = ((VolatileImage)getImage()).contentsLost();
    }
    updateDisplay(b || force);
  }

  @Override
  public void paint(Graphics g)
  {
    // checking whether VolatileImage instance needs to be updated
    if (getImage() != null && getImage() instanceof VolatileImage) {
      VolatileImage image = (VolatileImage)getImage();
      int valCode;
      do {
        valCode = image.validate(getGraphicsConfiguration());
        if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
          // recreate the image object
          int w = image.getWidth();
          int h = image.getHeight();
          image = createVolatileImage(w, h);
          setImage(image);
        }
        if (valCode != VolatileImage.IMAGE_OK) {
          updateDisplay(true);
        }
      } while (image.contentsLost());
    }
    super.paint(g);
  }

  protected void updateSize()
  {
    if (isInitialized()) {
      int w = getMapWidth(true);
      int h = getMapHeight(true);
      Dimension newDim = new Dimension(w, h);
      setScalingEnabled(zoomFactor != 1.0);
      if (!forcedInterpolation) {
        setInterpolationType((zoomFactor < 1.0) ? TYPE_BILINEAR : TYPE_NEAREST_NEIGHBOR);
      }
      setSize(newDim);
      setPreferredSize(newDim);
      setMinimumSize(newDim);
    } else {
      super.update();
    }
  }

  @Override
  protected void paintCanvas(Graphics g)
  {
    super.paintCanvas(g);
    if (showGrid) {
      double tileWidth = 64.0 * zoomFactor;
      double tileHeight = 64.0 * zoomFactor;
      double mapWidth = (double)getMapWidth(true);
      double mapHeight = (double)getMapHeight(true);
      g.setColor(Color.GRAY);
      for (double curY = 0.0; curY < mapHeight; curY += tileHeight) {
        for (double curX = 0.0; curX < mapWidth; curX += tileWidth) {
          g.drawLine((int)Math.ceil(curX), (int)Math.ceil(curY + tileHeight),
                     (int)Math.ceil(curX + tileWidth), (int)Math.ceil(curY + tileHeight));
          g.drawLine((int)Math.ceil(curX + tileWidth), (int)Math.ceil(curY),
                     (int)Math.ceil(curX + tileWidth), (int)Math.ceil(curY + tileHeight));
        }
      }
    }
  }

  private static int[] createDefaultTile()
  {
    int[] buffer = new int[64*64];
    BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setColor(Color.GRAY);
    g.fillRect(0, 0, image.getWidth(), image.getHeight());
    g.dispose();
    int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    for (int i = 0, cnt = Math.min(buffer.length, pixels.length); i < cnt; i++) {
      buffer[i] = pixels[i];
    }
    return buffer;
  }

  // Resizes the current image or creates a new one if needed
  private boolean updateImageSize()
  {
    if (isInitialized()) {
      if (getImage() == null || getImage().getWidth(null) != getMapWidth(false) ||
          getImage().getHeight(null) != getMapHeight(false)) {
        setImage(ColorConvert.createVolatileImage(getMapWidth(false), getMapHeight(false), false));
      }
      updateSize();
      return true;
    }
    return false;
  }

  // Initializes a new map
  private boolean init(int overlayTransparency, WedResource wed)
  {
    release(false);

    // resetting states
    blendedOverlays = Profile.getEngine() != Profile.Engine.BG1;
    lighting = ViewerConstants.LIGHTING_DAY;

    // loading map data
    this.overlayTransparency = overlayTransparency;
    if (wed != null) {
      if (initWed(wed)) {
        this.wed = wed;
        if (!updateImageSize()) {
          return false;
        }
      } else {
        return false;
      }
    }
    hasChangedMap = true;

    // drawing map data
    updateDisplay();

    return true;
  }

  // Removes all map-related data from memory
  private void release(boolean forceUpdate)
  {
    if (isInitialized()) {
      wed = null;
      listTilesets.clear();
      listDoorTileIndices.clear();

      Image img = getImage();
      if (img != null) {
        if (forceUpdate) {
          Graphics2D g = (Graphics2D)img.getGraphics();
          try {
            g.setBackground(new Color(0, true));
            g.clearRect(0, 0, img.getWidth(null), img.getHeight(null));
          } finally {
            g.dispose();
          }
          repaint();
        }
      }

      hasChangedMap = false;
      hasChangedAppearance = false;
      hasChangedOverlays = false;
      hasChangedDoorState = false;
    }
  }

  // Simply returns whether a map has been loaded
  private boolean isInitialized()
  {
    return (wed != null) && (!listTilesets.isEmpty());
  }

  private boolean initWed(WedResource wed)
  {
    if (wed != null) {
      // loading overlay structures
      IsNumeric so = (IsNumeric)wed.getAttribute(WedResource.WED_OFFSET_OVERLAYS);
      IsNumeric sc = (IsNumeric)wed.getAttribute(WedResource.WED_NUM_OVERLAYS);
      if (so != null && sc != null) {
        for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
          Overlay ovl = (Overlay)wed.getAttribute(curOfs, false);
          if (i == 0) {
            if (Profile.getGame() == Profile.Game.BG1EE || Profile.getGame() == Profile.Game.BG1SoD) {
              // updating overlay rendering mode (BG1-style or BG2-style)
              blendedOverlays &= (((IsNumeric)ovl.getAttribute(Overlay.WED_OVERLAY_MOVEMENT_TYPE)).getValue() == 2);
            }
          }
          if (ovl != null) {
            listTilesets.add(new Tileset(wed, ovl));
            curOfs += ovl.getSize();
          } else {
            release(true);
            return false;
          }
        }
      } else {
        release(true);
        return false;
      }

      // loading door structures
      so = (IsNumeric)wed.getAttribute(WedResource.WED_OFFSET_DOORS);
      sc = (IsNumeric)wed.getAttribute(WedResource.WED_NUM_DOORS);
      IsNumeric lookupOfs = (IsNumeric)wed.getAttribute(WedResource.WED_OFFSET_DOOR_TILEMAP_LOOKUP);
      if (so != null && sc != null && lookupOfs != null) {
        for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
          Door door = (Door)wed.getAttribute(curOfs, false);
          if (door != null) {
            String name = ((IsTextual)door.getAttribute(Door.WED_DOOR_NAME)).getText();
            boolean isClosed = ((IsNumeric)door.getAttribute(Door.WED_DOOR_IS_DOOR)).getValue() == 1;
            final int tileSize = 2;
            int tileIdx = ((IsNumeric)door.getAttribute(Door.WED_DOOR_TILEMAP_LOOKUP_INDEX)).getValue();
            int tileCount = ((IsNumeric)door.getAttribute(Door.WED_DOOR_NUM_TILEMAP_INDICES)).getValue();
            if (tileCount < 0) tileCount = 0;
            int[] indices = new int[tileCount];
            for (int j = 0; j < tileCount; j++) {
              indices[j] = ((IsNumeric)door.getAttribute(lookupOfs.getValue() + (tileIdx+j)*tileSize, false)).getValue();
            }
            listDoorTileIndices.add(new DoorInfo(name, isClosed, indices));
            curOfs += door.getSize();
          } else {
            listDoorTileIndices.add(new DoorInfo("", true, new int[]{}));   // needed as placeholder
          }
        }
      } else {
        release(true);
        return false;
      }

      return true;
    } else {
      return false;
    }
  }

  // For compatibility reasons only
  private void updateDisplay()
  {
    updateDisplay(false);
  }

  // (Re-)draw display, resize if needed
  private void updateDisplay(boolean forced)
  {
    if (isInitialized()) {
      updateImageSize();

      // VolatileImage objects may lose their content under specific circumstances
      if (!forced && getImage() != null && getImage() instanceof VolatileImage) {
        forced |= ((VolatileImage)getImage()).contentsLost();
      }

      if (hasChangedMap || hasChangedAppearance || forced) {
        // redraw each tile
        drawAllTiles();
      } else {
        if (hasChangedOverlays) {
          // redraw overlayed tiles only
          drawOverlayTiles();
        }
        if (hasChangedDoorState) {
          // redraw door tiles only
          drawDoorTiles();
        }
      }
      repaint();
      notifyChangeListeners();
      hasChangedMap = false;
      hasChangedAppearance = false;
      hasChangedOverlays = false;
      hasChangedDoorState = false;
    }
  }

  // Returns if the specified overlay index points to valid overlay data
  private boolean hasOverlay(int ovlIdx)
  {
    if (ovlIdx > 0 && ovlIdx < listTilesets.size()) {
      return !listTilesets.get(ovlIdx).listTiles.isEmpty();
    }
    return false;
  }

  // draws all tiles of the map
  private void drawAllTiles()
  {
    Tileset ts = listTilesets.get(0);
    for (int i = 0, size = ts.listTiles.size(); i < size; i++) {
      Tile tile = ts.listTiles.get(i);
      drawTile(tile, isDoorTile(tile));
    }
  }

  // draws overlayed tiles only
  private void drawOverlayTiles()
  {
    Tileset ts = listTilesets.get(0);
    for (int i = 0, size = ts.listOverlayTiles.size(); i < size; i++) {
      Tile tile = ts.listOverlayTiles.get(i);
      drawTile(tile, isDoorTile(tile));
    }
  }

  // draws door tiles only
  private void drawDoorTiles()
  {
    for (int i = 0, size = listDoorTileIndices.size(); i < size; i++) {
      DoorInfo di = listDoorTileIndices.get(i);
      for (int j = 0, iCount = di.getIndicesCount(); j < iCount; j++) {
        Tile tile = listTilesets.get(0).listTiles.get(di.getIndex(j));
        drawTile(tile, isDoorTile(tile));
      }
    }
  }

  // render tile graphics without overlays
  private void drawTileSimple(int[] sourceTile, int[] renderTarget)
  {
    if (sourceTile != null) {
      int pixel, fr, fg, fb;
      for (int ofs = 0; ofs < 4096; ofs++) {
        pixel = sourceTile[ofs];
        fr = (pixel >>> 16) & 0xff;
        fg = (pixel >>> 8) & 0xff;
        fb = pixel & 0xff;

        // applying lighting conditions
        fr = (fr * LightingAdjustment[lighting][0]) >>> LightingAdjustmentShift;
        fg = (fg * LightingAdjustment[lighting][1]) >>> LightingAdjustmentShift;
        fb = (fb * LightingAdjustment[lighting][2]) >>> LightingAdjustmentShift;
        renderTarget[ofs] = 0xff000000 | (fr << 16) | (fg << 8) | fb;
      }
    } else {
      // no tile = transparent pixel data (work-around for faulty tiles in BG1's WEDs)
      for (int ofs = 0; ofs < 4096; ofs++)
        renderTarget[ofs] = 0;
    }
  }

  // compose tile graphics in BG1 mode
  private void drawTileMasked(int[] primaryTile, int[] secondaryTile, int[] overlayTile, int[] renderTarget, boolean isDoorTile, boolean isDoorClosed)
  {
    if (renderTarget != null) {
      int[] src = (isDoorTile && isDoorClosed) ? secondaryTile : primaryTile;
      int fr, fg, fb, pixel;
      for (int ofs = 0; ofs < 4096; ofs++) {
        // composing pixel data
        if (src != null && (src[ofs] & 0xff000000) != 0)
          pixel = src[ofs];
        else if (overlayTile != null)
          pixel = overlayTile[ofs];
        else
          pixel = 0;
        fr = (pixel >>> 16) & 0xff;
        fg = (pixel >>> 8) & 0xff;
        fb = pixel & 0xff;

        // applying lighting conditions
        fr = (fr * LightingAdjustment[lighting][0]) >>> LightingAdjustmentShift;
        fg = (fg * LightingAdjustment[lighting][1]) >>> LightingAdjustmentShift;
        fb = (fb * LightingAdjustment[lighting][2]) >>> LightingAdjustmentShift;
        renderTarget[ofs] = 0xff000000 | (fr << 16) | (fg << 8) | fb;
      }
    }
  }

  // compose tile graphics in BG2 mode
  private void drawTileBlended(int[] primaryTile, int[] secondaryTile, int[] overlayTile, int[] renderTarget, boolean isPaletted)
  {
    if (renderTarget != null) {
      int pixel, fr, fg, fb;
      boolean pa = false, sa = false;
      int pr = 0, pg = 0, pb = 0, sr = 0, sg = 0, sb = 0, or = 0, og = 0, ob = 0;
      int alphaSrc = overlayTransparency, alphaDst = 255 - overlayTransparency;
      for (int ofs = 0; ofs < 4096; ofs++) {
        // getting source pixels
        if (primaryTile != null) {
          pixel = primaryTile[ofs];
          pa = (pixel & 0xff000000) != 0;
          pr = (pixel >>> 16) & 0xff;
          pg = (pixel >>> 8) & 0xff;
          pb = pixel & 0xff;
        }

        if (secondaryTile != null) {
          pixel = secondaryTile[ofs];
          sa = (pixel & 0xff000000) != 0;
          sr = (pixel >>> 16) & 0xff;
          sg = (pixel >>> 8) & 0xff;
          sb = pixel & 0xff;
        }

        if (overlayTile != null) {
          pixel = overlayTile[ofs];
          or = (pixel >>> 16) & 0xff;
          og = (pixel >>> 8) & 0xff;
          ob = pixel & 0xff;
        }

        // composing pixel data
        // blending modes depend on transparency states of primary and secondary pixels
        if (pa && !sa) {
          if (isPaletted) {
            fr = (pr * alphaSrc) + (or * alphaDst) >>> 8;
            fg = (pg * alphaSrc) + (og * alphaDst) >>> 8;
            fb = (pb * alphaSrc) + (ob * alphaDst) >>> 8;
          } else {
            if (secondaryTile != null) {
              fr = pr;
              fg = pg;
              fb = pb;
            } else {
              fr = (pr * alphaSrc) + (or * alphaDst) >>> 8;
              fg = (pg * alphaSrc) + (og * alphaDst) >>> 8;
              fb = (pb * alphaSrc) + (ob * alphaDst) >>> 8;
            }
          }
        } else if (pa && sa) {
          fr = (pr * alphaSrc) + (sr * alphaDst) >>> 8;
          fg = (pg * alphaSrc) + (sg * alphaDst) >>> 8;
          fb = (pb * alphaSrc) + (sb * alphaDst) >>> 8;
        } else if (!pa && !sa) {
          fr = or;
          fg = og;
          fb = ob;
        } else {  // !pa && sa
          fr = (sr * alphaSrc) + (or * alphaDst) >>> 8;
          fg = (sg * alphaSrc) + (og * alphaDst) >>> 8;
          fb = (sb * alphaSrc) + (ob * alphaDst) >>> 8;
        }

        // applying lighting conditions
        fr = (fr * LightingAdjustment[lighting][0]) >>> LightingAdjustmentShift;
        fg = (fg * LightingAdjustment[lighting][1]) >>> LightingAdjustmentShift;
        fb = (fb * LightingAdjustment[lighting][2]) >>> LightingAdjustmentShift;
        renderTarget[ofs] = 0xff000000 | (fr << 16) | (fg << 8) | fb;
      }
    }
  }

  // draws the specified tile into the target graphics buffer
  private synchronized void drawTile(Tile tile, boolean isDoorTile)
  {
    if (tile != null) {
      boolean isDoorClosed = (Profile.getEngine() == Profile.Engine.PST) ? !isClosed : isClosed;
      int[] target = ((DataBufferInt)workingTile.getRaster().getDataBuffer()).getData();

      if (overlaysEnabled && tile.hasOverlay() && hasOverlay(tile.getOverlayIndex())) {   // overlayed tile
        // preparing graphics data
        int overlay = tile.getOverlayIndex();
        if (overlay < listTilesets.size() && !listTilesets.get(overlay).listTiles.isEmpty()) {
          int tileIdx = listTilesets.get(overlay).listTiles.get(0).getPrimaryIndex();
          int[] srcOvl = null;
          if (tileIdx >= 0) {
            srcOvl = listTilesets.get(overlay).listTileData.get(tileIdx);
          }
          int[] srcPri = null;
          tileIdx = tile.getPrimaryIndex();
          if (tileIdx >= 0) {
            srcPri = listTilesets.get(0).listTileData.get(tileIdx);
          }
          int[] srcSec = null;
          tileIdx = tile.getSecondaryIndex();
          if (tileIdx >= 0) {
            if (tileIdx < listTilesets.get(0).listTileData.size()) {
              srcSec = listTilesets.get(0).listTileData.get(tileIdx);
            } else {
              System.err.println("Invalid tile index: " + tileIdx + " of " + listTilesets.get(0).listTileData.size());
            }
          }

          // determining correct rendering mode
          boolean blended = (renderingMode == RenderMode.Auto && blendedOverlays) || (renderingMode == RenderMode.Blended);

          // drawing tile graphics
          if (blended) {
            drawTileBlended(srcPri, srcSec, srcOvl, target, tile.isTisV1());
          } else {
            drawTileMasked(srcPri, srcSec, srcOvl, target, isDoorTile, isDoorClosed);
          }
          srcOvl = null;
          srcPri = null;
          srcSec = null;
        }
      } else {    // no overlay or disabled overlay
        // preparing tile graphics
        int[] srcTile = null;
        int tileIdx = (!isDoorClosed || !isDoorTile) ? tile.getPrimaryIndex() : tile.getSecondaryIndex();
        if (tileIdx < 0) { tileIdx = tile.getPrimaryIndex(); }    // XXX: hackish work-around for faulty tile definitions
        if (tileIdx >= 0 && tileIdx < listTilesets.get(0).listTileData.size()) {
          srcTile = listTilesets.get(0).listTileData.get(tileIdx);
        } else {
          // loading default tile
          srcTile = DEFAULT_TILE_DATA;
        }

        // drawing tile graphics
        drawTileSimple(srcTile, target);
        srcTile = null;
      }

      // drawing mini map if available
      if (miniMap != null && miniMapType != -1) {
        BufferedImage miniMapImage = miniMap.getImage();
        int miniMapWidth = miniMapImage.getWidth();
        int miniMapHeight = miniMapImage.getHeight();
        int[] map = ((DataBufferInt)miniMapImage.getRaster().getDataBuffer()).getData();

        double scaleX = (double)miniMapWidth / (double)getMapWidth(false);
        double scaleY = (double)miniMapHeight / (double)getMapHeight(false);
        double curX = (double)tile.getX() * scaleX;
        double nextX = Math.floor(curX) + 1.0;
        double curY = (double)tile.getY() * scaleY;
        double nextY = Math.floor(curY) + 1.0;
        int startPixelX = (int)Math.floor(curX);
        int curPixelX = startPixelX;
        int curPixelY = (int)Math.floor(curY);

        int srcAlpha = miniMapAlpha;
        int dstAlpha = 256 - srcAlpha;
        int dstOfs = 0;
        for (int y = 0; y < 64; y++) {
          curPixelX = startPixelX;
          int srcOfs = curPixelY*miniMapWidth + curPixelX;
          for (int x = 0; x < 64; x++) {
            // blending pixels
            int sr = (((map[srcOfs] >>> 16) & 0xff) * srcAlpha) >>> 8;
            int sg = (((map[srcOfs] >>> 8) & 0xff) * srcAlpha) >>> 8;
            int sb = ((map[srcOfs] & 0xff) * srcAlpha) >>> 8;
            int dr = (((target[dstOfs] >>> 16) & 0xff) * dstAlpha) >>> 8;
            int dg = (((target[dstOfs] >>> 8) & 0xff) * dstAlpha) >>> 8;
            int db = ((target[dstOfs] & 0xff) * dstAlpha) >>> 8;
            int color = ((sr + dr) << 16) | ((sg + dg) << 8) | (sb + db);
            target[dstOfs] = 0xff000000 | color;

            curX += scaleX;
            if (curX >= nextX) {
              nextX += 1.0;
              curPixelX++;
              srcOfs++;
            }
            dstOfs++;
          }
          curY += scaleY;
          if (curY >= nextY) {
            nextY += 1.0;
            curPixelY++;
          }
        }
      }

      // drawing tile on canvas
      if (getImage() != null) {
        Graphics2D g = (Graphics2D)getImage().getGraphics();
        g.drawImage(workingTile, tile.getX(), tile.getY(), null);
        g.dispose();
      }
      target = null;
    }
  }

  // Returns whether the specified tile is used as a door tile
  private boolean isDoorTile(Tile tile)
  {
    if (tile != null) {
      int tileIdx = tile.getPrimaryIndex();
      for (int i = 0, size = listDoorTileIndices.size(); i < size; i++) {
        DoorInfo di = listDoorTileIndices.get(i);
        for (int j = 0, iCount = di.getIndicesCount(); j < iCount; j++) {
          try {
            int idx = listTilesets.get(0).listTiles.get(di.getIndex(j)).getPrimaryIndex();
            if (idx == tileIdx) {
              return true;
            }
          } catch (Exception e) {
            // ignore invalid tile indices
          }
        }
      }
    }
    return false;
  }

  // Notify all registered change listeners
  private void notifyChangeListeners()
  {
    if (hasChangedMap || hasChangedAppearance || hasChangedOverlays || hasChangedDoorState) {
      for (int i = 0, size = listChangeListener.size(); i < size; i++) {
        listChangeListener.get(i).tilesetChanged(new TilesetChangeEvent(this,
            hasChangedMap, hasChangedAppearance, hasChangedOverlays, hasChangedDoorState));
      }
    }
  }


//----------------------------- INNER CLASSES -----------------------------

  // Stores data of a specific overlay structure
  private static class Tileset
  {
    // graphics data for all tiles of this overlay (as int arrays of 64*64 pixels)
    public final List<int[]> listTileData = new ArrayList<>();
    // info structures for all tiles of this overlay
    public final List<Tile> listTiles = new ArrayList<>();
    // lists references to all tiles containing overlays from listTiles
    public final List<Tile> listOverlayTiles = new ArrayList<>();

    public int tilesX, tilesY;    // stores number of tiles per row/column
    public boolean isTisPalette;  // whether tileset is palette-based

    public Tileset(WedResource wed, Overlay ovl)
    {
      init(wed, ovl);
    }

    public void advanceTileFrame()
    {
      for (int i = 0, size = listTiles.size(); i < size; i++) {
        listTiles.get(i).advancePrimaryIndex();
      }
    }

    public void setTileFrame(int index)
    {
      for (int i = 0, size = listTiles.size(); i < size; i++) {
        listTiles.get(i).setCurrentPrimaryIndex(index);
      }
    }

    private void init(WedResource wed, Overlay ovl)
    {
      if (wed != null && ovl != null) {
        // storing tile data
        isTisPalette = !Profile.isEnhancedEdition();    // choose sane default
        ResourceEntry tisEntry = getTisResource(wed, ovl);
        if (tisEntry != null) {
          try {
            TisDecoder decoder = TisDecoder.loadTis(tisEntry);
            isTisPalette = decoder.getType() == TisDecoder.Type.PALETTE;
            BufferedImage tileImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int i = 0, tCount = decoder.getTileCount(); i < tCount; i++) {
              decoder.getTile(i, tileImage);
              int[] srcData = ((DataBufferInt)tileImage.getRaster().getDataBuffer()).getData();
              int[] dstData = new int[64*64];
              System.arraycopy(srcData, 0, dstData, 0, 64*64);
              listTileData.add(dstData);
            }
            tileImage.flush();
            tileImage = null;
            decoder.close();
            decoder = null;
          } catch (Exception e) {
            e.printStackTrace();
            return;
          }
        }

        // storing tile information
        tilesX = ((IsNumeric)ovl.getAttribute(Overlay.WED_OVERLAY_WIDTH)).getValue();
        tilesY = ((IsNumeric)ovl.getAttribute(Overlay.WED_OVERLAY_HEIGHT)).getValue();
        int mapOfs = ((IsNumeric)ovl.getAttribute(Overlay.WED_OVERLAY_OFFSET_TILEMAP)).getValue();
        int idxOfs = ((IsNumeric)ovl.getAttribute(Overlay.WED_OVERLAY_OFFSET_TILEMAP_LOOKUP)).getValue();
        int tileCount = tilesX * tilesY;
        for (int i = 0, curOfs = mapOfs; i < tileCount; i++) {
          Tilemap tile = (Tilemap)ovl.getAttribute(curOfs, false);
          // tile coordinates in pixels
          int x = (i % tilesX) * 64;
          int y = (i / tilesX) * 64;

          if (tile != null) {
            // initializing list of primary tile indices
            final int idxSize = 2;
            int index = ((IsNumeric)tile.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_PRI)).getValue();
            int count = ((IsNumeric)tile.getAttribute(Tilemap.WED_TILEMAP_TILE_COUNT_PRI)).getValue();
            if (count < 0) count = 0;
            int[] tileIdx = new int[count];
            for (int j = 0; j < count; j++) {
              if (index >= 0) {
                IsNumeric dn = (IsNumeric)ovl.getAttribute(idxOfs + (index+j)*idxSize, false);
                if (dn != null) {
                  tileIdx[j] = dn.getValue();
                } else {
                  tileIdx[j] = -1;
                }
              } else {
                tileIdx[j] = -1;
              }
            }

            // initializing secondary tile index
            int tileIdx2 = ((IsNumeric)tile.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_SEC)).getValue();

            // initializing overlay flags
            IsNumeric drawOverlays = (IsNumeric)tile.getAttribute(Tilemap.WED_TILEMAP_DRAW_OVERLAYS);
            int flags = drawOverlays.getValue() & 255;

            listTiles.add(new Tile(x, y, count, tileIdx, tileIdx2, flags, isTisPalette));
            curOfs += tile.getSize();
          } else {
            listTiles.add(new Tile(x, y, 0, new int[]{}, -1, 0, true));     // needed as placeholder
          }
        }

        // grouping overlayed tiles for faster access
        for (int i = 0, size = listTiles.size(); i < size; i++) {
          Tile tile = listTiles.get(i);
          if (tile.getFlags() > 0) {
            listOverlayTiles.add(tile);
          }
        }

      } else {
        tilesX = tilesY = 0;
      }
    }

    // Returns the TIS file defined in the specified Overlay structure
    private ResourceEntry getTisResource(WedResource wed, Overlay ovl)
    {
      ResourceEntry entry = null;
      if (wed != null && ovl != null) {
        String tisName = ((ResourceRef)ovl.getAttribute(Overlay.WED_OVERLAY_TILESET)).getResourceName().toUpperCase(Locale.ENGLISH);
        if (tisName == null || "None".equalsIgnoreCase(tisName)) {
          tisName = "";
        }
        if (!tisName.isEmpty()) {
          // Special: BG1 has a weird way to select extended night tilesets
          if (Profile.getEngine() == Profile.Engine.BG1) {
            String wedName = wed.getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH);
            if (wedName.lastIndexOf('.') > 0) {
              wedName = wedName.substring(0, wedName.lastIndexOf('.'));
            }
            if (tisName.lastIndexOf('.') > 0) {
              tisName = tisName.substring(0, tisName.lastIndexOf('.'));
            }

            // XXX: not sure whether this check is correct
            if (wedName.length() > 6 && wedName.charAt(6) == 'N' && tisName.length() == 6) {
              entry = ResourceFactory.getResourceEntry(tisName + "N.TIS");
            }
            if (entry == null) {
              entry = ResourceFactory.getResourceEntry(tisName + ".TIS");
            }
          } else {
            entry = ResourceFactory.getResourceEntry(tisName);
          }
        }
      }
      return entry;
    }

  }


  // Stores tilemap information only (no graphics data)
  private static class Tile
  {
    private int tileIdx2;     // (start) indices of primary and secondary tiles
    private int[] tileIdx;    // tile indices for primary and secondary tiles
    private int tileCount, curTile;   // number of primary tiles, currently selected tile
    private int x, y, flags;          // (x, y) as pixel coordinates, flags defines overlay usage
    private boolean isTisV1;

    public Tile(int x, int y, int tileCount, int[] index, int index2, int flags, boolean isTisV1)
    {
      if (tileCount < 0) tileCount = 0;
      this.x = x;
      this.y = y;
      this.tileCount = tileCount;
      this.curTile = 0;
      this.tileIdx = index;
      this.tileIdx2 = index2;
      this.flags = flags;
      this.isTisV1 = isTisV1;
    }

    // Returns whether this tile references a TIS v1 resource
    public boolean isTisV1()
    {
      return isTisV1;
    }

    // Returns the current primary tile index
    public int getPrimaryIndex()
    {
      if (tileIdx.length > 0) {
        return tileIdx[curTile];
      } else {
        return -1;
      }
    }

//    // Returns the primary tile index of the specified frame (useful for animated tiles)
//    public int getPrimaryIndex(int frame)
//    {
//      if (tileCount > 0) {
//        return tileIdx[frame % tileCount];
//      } else {
//        return -1;
//      }
//    }

    // Sets a new selected primary tile index
    public void setCurrentPrimaryIndex(int frame)
    {
      if (tileCount > 0) {
        if (frame < 0) frame = 0; else if (frame >= tileCount) frame = tileCount - 1;
        curTile = frame;
      }
    }

//    // Returns the primary tile count
//    public int getPrimaryIndexCount()
//    {
//      return tileCount;
//    }

    // Advances the primary tile index by 1 for animated tiles, wraps around automatically
    public void advancePrimaryIndex()
    {
      if (tileCount > 0) {
        curTile = (curTile + 1) % tileCount;
      }
    }

    // Returns the secondary tile index (or -1 if not available)
    public int getSecondaryIndex()
    {
      return tileIdx2;
    }

    // Returns the x pixel coordinate of this tile
    public int getX()
    {
      return x;
    }

    // Returns y pixel coordinate of this tile
    public int getY()
    {
      return y;
    }

    // Returns the unprocessed flags data
    public int getFlags()
    {
      return flags;
    }

    // Returns true if overlays have been defined
    public boolean hasOverlay()
    {
      return flags != 0;
    }

    // Returns the overlay index (or 0 otherwise)
    public int getOverlayIndex()
    {
      if (flags != 0) {
        for (int i = 1; i < 8; i++) {
          if ((flags & (1 << i)) != 0) {
            return i;
          }
        }
      }
      return 0;
    }
  }


  // Stores relevant information about door structures
  private static class DoorInfo
  {
//    private String name;        // door info structure name
//    private boolean isClosed;   // indicates the door state for the specified list of tile indices
    private int[] indices;      // list of tilemap indices used for the door

    public DoorInfo(String name, boolean isClosed, int[] indices)
    {
//      this.name = (name != null) ? name : "";
//      this.isClosed = isClosed;
      this.indices = (indices != null) ? indices : new int[0];
    }

//    // Returns the name of the door structure
//    public String getName()
//    {
//      return name;
//    }

//    // Returns whether the tile indices are used for the closed state of the door
//    public boolean isClosed()
//    {
//      return isClosed;
//    }

    // Returns number of tiles used in this door structure
    public int getIndicesCount()
    {
      return indices.length;
    }

    // Returns tilemap index of specified entry
    public int getIndex(int entry)
    {
      if (entry >= 0 && entry < indices.length) {
        return indices[entry];
      } else {
        return -1;
      }
    }
  }
}
