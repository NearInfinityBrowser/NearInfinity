// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import org.infinity.resource.graphics.PseudoBamDecoder;

/**
 * The base class for filters that output the current state of the BAM structure to disk.
 * @author argent77
 */
public abstract class BamFilterBaseOutput extends BamFilterBase
{
  protected BamFilterBaseOutput(ConvertToBam parent, String name, String desc)
  {
    super(parent, name, desc, Type.OUTPUT);
  }

  /**
   * Outputs the specified BamDecoder to disk, considers the configuration stored in the
   * parent ConvertToBam instance.
   * @param decoder The BamDecoder to convert.
   * @return <code>true</code> if the conversion finished successfully, <code>false</code> otherwise.
   */
  public abstract boolean process(PseudoBamDecoder decoder) throws Exception;



  /** Returns a palette of 256 colors if all frames are using the same palette, or null otherwise. */
  public static int[] retrievePalette(PseudoBamDecoder decoder)
  {
    int[] retVal = null;
    if (decoder != null) {
      int[] tmpPalette = new int[256];
      for (int i = 0; i < decoder.frameCount(); i++) {
        if (decoder.getFrameInfo(i).getFrame().getType() != BufferedImage.TYPE_BYTE_INDEXED) {
          // frames must be paletted
          break;
        } else if (retVal == null) {
          // creating reference palette
          IndexColorModel cm = (IndexColorModel)decoder.getFrameInfo(i).getFrame().getColorModel();
          retVal = new int[256];
          cm.getRGBs(retVal);
        } else {
          // comparing current frame's palette with reference palette
          IndexColorModel cm = (IndexColorModel)decoder.getFrameInfo(i).getFrame().getColorModel();
          cm.getRGBs(tmpPalette);
          if (!Arrays.equals(retVal, tmpPalette)) {
            retVal = null;
            break;
          }
        }
      }
    }
    return retVal;
  }
}
