// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.tuples.Couple;

/**
 * Specialized cache for portrait icons.
 *
 * Icons are preprocessed and optimized for display in lists or tables.
 */
public class PortraitIconCache {
  // Maps icons of original and magnified size to a portrait icon index
  private static final HashMap<Integer, Couple<ImageIcon, ImageIcon>> CACHE = new HashMap<>();

  // Mappings for transparent default icons (original and scale size)
  private static final Couple<ImageIcon, ImageIcon> DEFAULT_ICONS = Couple.with(null, null);

  // Border size of transparent pixels around portrait icon
  private static final int ICON_BORDER = 4;

  /**
   * Returns the default portrait icon width and height for the current game.
   *
   * @param magnified Specifies whether to return the original or magnified size.
   */
  private static int getDefaultIconSize(boolean magnified) {
    final int scale = magnified ? 2 : 1;
    final int size;
    switch (Profile.getEngine()) {
      case IWD2:
        size = 10;
        break;
      default:
        size = 13;
    }
    return ICON_BORDER + size * scale;
  }

  /**
   * Returns a transparent default icon of given dimension.
   *
   * @param magnified Specifies whether to return the icon in original size or magnified size.
   * @return a transparent {@link ImageIcon} of the desired size.
   */
  public static synchronized ImageIcon getDefaultIcon(boolean magnified) {
    if (DEFAULT_ICONS.getValue0() == null) {
      DEFAULT_ICONS.setValue0(new ImageIcon(ColorConvert.createCompatibleImage(getDefaultIconSize(false),
          getDefaultIconSize(false), Transparency.BITMASK)));
    }
    if (DEFAULT_ICONS.getValue1() == null) {
      DEFAULT_ICONS.setValue1(new ImageIcon(ColorConvert.createCompatibleImage(getDefaultIconSize(true),
          getDefaultIconSize(true), Transparency.BITMASK)));
    }

    return magnified ? DEFAULT_ICONS.getValue1() : DEFAULT_ICONS.getValue0();
  }

  /** Removes original and magnified portrait icons of the specified icon index. */
  public static synchronized void remove(int iconIndex) {
    CACHE.remove(iconIndex);
  }

  /** Removes all entries from the cache. */
  public static synchronized void clearCache() {
    CACHE.clear();
    DEFAULT_ICONS.setValue0(null);
    DEFAULT_ICONS.setValue1(null);
  }

  /**
   * Returns the specified portrait icon in the specified scale.
   *
   * @param iconIndex Portrait icon index.
   * @param magnified Specifies whether the icon should be returned in original size or scaled size.
   * @return {@link ImageIcon} instance of the specified portrait icon. Returns {@code null} if a portrait icon is
   *         unavailable.
   */
  public static synchronized ImageIcon get(int iconIndex, boolean magnified) {
    return get(iconIndex, magnified, null);
  }

  /**
   * Returns the specified portrait icon in the specified scale.
   *
   * @param iconIndex   Portrait icon index.
   * @param magnified   Specifies whether the icon should be returned in original size or scaled size.
   * @param defaultIcon {@link ImageIcon} instance that is returned if the specified portrait icon does not exist.
   * @return {@link ImageIcon} instance of the specified portrait icon. Returns {@code null} if a portrait icon is
   *         unavailable.
   */
  public static synchronized ImageIcon get(int iconIndex, boolean magnified, ImageIcon defaultIcon) {
    if (defaultIcon == null) {
      defaultIcon = getDefaultIcon(magnified);
    }

    if (!isIconsAvailable()) {
      return defaultIcon;
    }

    ensureIconsLoaded();

    final ImageIcon retVal;
    final Couple<ImageIcon, ImageIcon> couple = CACHE.get(iconIndex);
    if (couple != null) {
      retVal = magnified ? couple.getValue1() : couple.getValue0();
    } else {
      retVal = defaultIcon;
    }
    return retVal;
  }

  /** Returns whether portrait icons are supported for the current game. */
  public static boolean isIconsAvailable() {
    return Profile.getGame() != Profile.Game.PSTEE && Profile.getGame() != Profile.Game.PST;
  }

  private static synchronized void ensureIconsLoaded() {
    if (!CACHE.isEmpty()) {
      return;
    }

    final Table2da table = Table2daCache.get("STATDESC.2DA");
    if (table == null) {
      Logger.warn("Could not load resource: STATDESC.2DA");
      return;
    }

    final ResourceEntry defEntry = ResourceFactory.getResourceEntry("STATES.BAM");
    final BamDecoder defDecoder = BamDecoder.loadBam(defEntry);
    final BamControl defControl = defDecoder.createControl();

    for (int row = 0, count = table.getRowCount(); row < count; row++) {
      // fetching table data
      final int iconIndex = Misc.toNumber(table.get(row, 0), -1);
      if (iconIndex >= 0) {
        ResourceEntry entry = null;
        if (table.getColCount(row) > 2) {
          final String res = table.get(row, 2);
          if (!res.contains("*")) {
            entry = ResourceFactory.getResourceEntry(res + ".BAM");
          }
        }

        if (entry == null) {
          entry = defEntry;
        }

        // loading BAM image
        final BamDecoder decoder;
        final BamControl control;
        final int cycleIndex;
        if (entry.equals(defEntry)) {
          decoder = defDecoder;
          control = defControl;
          cycleIndex = iconIndex + 65;
        } else {
          decoder = BamDecoder.loadBam(entry);
          control = (decoder != null) ? decoder.createControl() : null;
          cycleIndex = 0;
        }

        ImageIcon icon = null;
        if (control != null) {
          final int frameIdx = control.cycleGetFrameIndexAbsolute(cycleIndex, 0);
          if (frameIdx >= 0) {
            icon = new ImageIcon(decoder.frameGet(control, frameIdx));
            if (icon.getIconWidth() < 4 || icon.getIconHeight() < 4) {
              // don't cache empty/placeholder icons
              icon = null;
            }
          }
        }

        if (icon != null) {
          ImageIcon iconScaled = createScaledIcon(icon);
          CACHE.put(iconIndex, Couple.with(icon, iconScaled));
        }
      }
    }
  }

  private static synchronized ImageIcon createScaledIcon(ImageIcon icon) {
    ImageIcon retVal = null;

    if (icon != null) {
      final BufferedImage srcImage = ColorConvert.toBufferedImage(icon.getImage(), true, true);
      final BufferedImage dstImage = ColorConvert.createCompatibleImage(ICON_BORDER + srcImage.getWidth() * 2,
          ICON_BORDER + srcImage.getHeight() * 2, true);
      final Graphics2D g = dstImage.createGraphics();
      try {
        g.setComposite(AlphaComposite.SrcOver);
        g.drawImage(srcImage, new AffineTransformOp(AffineTransform.getScaleInstance(2.0, 2.0),
            AffineTransformOp.TYPE_NEAREST_NEIGHBOR), ICON_BORDER / 2, ICON_BORDER / 2);
      } finally {
        g.dispose();
      }
      retVal = new ImageIcon(dstImage);
    }

    return retVal;
  }

}
