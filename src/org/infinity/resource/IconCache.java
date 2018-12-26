// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.infinity.datatype.ResourceRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.ColorConvert;

/**
 * Cache for images, decoded from BAM files
 *
 * @author Mingun
 */
public class IconCache
{
  /** Mapping from resource name to icon, created from it. */
  private static final Map<String, Icon> CACHE = new HashMap<>();
  private IconCache() {}

  /** Removes all entries from cache. */
  public static void clearCache() { CACHE.clear(); }

  /**
   * Receives the image for an object, if necessary decoding it from the BAM file.
   * If such icon was already decoded, returns it from a cache
   *
   * @param object Object that can have icon
   * @return Icon that can be shown in the Jlabel, never {@code null}
   */
  public static Icon getIcon(HasIcon object)
  {
    final BrowserMenuBar.ShowItemIcons option = BrowserMenuBar.getInstance().getShowItemIcons();
    if (option == BrowserMenuBar.ShowItemIcons.None) {
      return null;
    }

    final boolean is32 = option == BrowserMenuBar.ShowItemIcons.Large;
    final ResourceRef ref = object.getIcon();
    if (ref != null) {
      return CACHE.computeIfAbsent(ref.getResourceName(), (key) -> {
        final BamDecoder decoder = BamDecoder.loadBam(ResourceFactory.getResourceEntry(key));
        if (decoder == null || decoder.frameCount() == 0) {
          return Icons.getIcon(is32 ? Icons.ICON_NO_ICON_32 : Icons.ICON_NO_ICON_16);
        }
        return new ImageIcon(getImage(decoder, 0, is32 ? 32 : 16));
      });
    }
    return Icons.getIcon(is32 ? Icons.ICON_NO_ICON_32 : Icons.ICON_NO_ICON_16);
  }
  /**
   * Decodes specified frame and return it as image, scaled to specified size.
   *
   * @param decoder Image data
   * @param frameIndex Number of frame that need to decode
   * @param size Size of square image that need to get
   *
   * @return Image with size {@code size x size}
   */
  private static Image getImage(BamDecoder decoder, int frameIndex, int size)
  {
    final BamDecoder.FrameEntry info = decoder.getFrameInfo(frameIndex);
    final boolean isHgtW = info.getHeight() > info.getWidth();

    final Image image  = decoder.frameGet(decoder.createControl(), frameIndex);
    final Image scaled = image.getScaledInstance(isHgtW ? -1 : size, isHgtW ? size : -1, Image.SCALE_DEFAULT);
    final int w = scaled.getWidth(null);
    final int h = scaled.getHeight(null);
    final int x = (size - w) / 2;
    final int y = (size - h) / 2;

    final BufferedImage newImage = ColorConvert.createCompatibleImage(size, size, true);
    final Graphics g = newImage.getGraphics();
    g.drawImage(scaled, x, y, null);
    g.dispose();
    return newImage;
  }
}
