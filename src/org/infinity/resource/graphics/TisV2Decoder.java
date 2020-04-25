// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

/**
 * Handles new PVRZ-based TIS resources.
 */
public class TisV2Decoder extends TisDecoder
{
  private static final int HeaderSize = 24;   // Size of the TIS header

  private ByteBuffer tisBuffer;
  private int tileCount, tileSize;
  private String pvrzNameBase;
  private BufferedImage workingCanvas;

  public TisV2Decoder(ResourceEntry tisEntry)
  {
    super(tisEntry);
    init();
  }

  /**
   * Returns the base filename of the PVRZ resource (without page suffix and extension).
   * @return Base filename of the PVRZ resource related to this TIS resource.
   */
  public String getPvrzFileBase()
  {
    return pvrzNameBase;
  }

  /**
   * Returns the page index of the PVRZ resource containing the graphics data of the specified tile.
   * @param tileIdx The tile index.
   * @return A page index that can be used to determine the PVRZ resource that contains the graphics
   *         data of the specified tile. Returns -1 on error.
   */
  public int getPvrzPage(int tileIdx)
  {
    int ofs = getTileOffset(tileIdx);
    if (ofs > 0) {
      return tisBuffer.getInt(ofs);
    } else {
      return -1;
    }
  }

  /**
   * Returns the full PVRZ resource filename that contains the graphics data of the specified tile.
   * @param tileIdx The tile index
   * @return Full PVRZ resource filename with page and extension. Returns an empty string on error.
   */
  public String getPvrzFileName(int tileIdx)
  {
    int page = getPvrzPage(tileIdx);
    if (page >= 0) {
      return String.format("%s%02d.PVRZ", getPvrzFileBase(), page);
    } else {
      return "";
    }
  }

  @Override
  public void close()
  {
    PvrDecoder.flushCache();
    tisBuffer = null;
    tileCount = 0;
    tileSize = 0;
    pvrzNameBase = "";
    if (workingCanvas != null) {
      workingCanvas.flush();
      workingCanvas = null;
    }
  }

  @Override
  public void reload()
  {
    init();
  }

  @Override
  public ByteBuffer getResourceBuffer()
  {
    return tisBuffer;
  }

  @Override
  public int getTileWidth()
  {
    return TileDimension;
  }

  @Override
  public int getTileHeight()
  {
    return TileDimension;
  }

  @Override
  public int getTileCount()
  {
    return tileCount;
  }

  @Override
  public Image getTile(int tileIdx)
  {
    BufferedImage image = ColorConvert.createCompatibleImage(TileDimension, TileDimension, true);
    renderTile(tileIdx, image);
    return image;
  }

  @Override
  public boolean getTile(int tileIdx, Image canvas)
  {
    return renderTile(tileIdx, canvas);
  }

  @Override
  public int[] getTileData(int tileIdx)
  {
    int[] buffer = new int[TileDimension*TileDimension];
    renderTile(tileIdx, buffer);
    return buffer;
  }

  @Override
  public boolean getTileData(int tileIdx, int[] buffer)
  {
    return renderTile(tileIdx, buffer);
  }


  private void init()
  {
    close();

    if (getResourceEntry() != null) {
      try {
        int[] info = getResourceEntry().getResourceInfo();
        if (info == null || info.length < 2) {
          throw new Exception("Error reading TIS header");
        }

        tileCount = info[0];
        if (tileCount <= 0) {
          throw new Exception("Invalid tile count: " + tileCount);
        }
        tileSize = info[1];
        if (tileSize != 12) {
          throw new Exception("Invalid tile size: " + tileSize);
        }
        tisBuffer = getResourceEntry().getResourceBuffer();

        String name = getResourceEntry().getResourceRef();
        pvrzNameBase = getResourceEntry().getResourceName().substring(0, 1) +
                       getResourceEntry().getResourceName().substring(2, name.length());

        setType(Type.PVRZ);

        workingCanvas = new BufferedImage(TileDimension, TileDimension, BufferedImage.TYPE_INT_ARGB);
      } catch (Exception e) {
        e.printStackTrace();
        close();
      }
    }
  }

  // Returns and caches the PVRZ resource of the specified page
  private PvrDecoder getPVR(int page)
  {
    try {
      String name = String.format("%s%02d.PVRZ", pvrzNameBase, page);
      ResourceEntry entry = ResourceFactory.getResourceEntry(name);
      if (entry != null) {
        return PvrDecoder.loadPvr(entry);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // Returns the start offset of the specified tile. Returns -1 on error.
  private int getTileOffset(int tileIdx)
  {
    if (tileIdx >= 0 && tileIdx < getTileCount()) {
      return HeaderSize + tileIdx*tileSize;
    } else {
      return -1;
    }
  }

  // Paints the specified tile onto the canvas
  private boolean renderTile(int tileIdx, Image canvas)
  {
    if (canvas != null && canvas.getWidth(null) >= TileDimension && canvas.getHeight(null) >= TileDimension) {
      if (updateWorkingCanvas(tileIdx)) {
        Graphics2D g = (Graphics2D)canvas.getGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.drawImage(workingCanvas, 0, 0, null);
        } finally {
          g.dispose();
          g = null;
        }
        return true;
      }
    }
    return false;
  }

  // Writes the specified tile data into the buffer
  private boolean renderTile(int tileIdx, int[] buffer)
  {
    int size = TileDimension*TileDimension;
    if (buffer != null && buffer.length >= size) {
      if (updateWorkingCanvas(tileIdx)) {
        int[] src = ((DataBufferInt)workingCanvas.getRaster().getDataBuffer()).getData();
        System.arraycopy(src, 0, buffer, 0, size);
        src = null;
        return true;
      }
    }
    return false;
  }

  // Draws the specified tile into the working canvas and returns the success state.
  private boolean updateWorkingCanvas(int tileIdx)
  {
    int ofs = getTileOffset(tileIdx);
    if (ofs > 0) {
      int page = tisBuffer.getInt(ofs);
      int x = tisBuffer.getInt(ofs+4);
      int y = tisBuffer.getInt(ofs+8);
      PvrDecoder decoder = getPVR(page);
      if (decoder != null || page == -1) {
        // removing old content
        try {
          if (page == -1) {
            Graphics2D g = workingCanvas.createGraphics();
            try {
              g.setColor(Color.BLACK);
              g.fillRect(0, 0, TileDimension, TileDimension);
            } finally {
              g.dispose();
              g = null;
            }
          } else {
            if (decoder != null) {
              // drawing new content
              decoder.decode(workingCanvas, x, y, TileDimension, TileDimension);
              decoder = null;
            }
          }
          return true;
        } catch (Exception e) {
          e.printStackTrace();
          decoder = null;
        }
      }
    }
    return false;
  }
}
