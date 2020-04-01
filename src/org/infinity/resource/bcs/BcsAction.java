// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;

import org.infinity.util.StringBufferStream;

/**
 * Handles BCS action (AC) byte code structures.
 */
public class BcsAction extends BcsStructureBase
{
  // Helpful constants for automating the byte code generation process:
  // Number/letter behind type: X=action code, 1..9=a1..a9
  private static final String PARSE_CODE  = "X123456789";

  public final Signatures signatures; // lookup for trigger function signatures

  public int id;
  public BcsObject a1;  // ActionOverride(a1, ...)
  public BcsObject a2;
  public BcsObject a3;
  public long a4;
  public final Point a5;
  public long a6;
  public long a7;
  public String a8;
  public String a9;

  /** Constructs an empty action structure. */
  public BcsAction(Signatures signatures)
  {
    this.signatures = signatures;
    this.a1 = new BcsObject();
    this.a2 = new BcsObject();
    this.a3 = new BcsObject();
    this.a5 = new Point();
    this.a8 = "";
    this.a9 = "";
  }

  /** Constructs a fully initialized action structure from the data of the specified stream. */
  public BcsAction(StringBufferStream sbs, Signatures signatures) throws Exception
  {
    this.signatures = signatures;
    this.a5 = new Point();
    init(sbs);
  }

  /**
   * Returns the bytecode representation of the action.
   * @return The bytecode representation of the action.
   * @throws Exception on unresolvable error.
   */
  public String toByteCode() throws Exception
  {
    StringBuilder sb = new StringBuilder();
    sb.append("AC\n");
    for (int i = 0, cnt = PARSE_CODE.length(); i < cnt; i++) {
      char name = PARSE_CODE.charAt(i);
      switch (name) {
        case 'X': sb.append(id); break;
        case '1': sb.append(a1.toByteCode()); break;
        case '2': sb.append(a2.toByteCode()); break;
        case '3': sb.append(a3.toByteCode()); break;
        case '4': sb.append((int)a4).append(' '); break;
        case '5': sb.append(a5.x).append(' ').append(a5.y).append(' '); break;
        case '6': sb.append((int)a6).append(' '); break;
        case '7': sb.append((int)a7); break;
        case '8': sb.append('"').append(a8).append('"').append(' '); break;
        case '9': sb.append('"').append(a9).append('"').append(' '); break;
        default: throw new Exception("Internal bytecode error (" + name + ")");
      }
    }
    sb.append("AC\n");
    return sb.toString();
  }

  @Override
  public String toString()
  {
    try {
      return toByteCode();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  /**
   * Attempts to find the best matching function signature for the currently defined
   * action parameters.
   * @return A function signature when a match has been found, {@code null} otherwise.
   */
  public Signatures.Function getMatchingFunction()
  {
    Signatures.Function retVal = null;

    Signatures.Function[] functions = getSortedFunction(id);
    if (functions != null && functions.length > 0) {
      if (functions.length == 1) {
        retVal = functions[0];
      } else {
        // weighting parameter types (strings are most important)
        int weightI = 1;
        int weightO = 2;
        int weightP = 4;
        int weightS = 8;
        int bestScore = Integer.MAX_VALUE;  // lower is better
        Signatures.Function fallback = null;
        for (Signatures.Function f: functions) {
          int sidx = 0; // tracks usage of strings
          int pi = 0, ps = 0, po = 0, pp = 0;
          for (int i = 0, cnt = f.getNumParameters(); i < cnt; i++) {
            Signatures.Function.Parameter param = f.getParameter(i);
            switch (param.getType()) {
              case Signatures.Function.Parameter.TYPE_INTEGER:
                pi++;
                break;
              case Signatures.Function.Parameter.TYPE_STRING:
                ps++;
                sidx += param.isCombinedString() ? 1 : 2;
                break;
              case Signatures.Function.Parameter.TYPE_OBJECT:
                po++;
                break;
              case Signatures.Function.Parameter.TYPE_POINT:
                pp++;
                break;
            }
          }

          // prefer function signature with string arguments for fallback solution
          if (fallback == null && ps > 0) {
            fallback = f;
          }

          // evaluating remaining arguments
          for (int i = 0; i < 3; i++) {
            if (getNumericParam(i) != 0) {
              pi--;
            }
          }

          if ((sidx < 2 && !a8.isEmpty()) ||
              (sidx < 4 && !a9.isEmpty())) {
            // don't choose if string arguments are left
            ps = -1;
          } else {
            for (int i = 0; i < 4; i++) {
              try {
                if (!getStringParam(f, i).isEmpty()) {
                  ps--;
                }
              } catch (IllegalArgumentException e) {
                break;
              }
            }
          }

          for (int i = 1; i < 3; i++) {
            if (!getObjectParam(i).isEmpty()) {
              po--;
            }
          }

          if (a5.x != 0 || a5.y != 0) {
            pp--;
          }

          // finding match
          int score = Math.abs(pi * weightI + ps * weightS + po * weightO + pp * weightP);
          if (score < bestScore) {
            bestScore = score;
            retVal = f;
          }
        }

        if (retVal == null) {
          retVal = (fallback != null) ? fallback : functions[0];
        }
      }
    }

    return retVal;
  }

  /**
   * Returns the string at the specified relative string argument position.
   * @throws IllegalArgumentException if index is out of range.
   */
  public String getStringParam(Signatures.Function function, int position)
  {
    return getStringParam(function, position, new String[]{a8, a9});
  }

  /**
   * Returns the numeric parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public long getNumericParam(int index)
  {
    switch (index) {
      case 0: return a4;
      case 1: return a6;
      case 2: return a7;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Sets the numeric parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public void setNumericParam(int index, long value)
  {
    switch (index) {
      case 0: a4 = value; break;
      case 1: a6 = value; break;
      case 2: a7 = value; break;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the object parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public BcsObject getObjectParam(int index)
  {
    switch (index) {
      case 0: return a1;  // ActionOverride target
      case 1: return a2;
      case 2: return a3;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Set the object parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public void setObjectParam(int index, BcsObject object)
  {
    if (object == null) { object = BcsObject.getEmptyObject(); }
    switch (index) {
      case 0: a1 = object; break;
      case 1: a2 = object; break;
      case 2: a3 = object; break;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the point parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public Point getPointParam(int index)
  {
    if (index == 0) {
      return a5;
    } else {
      throw new IllegalArgumentException();
    }
  }

  private void init(StringBufferStream sbs) throws Exception
  {
    int cntNums= 0, cntStrings = 0, cntObjects = 0;
    for (int cnt = 0; !sbs.eos() && !sbs.skip("AC"); cnt++) {
      char ch = determineParamType(sbs);
      switch (ch) {
        case 'I':
          try {
            long n = parseNumber(sbs);
            switch (cntNums) {
              case 0: id = (int)n; break;
              case 1: a4 = n; break;
              case 2: a5.x = (int)n; break;
              case 3: a5.y = (int)n; break;
              case 4: a6 = n; break;
              case 5: a7 = n; break;
            }
            cntNums++;
          } catch (Exception e) {
          }
          break;
        case 'S':
          try {
            String s = parseString(sbs);
            if (cntStrings == 0) {
              a8 = s;
            } else if (cntStrings == 1) {
              a9 = s;
            }
            cntStrings++;
          } catch (Exception e) {
          }
          break;
        case 'O':
          switch (cntObjects) {
            case 0: a1 = parseObject(sbs); break;
            case 1: a2 = parseObject(sbs); break;
            case 2: a3 = parseObject(sbs); break;
          }
          cntObjects++;
          break;
        default:
          throw new Exception("Invalid BCS action code at position " + cnt);
      }
    }
  }

  // Helper method: Returns a sorted list of function signatures matching the specified function id.
  private Signatures.Function[] getSortedFunction(int id)
  {
    Signatures.Function[] functions = signatures.getFunction(id);
    Arrays.sort(functions, new Comparator<Signatures.Function>() {
      @Override
      public int compare(Signatures.Function o1, Signatures.Function o2)
      {
        if (o1 != null && o2 != null)
          return o1.getName().compareTo(o2.getName());
        else if (o1 == null && o2 == null)
          return 0;
        else if (o1 != null && o2 == null)
          return -1;
        else
          return 1;
      }
    });
    return functions;
  }
}
