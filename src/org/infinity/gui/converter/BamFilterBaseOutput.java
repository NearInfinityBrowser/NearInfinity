// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.nio.file.Path;
import java.util.Arrays;

import org.infinity.resource.graphics.DxtEncoder;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.util.Logger;

/**
 * The base class for filters that output the current state of the BAM structure to disk.
 */
public abstract class BamFilterBaseOutput extends BamFilterBase {
  protected BamFilterBaseOutput(ConvertToBam parent, String name, String desc) {
    super(parent, name, desc, Type.OUTPUT);
  }

  /**
   * Outputs the specified BamDecoder to disk, considers the configuration stored in the parent ConvertToBam instance.
   *
   * @param decoder The BamDecoder to convert.
   * @return {@code true} if the conversion finished successfully, {@code false} otherwise.
   */
  public abstract boolean process(PseudoBamDecoder decoder) throws Exception;

  /** Returns a palette of 256 colors if all frames are using the same palette, or null otherwise. */
  public static int[] retrievePalette(PseudoBamDecoder decoder) {
    int[] retVal = null;
    if (decoder != null) {
      int[] tmpPalette = new int[256];
      for (int i = 0; i < decoder.frameCount(); i++) {
        if (decoder.getFrameInfo(i).getFrame().getType() != BufferedImage.TYPE_BYTE_INDEXED) {
          // frames must be paletted
          break;
        } else if (retVal == null) {
          // creating reference palette
          IndexColorModel cm = (IndexColorModel) decoder.getFrameInfo(i).getFrame().getColorModel();
          retVal = new int[256];
          cm.getRGBs(retVal);
        } else {
          // comparing current frame's palette with reference palette
          IndexColorModel cm = (IndexColorModel) decoder.getFrameInfo(i).getFrame().getColorModel();
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

  /**
   * Converts animation data from the specified decoder into the target BAM format and saves it to disk.
   *
   * @param converter The global {@link ConvertToBam} instance.
   * @param outFileName Output path of the resulting BAM file.
   * @param decoder {@link PseudoBamDecoder} instance with animation data to convert.
   * @return {@code true} if the conversion completed successfully, {@code false} otherwise.
   * @throws Exception if an unrecoverable error occurs during the conversion process.
   */
  public static boolean convertBam(ConvertToBam converter, Path outFileName, PseudoBamDecoder decoder)
      throws Exception {
    if (converter != null && outFileName != null && decoder != null) {
      if (converter.isBamV1Selected()) {
        // convert to BAM v1
        decoder.setOption(PseudoBamDecoder.OPTION_INT_RLEINDEX, converter.getPaletteDialog().getRleIndex());
        decoder.setOption(PseudoBamDecoder.OPTION_BOOL_COMPRESSED, converter.isBamV1Compressed());
        try {
          return decoder.exportBamV1(outFileName, converter.getProgressMonitor(), converter.getProgressMonitorStage());
        } catch (Exception e) {
          Logger.error(e);
          throw e;
        }
      } else {
        // convert to BAM v2
        DxtEncoder.DxtType dxtType = converter.getDxtType();
        int pvrzIndex = converter.getPvrzIndex();
        try {
          return decoder.exportBamV2(outFileName, dxtType, pvrzIndex, BamOptionsDialog.getOverwritePvrzIndices(),
              converter.getProgressMonitor(), converter.getProgressMonitorStage());
        } catch (Exception e) {
          Logger.error(e);
          throw e;
        }
      }
    }
    return false;
  }
}
