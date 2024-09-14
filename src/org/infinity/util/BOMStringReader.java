// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.IOException;
import java.io.StringReader;

/**
 * A simple wrapper around {@link StringReader} which optionally skips UTF BOM (Byte Order Mark) in the associated
 * string.
 */
public class BOMStringReader extends StringReader {
  /**
   * Creates a new string reader and skips UTF BOM if found.
   *
   * @param s String providing the character stream.
   */
  public BOMStringReader(String s) {
    this(s, true);
  }

  /**
   * Creates a new string reader.
   *
   * @param s       String providing the character stream.
   * @param skipBOM Indicates whether BOM should be skipped
   */
  public BOMStringReader(String s, boolean skipBOM) {
    super(s);
    if (skipBOM) {
      try {
        mark(1);
        char bom = (char) read();
        if (bom != '\ufeff') {
          reset();
        }
      } catch (IOException e) {
        Logger.trace(e);
      }
    }
  }
}
