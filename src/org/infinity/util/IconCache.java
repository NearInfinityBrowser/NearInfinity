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
  public static Icon getDefaultIcon(int size) {
    if (size < MIN_SIZE) {
      return null;
    }

    return DEFAULT_ICONS.computeIfAbsent(size,
        k -> new ImageIcon(ColorConvert.createCompatibleImage(size, size, Transparency.BITMASK)));
  }

  /** Removes all icons associated with the specified BAM {@link ResourceEntry}. */
  public static void remove(ResourceEntry entry) {
    if (entry != null) {
      CACHE.remove(entry);
    }
  }

  /** Removes all entries from the cache. */
  public static void clearCache() {
    CACHE.clear();
    DEFAULT_ICONS.clear();
  }

  /** Returns the icon associated with the specified BAM {@link ResourceEntry} scaled to the specified size. */
  public static synchronized Icon get(ResourceEntry entry, int size) {
    if (size < MIN_SIZE) {
      return null;
    }

    final ResourceEntry bamEntry = ResourceFactory.getResourceIcon(entry);
    Icon retVal = getCachedIcon(bamEntry, size);

    if (bamEntry != null && retVal == null) {
      Image image = null;

      final BamDecoder decoder = BamDecoder.loadBam(bamEntry);
      if (decoder != null) {
        final BamControl control = decoder.createControl();

        // selecting suitable icon
        // PSTEE: cycle 0 contains the non-highlighted icon (which looks better)
        // other: cycle 1 contains the inventory icon in most cases
        int cycleIdx = (Profile.getGame() == Profile.Game.PSTEE) ? 0 : 1;
        cycleIdx = Math.min(cycleIdx, control.cycleCount() - 1);
        if (cycleIdx >= 0) {
          // getting first available frame from cycle
          for (int ci = cycleIdx; ci >= 0; ci--) {
            control.cycleSet(ci);
            if (control.cycleFrameCount() > 0) {
              image = control.cycleGetFrame(control.cycleFrameCount() - 1);
              ci = -1;
            }
          }
        } else if (decoder.frameCount() > 0) {
          // otherwise, try using frames directly
          for (int i = 0, count = decoder.frameCount(); i < count; i++) {
            final Image tmp = decoder.frameGet(control, 0);
            if (tmp.getWidth(null) >= MIN_SIZE && tmp.getHeight(null) >= MIN_SIZE) {
              image = tmp;
              i = count;
            }
          }
        }
      }

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
          g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
          g2.drawImage(image, x, y, dstWidth, dstHeight, null);
          g2.dispose();
          image = scaledImage;
        }

        retVal = new ImageIcon(image);
        setCachedIcon(bamEntry, size, retVal);
      }
    }

    if (retVal == null) {
      retVal = getDefaultIcon(size);
    }

    return retVal;
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
  private static void setCachedIcon(ResourceEntry entry, int size, Icon icon) {
    if (entry != null && size > 0 && icon != null) {
      CACHE.computeIfAbsent(entry, k -> new HashMap<>(8)).put(size, icon);
    }
  }
}
