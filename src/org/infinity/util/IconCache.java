// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.key.ResourceEntry;

/**
 * Cache for icons associated with ITM or SPL resources.
 *
 * Icons are preprocessed and optimized for display in lists or tables.
 */
public class IconCache {
  // Icon width and height cannot be smaller than this
  private static final int MIN_SIZE = 2;
  private static final int SIZE_LIST = 32;
  private static final int SIZE_TREE = 16;

  // Mappings for transparent default icons of various sizes
  private static final HashMap<Integer, Icon> DEFAULT_ICONS = new HashMap<>(8);

  // Maps icons of various sizes to a BAM ResourceEntry
  private static final HashMap<ResourceEntry, HashMap<Integer, Icon>> CACHE = new HashMap<>();

  /** Returns the default icon width and height used in resource selection lists. */
  public static int getDefaultListIconSize() {
    return SIZE_LIST;
  }

  /** Returns the default icon width and height used in the resource tree. */
  public static int getDefaultTreeIconSize() {
    return SIZE_TREE;
  }

  /**
   * Returns a transparent default icon of given dimension.
   * @param size Width and height of the icon, in pixels.
   * @return a transparent {@link Icon} of the given size. {@code null} if specified size is too small.
   */
  public static synchronized Icon getDefaultIcon(int size) {
    if (size < MIN_SIZE) {
      return null;
    }

    return DEFAULT_ICONS.computeIfAbsent(size,
        k -> new ImageIcon(ColorConvert.createCompatibleImage(size, size, Transparency.BITMASK)));
  }

  /** Removes all icons associated with the specified BAM {@link ResourceEntry}. */
  public static synchronized void remove(ResourceEntry entry) {
    if (entry != null) {
      CACHE.remove(entry);
    }
  }

  /** Removes all entries from the cache. */
  public static synchronized void clearCache() {
    CACHE.clear();
    DEFAULT_ICONS.clear();
  }

  /**
   * Returns the icon associated with the specified graphics {@link ResourceEntry} scaled to the specified size.
   *
   * @param entry {@link ResourceEntry} of a supported graphics resource. Currently supported: BAM, BMP.
   * @param size Width and height of the resulting icon, in pixels.
   * @return {@link Icon} from the specified graphics resource. Returns {@code null} if icon is not available.
   */
  public static synchronized Icon getIcon(ResourceEntry entry, int size) {
    if (size < MIN_SIZE) {
      return null;
    }

    Icon retVal = getCachedIcon(entry, size);
    if (entry != null && retVal == null) {
      Image image = null;

      if ("BAM".equalsIgnoreCase(entry.getExtension())) {
        // load suitable BAM frame
        image = getBamFrameImage(entry);
      } else if ("BMP".equalsIgnoreCase(entry.getExtension())) {
        // load BMP image
        image = getBmpImage(entry);
      }

      image = getScaledImage(image, size, true);
      if (image != null) {
        retVal = new ImageIcon(image);
        setCachedIcon(entry, size, retVal);
      }
    }

    if (retVal == null) {
      retVal = getDefaultIcon(size);
    }

    return retVal;
  }

  /**
   * Returns the icon associated with the specified {@link ResourceEntry} scaled to the specified size.
   *
   * @param entry {@link ResourceEntry} of a supported game resource. Currently supported: ITM, SPL, BAM, BMP.
   * @param size Width and height of the resulting icon, in pixels.
   * @return {@link Icon} associated with the specified game resource. Returns {@code null} if icon is not available.
   */
  public static synchronized Icon get(ResourceEntry entry, int size) {
    ResourceEntry graphicsEntry = null;

    if (entry != null) {
      final String ext = entry.getExtension().toUpperCase();
      if (ext.equals("ITM") || ext.equals("SPL")) {
        graphicsEntry = ResourceFactory.getResourceIcon(entry);
      } else if (ext.equals("BAM") || ext.equals("BMP")) {
        graphicsEntry = entry;
      }
    }

    return getIcon(graphicsEntry, size);
  }

  /** Returns the cached icon for the specified BAM {@link ResourceEntry} and {@code size}. */
  private static Icon getCachedIcon(ResourceEntry entry, int size) {
    Icon retVal = null;

    if (entry != null) {
      final HashMap<Integer, Icon> map = CACHE.get(entry);
      if (map != null) {
        retVal = map.get(size);
      }
    }

    return retVal;
  }

  /** Adds the given BAM {@link ResourceEntry} to the cache and associates it with the specified {@link Icon}. */
  private static synchronized void setCachedIcon(ResourceEntry entry, int size, Icon icon) {
    if (entry != null && size > 0 && icon != null) {
      CACHE.computeIfAbsent(entry, k -> new HashMap<>(8)).put(size, icon);
    }
  }

  /**
   * Returns a scaled version of the specified {@link Image} that fits into the given size.
   *
   * @param image {@link Image} to scale.
   * @param size Width and height of the returned image should not exceed this value.
   * @param quality A rendering hint for the scaled image. Specify {@code true} to apply bicubic interpolation for
   * sharper details, or {@code false} for a quicker bilinear interpolation.
   * @return The scaled image.
   */
  private static synchronized Image getScaledImage(Image image, int size, boolean quality) {
    Image retVal = image;

    if (image != null) {
      if (image.getHeight(null) != size) {
        final int dstWidth;
        final int dstHeight;
        if (image.getHeight(null) > size) {
          // preserve image aspect ratio
          dstWidth = (image.getWidth(null) >= image.getHeight(null)) ? size : image.getWidth(null) * size / image.getHeight(null);
          dstHeight = (image.getHeight(null) >= image.getWidth(null)) ? size : image.getHeight(null) * size / image.getWidth(null);
        } else {
          dstWidth = image.getWidth(null);
          dstHeight = image.getHeight(null);
        }
        int x = (size - dstWidth) / 2;
        int y = (size - dstHeight) / 2;
        final BufferedImage scaledImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = scaledImage.createGraphics();
        final Object interpolation = quality ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        g2.drawImage(image, x, y, dstWidth, dstHeight, null);
        g2.dispose();
        image = scaledImage;
      }

      retVal = image;
    }

    return retVal;
  }

  /** Returns a suitable (and unscaled) image for display. */
  private static synchronized Image getBamFrameImage(ResourceEntry bamEntry) {
    Image retVal = null;

    if (bamEntry != null) {
      final BamDecoder decoder = BamDecoder.loadBam(bamEntry);
      if (decoder != null) {
        final BamControl control = decoder.createControl();

        // selecting suitable frame
        // PSTEE: cycle 0 contains the non-highlighted icon (which looks better)
        // other: cycle 1 contains the inventory icon in most cases
        int cycleIdx = (Profile.getGame() == Profile.Game.PSTEE) ? 0 : 1;
        cycleIdx = Math.min(cycleIdx, control.cycleCount() - 1);
        if (cycleIdx >= 0) {
          // getting first available frame from cycle
          for (int ci = cycleIdx; ci >= 0; ci--) {
            control.cycleSet(ci);
            if (control.cycleFrameCount() > 0) {
              retVal = control.cycleGetFrame(control.cycleFrameCount() - 1);
              ci = -1;
            }
          }
        } else if (decoder.frameCount() > 0) {
          // otherwise, try using frames directly
          for (int i = 0, count = decoder.frameCount(); i < count; i++) {
            final Image tmp = decoder.frameGet(control, 0);
            if (tmp.getWidth(null) >= MIN_SIZE && tmp.getHeight(null) >= MIN_SIZE) {
              retVal = tmp;
              i = count;
            }
          }
        }
      }
    }

    return retVal;
  }

  /** Returns the specified BMP resource as {@link Image} object. */
  private static synchronized Image getBmpImage(ResourceEntry bmpEntry) {
    Image retVal = null;

    if (bmpEntry != null) {
      try {
        final GraphicsResource res = new GraphicsResource(bmpEntry);
        retVal = res.getImage();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }
}
