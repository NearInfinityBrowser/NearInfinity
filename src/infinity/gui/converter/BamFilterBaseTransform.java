// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.converter;

import infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * The base class for filters that manipulate on frame level.
 * @author argent77
 */
public abstract class BamFilterBaseTransform extends BamFilterBase
{
  protected BamFilterBaseTransform(ConvertToBam parent, String name, String desc)
  {
    super(parent, name, desc, Type.Transform);
  }

  /**
   * Applies the filter to the specified FrameEntry object.
   * @param frame The frame entry to modify.
   * @return The modified frame entry. Can be either the modified source FrameEntry
   *         or a new FrameEntry instance.
   */
  public abstract PseudoBamFrameEntry process(PseudoBamFrameEntry frame) throws Exception;
}
