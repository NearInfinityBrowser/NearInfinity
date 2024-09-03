// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.awt.Rectangle;

import org.infinity.resource.Profile;
import org.infinity.util.Logger;
import org.infinity.util.StringBufferStream;

/**
 * Handles BCS object (OB) byte code structures.
 */
public class BcsObject extends BcsStructureBase {

  private static final BcsObject EMPTY_OBJECT = new BcsObject();

  // Helpful constants for automating the byte code creation process:
  // T=target, I=identifier, S=string, R=rectangle
  // Number behind type: index in array
  private static final String[] PARSE_CODE_BG   = "T0:T3:T4:T5:T6:T7:T8:I0:I1:I2:I3:I4:S0".split(":");
  private static final String[] PARSE_CODE_PST  = "T0:T1:T2:T3:T4:T5:T6:T7:T8:I0:I1:I2:I3:I4:R0:S0".split(":");
  private static final String[] PARSE_CODE_IWD  = "T0:T3:T4:T5:T6:T7:T8:I0:I1:I2:I3:I4:R0:S0".split(":");
  private static final String[] PARSE_CODE_IWD2 = "T0:T3:T4:T5:T6:T7:T8:T9:I0:I1:I2:I3:I4:R0:S0:TA:TB".split(":");

  public static final int IDX_EA        = 0;
  public static final int IDX_FACTION   = 1;  // PST
  public static final int IDX_TEAM      = 2;  // PST
  public static final int IDX_GENERAL   = 3;
  public static final int IDX_RACE      = 4;
  public static final int IDX_CLASS     = 5;
  public static final int IDX_SPECIFIC  = 6;
  public static final int IDX_GENDER    = 7;
  public static final int IDX_ALIGN     = 8;
  public static final int IDX_SUBRACE   = 9;  // IWD2
  public static final int IDX_AVCLASS   = 10; // IWD2
  public static final int IDX_CLASSMASK = 11; // IWD2

  public final long[] target;     // target ids (see above)
  public final long[] identifier; // identifiers from OBJECT.IDS
  public final Rectangle region;
  public String name;

  /** Constructs an empty object structure. */
  public BcsObject() {
    this.target = new long[12];
    this.identifier = new long[5];
    this.region = new Rectangle(-1, -1, -1, -1);
    this.name = "";
  }

  /** Constructs a fully initialized object structure from the data of the specified stream. */
  public BcsObject(StringBufferStream sbs) throws Exception {
    this();
    init(sbs);
  }

  /** Returns an empty target object. */
  public static BcsObject getEmptyObject() {
    return EMPTY_OBJECT;
  }

  /** Returns the correctly ordered list of target IDS resources (without extension). */
  public static String[] getTargetList() {
    return getTargetList(Profile.getEngine());
  }

  /**
   * Returns the correctly ordered list of target IDS resources (without extension) for the specified game engine.
   */
  public static String[] getTargetList(Profile.Engine engine) {
    return ScriptInfo.getInfo().getObjectIdsList();
  }

  /** Returns {@code true} if list of target values is empty. */
  public boolean isEmptyTarget() {
    boolean empty = true;
    for (long l : target) {
      empty = (l == 0L);
      if (!empty) {
        break;
      }
    }
    return empty;
  }

  /** Returns {@code true} if list of identifier values is empty. */
  public boolean isEmptyIdentifier() {
    boolean empty = true;
    for (long l : identifier) {
      empty = (l == 0L);
      if (!empty) {
        break;
      }
    }
    return empty;
  }

  /** Returns {@code true} if rectangle parameter is empty. */
  public boolean isEmptyRect() {
    return (region.x == -1) && (region.y == -1) && (region.width == -1) && (region.height == -1);
  }

  /** Returns {@code true} if string parameter is empty. */
  public boolean isEmptyString() {
    return name.isEmpty();
  }

  /** Returns {@code true} only if all parameters contain default values. */
  public boolean isEmpty() {
    return isEmptyTarget() && isEmptyIdentifier() && isEmptyRect() && isEmptyString();
  }

  /**
   * Sets the specified IDS target value.
   *
   * @param index A zero-based index that is relative to the IDS sequence of the current game.
   * @param value The value to set
   * @throws IllegalArgumentException if {@code index} is out of range.
   */
  public void setTargetValue(int index, long value) {
    String[] parseCode = getParseCode();
    int counter = 0;
    for (String code : parseCode) {
      char type = code.charAt(0);
      if (type == 'T') {
        if (index == counter) {
          char param = code.charAt(1);
          int dstIdx = (param >= 'A') ? (10 + (param - 'A')) : (param - '0');
          target[dstIdx] = value;
          return;
        } else {
          counter++;
        }
      }
    }
    throw new IllegalArgumentException();
  }

  /**
   * Sets the specified target identifier value.
   *
   * @param index Nesting level of identifier
   * @param value Identifier value to set
   * @throws IllegalArgumentException if {@code index} is out of range.
   */
  public void setIdentifierValue(int index, long value) {
    if (index >= 0 && index < identifier.length) {
      identifier[index] = value;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the bytecode representation of the object.
   *
   * @return The bytecode representation of the object.
   * @throws Exception on unresolvable error.
   */
  public String toByteCode() throws Exception {
    String[] parseCode = getParseCode();
    StringBuilder sb = new StringBuilder();
    sb.append("OB\n");
    char type = 0;
    for (String code : parseCode) {
      type = code.charAt(0);
      char param = code.charAt(1);
      int index = (param >= 'A') ? (10 + (param - 'A')) : (param - '0');
      switch (type) {
        case 'T':
          sb.append((int) target[index]);
          break;
        case 'I':
          sb.append((int) identifier[index]);
          break;
        case 'S':
          sb.append('"').append(name).append('"');
          break;
        case 'R':
          sb.append('[').append(region.x).append('.').append(region.y).append('.').append(region.width).append('.')
              .append(region.height).append(']');
          break;
        default:
          throw new Exception("Internal bytecode error (" + type + name + ")");
      }
      sb.append(' ');
    }
    if (type == 'S') {
      // special: remove trailing space if last element is string type
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append("OB\n");
    return sb.toString();
  }

  private String[] getParseCode() {
    switch (Profile.getEngine()) {
      case PST:
        return PARSE_CODE_PST;
      case IWD:
        return PARSE_CODE_IWD;
      case IWD2:
        return PARSE_CODE_IWD2;
      default:
        return PARSE_CODE_BG;
    }
  }

  private void init(StringBufferStream sbs) throws Exception {
    StringBuilder paramTypes = new StringBuilder();
    long[] num = new long[target.length + identifier.length];
    int cntNums = 0, posRect = -1, posName = -1;
    for (int cnt = 0; !sbs.eos() && !sbs.skip("OB"); cnt++) {
      char ch = determineParamType(sbs);
      switch (ch) {
        case 'I':
          try {
            long n = parseNumber(sbs);
            if (cntNums < num.length) {
              num[cntNums++] = n;
            }
          } catch (Exception e) {
            Logger.warn(e);
          }
          break;
        case 'S':
          try {
            name = parseString(sbs);
            posName = cnt;
          } catch (Exception e) {
            Logger.warn(e);
          }
          break;
        case 'P':
          try {
            Rectangle r = parseRectangle(sbs);
            region.x = r.x;
            region.y = r.y;
            region.width = r.width;
            region.height = r.height;
            posRect = cnt;
          } catch (Exception e) {
            Logger.warn(e);
          }
          break;
        default:
          throw new Exception("Invalid BCS object code at position " + cnt);
      }
      paramTypes.append(ch);
    }
    // assigning correct numeric parameters
    int numTargets = cntNums - 5;
    int numIdentifiers = 5;
    int posIdentifiers = ((posRect >= 0) ? posRect : posName) - 5;
    if (numTargets < 0 || posIdentifiers < 0) {
      throw new Exception("Too few numeric object parameters");
    }
    int cnt = 0, posTarget = 0, posIdent = 0;
    for (int i = 0, len = paramTypes.length(); i < len; i++) {
      char ch = paramTypes.charAt(i);
      if (ch == 'I') {
        if (i >= posIdentifiers && i < posIdentifiers + numIdentifiers) {
          identifier[posIdent++] = num[cnt];
        } else {
          target[posTarget++] = num[cnt];
        }
        cnt++;
      }
    }
  }
}
