// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics.decoder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Texture decoder for unsupported pixel formats.
 */
public class DummyDecoder implements Decodable {
  private final PvrInfo info;

  /**
   * Initializes a new dummy decoder from with the specified {@link PvrInfo}.
   * <p>
   * <b>Note:</b> The parameter is currently unused and exists only to satisfy the {@link Decodable} interface.
   * </p>
   */
  public DummyDecoder(PvrInfo pvr) {
    this.info = pvr;
  }

  // --------------------- Begin Interface Decodable ---------------------

  @Override
  public boolean decode(BufferedImage image, Rectangle region) throws Exception {
    if (image == null || region == null) {
      return false;
    }

    int[] imgData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    int ofs = 0;
    int maxX = (image.getWidth() < region.width) ? image.getWidth() : region.width;
    int maxY = (image.getHeight() < region.height) ? image.getHeight() : region.height;
    for (int y = 0; y < maxY; y++) {
      for (int x = 0; x < maxX; x++) {
        imgData[ofs + x] = 0;
      }
      ofs += image.getWidth();
    }
    imgData = null;

    return true;
  }

  @Override
  public PvrInfo getPvrInfo() {
    return info;
  }

  // --------------------- End Interface Decodable ---------------------
}
