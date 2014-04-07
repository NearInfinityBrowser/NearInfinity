// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.converter;

import java.awt.image.BufferedImage;

/**
 * The base class for filters that manipulate on color/pixel level.
 * @author argent77
 */
public abstract class BamFilterBaseColor extends BamFilterBase
{
  protected BamFilterBaseColor(ConvertToBam parent, String name, String desc)
  {
    super(parent, name, desc, Type.Color);
  }

  /**
   * Applies the filter to the specified BufferedImage object.
   * The returned BufferedImage object can either ne the modified source image or a new copy.
   * @param frame The BufferedImage object to modify.
   * @return The resulting BufferedImage object.
   */
  public abstract BufferedImage process(BufferedImage frame) throws Exception;
}
