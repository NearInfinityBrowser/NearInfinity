// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2023 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics.decoder;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Common interface for texture decoder classes.
 */
public interface Decodable {
  /**
   * Decodes PVR data in the format as specified by the associated {@link PvrInfo}, and draws the specified
   * "region" into "image".
   *
   * @param image  The output image
   * @param region The PVR texture region to draw onto "image"
   * @return Success state of the operation.
   * @throws Exception on error.
   */
  boolean decode(BufferedImage image, Rectangle region) throws Exception;

  /** Returns the associated {@link PvrInfo} object. */
  PvrInfo getPvrInfo();
}
