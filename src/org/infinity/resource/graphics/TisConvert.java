// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.infinity.NearInfinity;
import org.infinity.datatype.IsNumeric;
import org.infinity.gui.converter.ConvertToPvrz;
import org.infinity.gui.converter.ConvertToTis;
import org.infinity.gui.converter.ConvertToTis.TileEntry;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.wed.Door;
import org.infinity.resource.wed.Overlay;
import org.infinity.resource.wed.Tilemap;
import org.infinity.resource.wed.WedResource;
import org.infinity.util.BinPack2D;
import org.infinity.util.DynamicArray;
import org.infinity.util.IntegerHashMap;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

/**
 * Utility class that provides methods for converting tileset (TIS) resources.
 */
public class TisConvert {
  /** Status definitions used by conversion routines. */
  public enum Status {
    /** Conversion completed successfully. */
    SUCCESS,
    /** Conversion was cancelled by the user. */
    CANCELLED,
    /** Conversion failed with an error. */
    ERROR,
    /** Conversion does not meet the requirements. */
    UNSUPPORTED
  }

  private static final Color TRANSPARENT_COLOR = new Color(0, true);

  /**
   * Composes an image from a list of tiles and exports it as PNG file.
   *
   * @param tiles List of tiles, ordered left-to-right, to-to-bottom.
   * @param tileCols Number of tiles per row.
   * @param pngFile {@link Path} of the PNG output file.
   * @return {@link Status} that indicates the completion state of the conversion.
   */
  public static Status exportPNG(List<Image> tiles, int tileCols, Path pngFile) {
    return exportPNG(tiles, tileCols, pngFile, false, null);
  }

  /**
   * Composes an image from a list of tiles and exports it as PNG file.
   *
   * @param tiles List of tiles, ordered left-to-right, to-to-bottom.
   * @param tileCols Number of tiles per row.
   * @param pngFile {@link Path} of the PNG output file.
   * @param showProgress Indicates whether a progress dialog is shown during the conversion process.
   * @param parent Parent component of the progress dialog.
   * @return {@link Status} that indicates the completion state of the conversion.
   */
  public static Status exportPNG(List<Image> tiles, int tileCols, Path pngFile, boolean showProgress, Component parent) {
    Status retVal = Status.ERROR;
    if (pngFile == null || tiles == null || tiles.isEmpty() || tileCols < 1) {
      return retVal;
    }

    int tileRows = (tiles.size() + tileCols - 1) / tileCols;

    if (showProgress && parent == null) {
      parent = NearInfinity.getInstance();
    }

    if (tileCols > 0 && tileRows > 0) {
      BufferedImage image = null;
      ProgressMonitor progress = null;
      if (showProgress) {
        progress = new ProgressMonitor(parent, "Exporting TIS to PNG...", "", 0, 2);
        progress.setMillisToDecideToPopup(0);
        progress.setMillisToPopup(0);
        progress.setProgress(0);
      }

      image = ColorConvert.createCompatibleImage(tileCols * 64, tileRows * 64, Transparency.BITMASK);
      Graphics2D g = image.createGraphics();
      for (int idx = 0; idx < tiles.size(); idx++) {
        if (tiles.get(idx) != null) {
          int tx = idx % tileCols;
          int ty = idx / tileCols;
          g.drawImage(tiles.get(idx), tx * 64, ty * 64, null);
        }
      }
      g.dispose();

      if (showProgress) {
        progress.setProgress(1);
      }

      try (OutputStream os = StreamUtils.getOutputStream(pngFile, true)) {
        if (ImageIO.write(image, "png", os)) {
          retVal = Status.SUCCESS;
        }
      } catch (IOException e) {
        retVal = Status.ERROR;
        e.printStackTrace();
      }

      if (showProgress && progress.isCanceled()) {
        retVal = Status.CANCELLED;
      }

      if (showProgress) {
        progress.close();
        progress = null;
      }
    }

    if (retVal != Status.SUCCESS && FileEx.create(pngFile).isFile()) {
      try {
        Files.delete(pngFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }


  /**
   * Converts a PVRZ-based tileset into the palette-based variant.
   *
   * @param tiles List of tiles, ordered left-to-right, to-to-bottom.
   * @param decoder {@link TisDecoder} instance of the source tileset.
   * @param tisFile {@link Path} of the palette-based TIS output file.
   * @return {@link Status} that indicates the completion state of the tileset conversion.
   */
  public static Status convertToPaletteTis(List<Image> tiles, TisDecoder decoder, Path tisFile) {
    return convertToPaletteTis(tiles, decoder, tisFile, false, null);
  }

  /**
   * Converts a PVRZ-based tileset into the palette-based variant.
   *
   * @param tiles List of tiles, ordered left-to-right, to-to-bottom.
   * @param decoder {@link TisDecoder} instance of the source tileset.
   * @param tisFile {@link Path} of the palette-based TIS output file.
   * @param showProgress Indicates whether a progress dialog is shown during the conversion process.
   * @param parent Parent component of the progress dialog.
   * @return {@link Status} that indicates the completion state of the tileset conversion.
   */
  public static Status convertToPaletteTis(List<Image> tiles, TisDecoder decoder, Path tisFile, boolean showProgress,
      Component parent) {
    Status retVal = Status.ERROR;
    if (tisFile == null || tiles == null || tiles.isEmpty() || decoder == null) {
      return retVal;
    }

    if (showProgress && parent == null) {
      parent = NearInfinity.getInstance();
    }

    String note = "Converting tile %d / %d";
    int progressIndex = 0, progressMax = decoder.getTileCount();
    ProgressMonitor progress = null;
    if (showProgress) {
      progress = new ProgressMonitor(parent, "Converting TIS...", String.format(note, progressIndex, progressMax),
          0, progressMax);
      progress.setMillisToDecideToPopup(500);
      progress.setMillisToPopup(2000);
    }

    try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tisFile))) {
      retVal = Status.SUCCESS;

      // writing header data
      byte[] header = new byte[24];
      System.arraycopy("TIS V1  ".getBytes(), 0, header, 0, 8);
      DynamicArray.putInt(header, 8, decoder.getTileCount());
      DynamicArray.putInt(header, 12, 0x1400);
      DynamicArray.putInt(header, 16, 0x18);
      DynamicArray.putInt(header, 20, 0x40);
      bos.write(header);

      // writing tile data
      int[] palette = new int[255];
      byte[] tilePalette = new byte[1024];
      byte[] tileData = new byte[64 * 64];
      BufferedImage image = ColorConvert.createCompatibleImage(decoder.getTileWidth(), decoder.getTileHeight(),
          Transparency.BITMASK);
      IntegerHashMap<Byte> colorCache = new IntegerHashMap<>(1800); // caching RGBColor -> index
      for (int tileIdx = 0; tileIdx < decoder.getTileCount(); tileIdx++) {
        colorCache.clear();
        if (showProgress && progress.isCanceled()) {
          retVal = Status.CANCELLED;
          break;
        }
        progressIndex++;
        if (showProgress && (progressIndex % 100) == 0) {
          progress.setProgress(progressIndex);
          progress.setNote(String.format(note, progressIndex, progressMax));
        }

        Graphics2D g = image.createGraphics();
        try {
          g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
          g.setColor(TRANSPARENT_COLOR);
          g.fillRect(0, 0, image.getWidth(), image.getHeight());
          g.drawImage(tiles.get(tileIdx), 0, 0, null);
        } finally {
          g.dispose();
          g = null;
        }

        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        if (ColorConvert.medianCut(pixels, 255, palette, true)) {
          // filling palette
          // first palette entry denotes transparency
          tilePalette[0] = tilePalette[2] = tilePalette[3] = 0;
          tilePalette[1] = (byte) 255;
          for (int i = 1; i < 256; i++) {
            tilePalette[(i << 2) + 0] = (byte) (palette[i - 1] & 0xff);
            tilePalette[(i << 2) + 1] = (byte) ((palette[i - 1] >>> 8) & 0xff);
            tilePalette[(i << 2) + 2] = (byte) ((palette[i - 1] >>> 16) & 0xff);
            tilePalette[(i << 2) + 3] = 0;
            colorCache.put(palette[i - 1], (byte) (i - 1));
          }
          // filling pixel data
          for (int i = 0; i < tileData.length; i++) {
            if ((pixels[i] & 0xff000000) == 0) {
              tileData[i] = 0;
            } else {
              Byte palIndex = colorCache.get(pixels[i]);
              if (palIndex != null) {
                tileData[i] = (byte) (palIndex + 1);
              } else {
                byte color = (byte) ColorConvert.getNearestColor(pixels[i], palette, 0.0, null);
                tileData[i] = (byte) (color + 1);
                colorCache.put(pixels[i], color);
              }
            }
          }
        } else {
          retVal = Status.ERROR;
          break;
        }
        bos.write(tilePalette);
        bos.write(tileData);
      }
      image.flush();
      image = null;
      tileData = null;
      tilePalette = null;
      palette = null;
    } catch (Exception e) {
      retVal = Status.ERROR;
      e.printStackTrace();
    } finally {
      if (showProgress) {
        progress.close();
        progress = null;
      }
    }
    if (retVal != Status.SUCCESS && FileEx.create(tisFile).isFile()) {
      try {
        Files.delete(tisFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }


  /**
   * Converts a palette-based tileset into the PVRZ-based variant.
   *
   * @param decoder {@link TisDecoder} instance of the source tileset.
   * @param tisFile {@link Path} of the palette-based TIS output file.
   * @return {@link Status} that indicates the completion state of the tileset conversion.
   */
  public static Status convertToPvrzTis(TisDecoder decoder, Path tisFile) {
    return convertToPvrzTis(decoder, tisFile, false, null);
  }

  /**
   * Converts a palette-based tileset into the PVRZ-based variant.
   *
   * @param decoder {@link TisDecoder} instance of the source tileset.
   * @param tisFile {@link Path} of the palette-based TIS output file.
   * @param showProgress Indicates whether a progress dialog is shown during the conversion process.
   * @param parent Parent component of the progress dialog.
   * @return {@link Status} that indicates the completion state of the tileset conversion.
   */
  public static Status convertToPvrzTis(TisDecoder decoder, Path tisFile, boolean showProgress, Component parent) {
    Status retVal = Status.ERROR;
    if (tisFile == null || decoder == null) {
      return retVal;
    }

    if (showProgress && parent == null) {
      parent = NearInfinity.getInstance();
    }

    try {
      ProgressMonitor progress = null;
      if (showProgress) {
        progress = new ProgressMonitor(parent, "Converting TIS...", "Preparing TIS", 0, 5);
        progress.setMillisToDecideToPopup(0);
        progress.setMillisToPopup(0);
      }

      // try to get associated WED resource
      int numTiles = decoder.getTileCount();
      String tisName = decoder.getResourceEntry().getResourceName().toUpperCase(Locale.ENGLISH);
      String wedName = tisName.replaceFirst("\\.TIS$", ".WED");
      WedResource wed = null;
      Overlay ovl = null;
      try {
        if (ResourceFactory.resourceExists(wedName)) {
          wed = new WedResource(ResourceFactory.getResourceEntry(wedName));
          if (wed != null) {
            ovl = (Overlay) wed.getAttribute(Overlay.WED_OVERLAY + " 0");
          }
        }
      } catch (Exception e) {
        wed = null;
        ovl = null;
        e.printStackTrace();
      }

      try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(tisFile))) {
        // writing header data
        byte[] header = new byte[24];
        System.arraycopy("TIS V1  ".getBytes(), 0, header, 0, 8);
        DynamicArray.putInt(header, 8, numTiles);
        DynamicArray.putInt(header, 12, 0x0c);
        DynamicArray.putInt(header, 16, 0x18);
        DynamicArray.putInt(header, 20, 0x40);
        bos.write(header);

        // processing tiles
        final BinPack2D.HeuristicRules binPackRule = BinPack2D.HeuristicRules.BOTTOM_LEFT_RULE;
        final int pageDim = 16; // 16 tiles a 64x64 pixels
        int tisWidth = 1;
        if (ovl != null) {
          tisWidth = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_WIDTH)).getValue();
        }
        int tisHeight = (numTiles + tisWidth - 1) / tisWidth;
        int numTilesPrimary = numTiles;
        if (ovl != null) {
          tisWidth = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_WIDTH)).getValue();
          tisHeight = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_HEIGHT)).getValue();
          numTilesPrimary = tisWidth * tisHeight;
        }
        boolean[] markedTiles = new boolean[numTiles];
        Arrays.fill(markedTiles, false);
        List<TileRect> listRegions = new ArrayList<>(256);

        // divide primary tiles into regions
        int pw = (tisWidth + pageDim - 1) / pageDim;
        int ph = (tisHeight + pageDim - 1) / pageDim;
        for (int py = 0; py < ph; py++) {
          int y = py * pageDim;
          int h = Math.min(pageDim, tisHeight - y);
          for (int px = 0; px < pw; px++) {
            int x = px * pageDim;
            int w = Math.min(pageDim, tisWidth - x);

            TileRect rect = new TileRect(x, y, w, h, tisWidth, numTiles, markedTiles);
            listRegions.add(rect);
          }
        }

        // defining additional regions from WED door structures
        if (wed != null) {
          int numDoors = ((IsNumeric) wed.getAttribute(WedResource.WED_NUM_DOORS)).getValue();
          for (int doorIdx = 0; doorIdx < numDoors; doorIdx++) {
            // for each door...
            Door door = (Door) wed.getAttribute(Door.WED_DOOR + " " + doorIdx);
            int numDoorTiles = ((IsNumeric) door.getAttribute(Door.WED_DOOR_NUM_TILEMAP_INDICES)).getValue();
            if (numDoorTiles > 0) {
              Point[] doorTiles = new Point[numDoorTiles];
              Arrays.fill(doorTiles, null);
              // getting actual tile indices
              for (int doorTileIdx = 0; doorTileIdx < numDoorTiles; doorTileIdx++) {
                // for each door tilemap...
                Point p = new Point(); // x=tilemap, y=tilemap index
                int doorTile = ((IsNumeric) door.getAttribute(Door.WED_DOOR_TILEMAP_INDEX + " " + doorTileIdx))
                    .getValue();
                p.x = doorTile;
                Tilemap tileMap = (Tilemap) ovl.getAttribute(Tilemap.WED_TILEMAP + " " + doorTile);
                // we need both primary and secondary tile index
                int index = ((IsNumeric) tileMap.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_SEC)).getValue();
                if (index > numTilesPrimary) {
                  // found already!
                  p.y = index;
                  doorTiles[doorTileIdx] = p;
                } else {
                  // processing another redirection for getting the primary tile index
                  index = ((IsNumeric) tileMap.getAttribute(Tilemap.WED_TILEMAP_TILE_INDEX_PRI)).getValue();
                  if (index >= 0 && index < numTilesPrimary) {
                    index = ((IsNumeric) ovl.getAttribute(Overlay.WED_OVERLAY_TILEMAP_INDEX + " " + index))
                        .getValue();
                    if (index > numTilesPrimary) {
                      // found!
                      p.y = index;
                      doorTiles[doorTileIdx] = p;
                    }
                  }
                }
              }

              int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE;
              int top = Integer.MAX_VALUE, bottom = Integer.MIN_VALUE;
              boolean initialized = false;
              for (Point p : doorTiles) {
                if (p != null) {
                  initialized = true;
                  left = Math.min(p.x % tisWidth, left);
                  right = Math.max(p.x % tisWidth, right);
                  top = Math.min(p.x / tisWidth, top);
                  bottom = Math.max(p.x / tisWidth, bottom);
                }
              }
              if (initialized) {
                // divide into regions in case door tile size exceeds max. texture size
                int doorWidth = right - left + 1;
                int doorHeight = bottom - top + 1;
                pw = (doorWidth + pageDim - 1) / pageDim;
                ph = (doorHeight + pageDim - 1) / pageDim;
                for (int py = 0; py < ph; py++) {
                  int y = py * pageDim;
                  int h = Math.min(pageDim, doorHeight - y);
                  for (int px = 0; px < pw; px++) {
                    int x = px * pageDim;
                    int w = Math.min(pageDim, doorWidth - x);

                    TileRect rect = new TileRect(w, h);
                    for (Point p : doorTiles) {
                      if (p != null) {
                        int dx = (p.x % tisWidth) - left;
                        int dy = (p.x / tisWidth) - top;
                        if (dx >= x && dx < x + w && dy >= y && dy < y + h && rect.setMarked(dx, dy, p.y)) {
                          markedTiles[p.y] = true;
                        }
                      }
                    }
                    listRegions.add(rect);
                  }
                }
              }
            }
          }

          // handling remaining unmarked tiles
          for (int idx = 0; idx < markedTiles.length; idx++) {
            if (!markedTiles[idx]) {
              TileRect rect = new TileRect(1, 1);
              rect.setMarked(0, 0, idx);
              listRegions.add(rect);
            }
          }
        }

        // packing tileset regions
        List<ConvertToTis.TileEntry> entryList = new ArrayList<>(numTiles);
        List<BinPack2D> pageList = new ArrayList<>();
        for (TileRect rect : listRegions) {
          Dimension space = new Dimension(rect.bounds);
          int pageIndex = -1;
          Rectangle rectMatch = null;
          for (int idx = 0; idx < pageList.size(); idx++) {
            BinPack2D packer = pageList.get(idx);
            rectMatch = packer.insert(space.width, space.height, binPackRule);
            if (rectMatch.height > 0) {
              pageIndex = idx;
              break;
            }
          }

          // create new page?
          if (pageIndex < 0) {
            BinPack2D packer = new BinPack2D(pageDim, pageDim);
            pageList.add(packer);
            pageIndex = pageList.size() - 1;
            rectMatch = packer.insert(space.width, space.height, binPackRule);
          }

          // registering tile entries
          for (int idx = 0; idx < rect.indices.length; idx++) {
            int x = rect.getX(idx);
            int y = rect.getY(idx);
            ConvertToTis.TileEntry entry;
            if (rect.indices[idx] >= 0) {
              entry = new ConvertToTis.TileEntry(rect.indices[idx], pageIndex, (rectMatch.x + x) * 64,
                  (rectMatch.y + y) * 64);
              entryList.add(entry);
            }
          }
        }

        // writing TIS entries
        Collections.sort(entryList, ConvertToTis.TileEntry.CompareByIndex);
        for (TileEntry entry : entryList) {
          bos.write(DynamicArray.convertInt(entry.page));
          bos.write(DynamicArray.convertInt(entry.x));
          bos.write(DynamicArray.convertInt(entry.y));
        }

        // generating PVRZ files
        retVal = writePvrzPages(decoder, tisFile, pageList, entryList, progress);
      } finally {
        if (progress != null) {
          progress.close();
          progress = null;
        }
      }
    } catch (Exception e) {
      retVal = Status.ERROR;
      e.printStackTrace();
    }
    if (retVal != Status.SUCCESS && FileEx.create(tisFile).isFile()) {
      try {
        Files.delete(tisFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Checks whether the given TIS filename can be used to generate PVRZ filenames from.
   *
   * @param fileName TIS filename.
   * @return {@code true} if the TIS filename is valid for PVRZ file generation, {@code false} otherwise.
   */
  public static boolean isTisFileNameValid(Path fileName) {
    if (fileName != null) {
      String name = fileName.getFileName().toString();
      int extOfs = name.lastIndexOf('.');
      if (extOfs >= 0) {
        name = name.substring(0, extOfs);
      }
      return Pattern.matches(".{2,7}", name);
    }
    return false;
  }

  /**
   * Attempts to fix the given filename to make it compatible with the naming scheme of PVRZ-based TIS files.
   *
   * @param fileName TIS filename.
   * @return TIS filename that resolves to a valid PVRZ filename scheme.
   */
  public static Path makeTisFileNameValid(Path fileName) {
    if (fileName != null && !isTisFileNameValid(fileName)) {
      Path path = fileName.getParent();
      String name = fileName.getFileName().toString();
      String ext = "";
      int extOfs = name.lastIndexOf('.');
      if (extOfs >= 0) {
        ext = name.substring(extOfs);
        name = name.substring(0, extOfs);
      }

      boolean isNight = (Character.toUpperCase(name.charAt(name.length() - 1)) == 'N');
      if (name.length() > 7) {
        int numDelete = name.length() - 7;
        int ofsDelete = name.length() - numDelete - (isNight ? 1 : 0);
        name = name.substring(ofsDelete, numDelete);
        return path.resolve(name);
      } else if (name.length() < 2) {
        String fmt, newName = null;
        int maxNum;
        switch (name.length()) {
          case 0:
            fmt = name + "%s02d";
            maxNum = 99;
            break;
          default:
            fmt = name + "%s01d";
            maxNum = 9;
            break;
        }
        for (int i = 0; i < maxNum; i++) {
          String s = String.format(fmt, i) + (isNight ? "N" : "") + ext;
          if (!ResourceFactory.resourceExists(s)) {
            newName = s;
            break;
          }
        }
        if (newName != null) {
          return path.resolve(newName);
        }
      }
    }
    return fileName;
  }

  // Generates PVRZ files based on the current TIS resource and the specified parameters
  private static Status writePvrzPages(TisDecoder decoder, Path tisFile, List<BinPack2D> pageList,
      List<ConvertToTis.TileEntry> entryList,
      ProgressMonitor progress) {
    Status retVal = Status.SUCCESS;
    DxtEncoder.DxtType dxtType = DxtEncoder.DxtType.DXT1;
    int dxtCode = 7; // PVR code for DXT1
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    String note = "Generating PVRZ file %s / %s";
    if (progress != null) {
      progress.setMaximum(pageList.size() + 1);
      progress.setProgress(1);
    }

    try {
      for (int pageIdx = 0; pageIdx < pageList.size(); pageIdx++) {
        if (progress != null) {
          if (progress.isCanceled()) {
            retVal = Status.CANCELLED;
            return retVal;
          }
          progress.setProgress(pageIdx + 1);
          progress.setNote(String.format(note, pageIdx + 1, pageList.size()));
        }

        Path pvrzFile = generatePvrzFileName(tisFile, pageIdx);
        BinPack2D packer = pageList.get(pageIdx);
        packer.shrinkBin(true);

        // generating texture image
        int w = packer.getBinWidth() * 64;
        int h = packer.getBinHeight() * 64;
        BufferedImage texture = ColorConvert.createCompatibleImage(w, h, true);
        Graphics2D g = texture.createGraphics();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        try {
          g.setBackground(new Color(0, true));
          g.setColor(new Color(0, true));
          g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
          for (final ConvertToTis.TileEntry entry : entryList) {
            if (entry.page == pageIdx) {
              Image tileImg = decoder.getTile(entry.tileIndex);
              int dx = entry.x, dy = entry.y;
              g.drawImage(tileImg, dx, dy, dx + 64, dy + 64, 0, 0, 64, 64, null);
            }
          }
        } finally {
          g.dispose();
          g = null;
        }

        int[] textureData = ((DataBufferInt) texture.getRaster().getDataBuffer()).getData();
        try {
          // compressing PVRZ
          int outSize = DxtEncoder.calcImageSize(texture.getWidth(), texture.getHeight(), dxtType);
          DxtEncoder.encodeImage(textureData, texture.getWidth(), texture.getHeight(), output, dxtType);
          byte[] header = ConvertToPvrz.createPVRHeader(texture.getWidth(), texture.getHeight(), dxtCode);
          byte[] pvrz = new byte[header.length + outSize];
          System.arraycopy(header, 0, pvrz, 0, header.length);
          System.arraycopy(output, 0, pvrz, header.length, outSize);
          header = null;
          pvrz = Compressor.compress(pvrz, 0, pvrz.length, true);

          // writing PVRZ to disk
          try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(pvrzFile))) {
            bos.write(pvrz);
          } catch (IOException e) {
            retVal = Status.ERROR;
            e.printStackTrace();
            return retVal;
          }
          pvrz = null;
        } catch (Exception e) {
          retVal = Status.ERROR;
          e.printStackTrace();
          return retVal;
        }
      }
    } finally {
      // cleaning up
      if (retVal != Status.SUCCESS) {
        for (int i = 0; i < pageList.size(); i++) {
          Path pvrzFile = generatePvrzFileName(tisFile, i);
          if (pvrzFile != null && FileEx.create(pvrzFile).isFile()) {
            try {
              Files.delete(pvrzFile);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
    return retVal;
  }

  // Generates PVRZ filename with full path from the given parameters
  private static Path generatePvrzFileName(Path tisFile, int page) {
    if (tisFile != null) {
      Path path = tisFile.getParent();
      String tisName = tisFile.getFileName().toString();
      int extOfs = tisName.lastIndexOf('.');
      if (extOfs > 0) {
        tisName = tisName.substring(0, extOfs);
      }
      if (Pattern.matches(".{2,7}", tisName)) {
        String pvrzName = String.format("%s%s%02d.PVRZ", tisName.substring(0, 1),
            tisName.substring(2, tisName.length()), page);
        return path.resolve(pvrzName);
      }
    }
    return null;
  }


  /**
   * Attempts to find and return a WED resource that references the given TIS resource.
   *
   * @param tisEntry   {@link ResourceEntry} instance of the TIS resource.
   * @param deepSearch Specify {@code false} to only check for a WED file of same resref as the given TIS
   *                     resource.Specify {@code true} to check all available WED files for a match.
   * @return {@link ResourceEntry} to the WED resource that references {@code tisEntry}. Returns {@code null} if not
   *         found.
   */
  public static ResourceEntry findWed(ResourceEntry tisEntry, boolean deepSearch) {
    if (tisEntry == null) {
      return null;
    }

    final Predicate<ResourceEntry> wedCheck = wedEntry -> {
      if (wedEntry != null) {
        try {
          final int[] resInfo = tisEntry.getResourceInfo();
          int numTiles = (resInfo != null && resInfo.length > 1) ? resInfo[0] : 0;
          if (numTiles > 0) {
            final ByteBuffer buf = wedEntry.getResourceBuffer().order(ByteOrder.LITTLE_ENDIAN);
            final String sig = StreamUtils.readString(buf, 0, 8);
            if ("WED V1.3".equals(sig) && buf.getInt(0x08) > 0) {
              final int ofsOvl = buf.getInt(0x10);
              final int w = buf.getShort(ofsOvl);
              final int h = buf.getShort(ofsOvl + 2);
              final String tisRef = StreamUtils.readString(buf, ofsOvl + 4, 8);
              return (numTiles >= w*h && tisRef != null && tisRef.equalsIgnoreCase(tisEntry.getResourceRef()));
            }
          }
        } catch (IOException e) {
          // do nothing
        } catch (Exception e) {
          System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
      }
      return false;
    };

    // trying WED of same name first
    ResourceEntry wedEntry = ResourceFactory.getResourceEntry(tisEntry.getResourceRef() + ".WED");
    boolean retVal = wedCheck.test(wedEntry);

    // searching WED
    if (!retVal && deepSearch) {
      final List<ResourceEntry> wedEntries = ResourceFactory.getResources("WED");
      wedEntry = wedEntries
          .parallelStream()
          .filter(wedCheck)
          .findAny()
          .orElse(null);
    }

    return wedEntry;
  }

  /**
   * Attempts to find and load the WED resource associated with the specified TIS resource.
   *
   * @param tisEntry   The TIS resource entry.
   * @param deepSearch Specify {@code false} to only check for a WED file of same resref as the given TIS
   *                     resource.Specify {@code true} to check all available WED files for a match.
   * @return {@code WedResource} instance if successful, {@code null} otherwise.
   */
  public static WedResource loadWedForTis(ResourceEntry tisEntry, boolean deepSearch) {
    WedResource wed = null;

    final ResourceEntry wedEntry = TisConvert.findWed(tisEntry, deepSearch);
    if (wedEntry != null) {
      try {
        wed = new WedResource(wedEntry);
      } catch (Exception e) {
      }
    }

    return wed;
  }

  /**
   * Attempts to calculate the TIS width from an associated WED file.
   *
   * @param entry      The TIS resource entry.
   * @param deepSearch Specify {@code false} to only check for a WED file of same resref as the given TIS
   *                     resource.Specify {@code true} to check all available WED files for a match.
   * @param tileCount  An optional tile count that will be used to "guess" the correct number of tiles per row if no
   *                     associated WED resource has been found.
   * @return The number of tiles per row for the specified TIS resource.
   */
  public static int calcTileWidth(ResourceEntry entry, boolean deepSearch, int tileCount) {
    // Try to fetch the correct width from an associated WED if available
    final ResourceEntry wedEntry = TisConvert.findWed(entry, deepSearch);
    if (wedEntry != null) {
      try {
        final ByteBuffer buf = wedEntry.getResourceBuffer().order(ByteOrder.LITTLE_ENDIAN);
        final int ofsOvl = buf.getInt(0x10);
        final int w = buf.getShort(ofsOvl);
        if (w > 0) {
          return w;
        }
      } catch (Exception e) {
      }
    }

    // If WED is not available: approximate the most commonly used aspect ratio found in TIS files
    // Disadvantage: does not take extra tiles into account
    return (tileCount < 9) ? tileCount : (int) (Math.sqrt(tileCount) * 1.18);
  }

  // -------------------------- INNER CLASSES --------------------------

  // Tracks regions of tiles used for the tile -> pvrz packing algorithm
  private static class TileRect {
    private Dimension bounds;
    private int[] indices;

    /** Creates an empty TileRect structure. */
    TileRect(int width, int height) {
      width = Math.max(1, width);
      height = Math.max(1, height);
      bounds = new Dimension(width, height);
      indices = new int[width * height];
      Arrays.fill(indices, -1);
    }

    /** Automatically fills the TileRect structure with valid tile indices. */
    TileRect(int left, int top, int width, int height, int rowLength, int numTiles, boolean[] markedTiles) {
      left = Math.max(0, left);
      top = Math.max(0, top);
      width = Math.max(1, width);
      height = Math.max(1, height);
      rowLength = Math.max(width, rowLength);
      bounds = new Dimension(width, height);
      indices = new int[width * height];
      for (int by = 0; by < height; by++) {
        int idx = by * width;
        int ofs = (top + by) * rowLength;
        for (int bx = 0; bx < width; bx++) {
          int tileIdx = ofs + left + bx;
          if (tileIdx < numTiles) {
            indices[idx + bx] = tileIdx;
            if (tileIdx < markedTiles.length) {
              markedTiles[tileIdx] = true;
            }
          } else {
            indices[idx + bx] = -1;
          }
        }
      }
    }

    /**
     * Sets the specified tile index in the TileRect structure. x and y specify a position within the TileRect
     * structure. tileIndex is the absolute tile index.
     */
    public boolean setMarked(int x, int y, int tileIndex) {
      tileIndex = Math.max(-1, tileIndex);
      if (x >= 0 && x < bounds.width && y >= 0 && y < bounds.height) {
        int index = y * bounds.width + x;
        if ((tileIndex != -1 && indices[index] == -1) || (tileIndex == -1 && indices[index] != -1)) {
          indices[index] = tileIndex;
          return true;
        }
      }
      return false;
    }

    public int getX(int index) {
      return (index >= 0 && index < bounds.width * bounds.height) ? index % bounds.width : -1;
    }

    public int getY(int index) {
      return (index >= 0 && index < bounds.width * bounds.height) ? index / bounds.width : -1;
    }
  }
}
