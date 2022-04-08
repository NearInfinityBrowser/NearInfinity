// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.awt.Point;
import java.awt.Rectangle;

import org.infinity.util.StringBufferStream;

/**
 * Base class for BCS trigger, action and object handling classes.
 */
public abstract class BcsStructureBase {
  protected char determineParamType(StringBufferStream sbs) {
    char ch = 0;
    if ("-0123456789".indexOf(sbs.peek()) >= 0) {
      ch = 'I';
    } else if (sbs.peek() == '"') {
      ch = 'S';
    } else if (sbs.peek() == '[') {
      ch = 'P'; // can be point or rectangle
    } else if (sbs.peek("OB")) {
      ch = 'O';
    }
    return ch;
  }

  protected long parseNumber(StringBufferStream sbs) throws Exception {
    String s = sbs.getMatch("-?[0-9]+");
    if (s == null) {
      throw new Exception();
    }
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      throw new Exception();
    }
  }

  protected String parseString(StringBufferStream sbs) throws Exception {
    String s = sbs.getMatch("\"[^\"]*\"");
    if (s == null || s.length() < 2) {
      throw new Exception();
    }
    return s.substring(1, s.length() - 1);
  }

  protected BcsObject parseObject(StringBufferStream sbs) throws Exception {
    if (sbs.skip("OB")) {
      return new BcsObject(sbs);
    } else {
      throw new Exception();
    }
  }

  protected Point parsePoint(StringBufferStream sbs) throws Exception {
    int[] values = parseNumberArray(sbs, '[', ']', ',', 2);
    return new Point(values[0], values[1]);
  }

  protected Rectangle parseRectangle(StringBufferStream sbs) throws Exception {
    int[] values = parseNumberArray(sbs, '[', ']', '.', 4);
    return new Rectangle(values[0], values[1], values[2], values[3]);
  }

  protected int[] parseNumberArray(StringBufferStream sbs, char tagOpen, char tagClose, char separator, int numItems)
      throws Exception {
    int[] retVal = new int[numItems];
    boolean success = true;
    success = (sbs.get() == tagOpen);
    for (int i = 0; i < numItems && success; i++) {
      if (i > 0) {
        success = (sbs.get() == separator);
      }
      if (success) {
        String s = sbs.getMatch("-?[0-9]+");
        try {
          retVal[i] = Integer.parseInt(s);
        } catch (NumberFormatException e) {
          success = false;
        }
      }
    }
    if (success) {
      success = (sbs.get() == tagClose);
    }
    if (success) {
      return retVal;
    } else {
      throw new Exception();
    }
  }

  /**
   * Returns the string argument for the specified function at the requested relative string position.
   *
   * @param function Function prototype
   * @param position Relative position of string argument (in range: [0, 4])
   * @param strings  Available strings parameters containing the requested string argument.
   * @return Requested string argument, empty string otherwise.
   * @throws IllegalArgumentException if position is out of range
   */
  public String getStringParam(Signatures.Function function, int position, String[] strings) {
    String retVal = null;
    if (function != null && position < strings.length * 2) {
      int scnt = 0, spos = 0;
      for (int i = 0, cnt = function.getNumParameters(); i < cnt; i++) {
        Signatures.Function.Parameter p = function.getParameter(i);
        if (p.getType() == Signatures.Function.Parameter.TYPE_STRING) {
          if (scnt == position) {
            String s = strings[spos >> 1];
            if (p.isCombinedString()) {
              boolean even = (spos & 1) == 0;
              boolean colon = p.isColonSeparatedString();
              int pos = colon ? s.indexOf(':') : Math.min(6, s.length());
              int ofs = colon ? 1 : 0;
              retVal = even ? s.substring(pos + ofs) : s.substring(0, pos);
            } else {
              retVal = s;
            }
            break;
          } else {
            spos += p.isCombinedString() ? 1 : 2;
            scnt++;
          }
        }
      }
    } else if (position < strings.length) {
      // simply return string parameters
      return strings[position];
    } else {
      throw new IllegalArgumentException();
    }
    if (retVal == null) {
      retVal = "";
    }
    return retVal;
  }

  /**
   * Stores the specified array of strings into two string parameters using hints from the Function object regarding how
   * and what to combine.
   *
   * @param function Function prototype
   * @param strings  Array of string arguments that should be stored in the two string parameters.
   * @return The two string parameters.
   */
  public String[] setStringParams(Signatures.Function function, String[] strings) {
    String[] retVal = { "", "" };

    if (strings != null) {
      int srcIndex = 0, dstIndex = 0;
      boolean even = true;
      for (int i = 0, cnt = function.getNumParameters(); i < cnt; i++) {
        Signatures.Function.Parameter p = function.getParameter(i);
        if (p.getType() == Signatures.Function.Parameter.TYPE_STRING && srcIndex < strings.length) {
          if (p.isCombinedString()) {
            boolean colon = p.isColonSeparatedString();

            if (colon && retVal[dstIndex].length() == 0) {
              retVal[dstIndex] = ":";
            }

            if (even) {
              retVal[dstIndex] = retVal[dstIndex] + strings[srcIndex];
            } else {
              retVal[dstIndex] = strings[srcIndex] + retVal[dstIndex];
              dstIndex++;
            }
            even = !even;
          } else {
            retVal[dstIndex] = strings[srcIndex];
            dstIndex++;
            even = true;
          }
          srcIndex++;
        }
      }
    }

    return retVal;
  }
}
