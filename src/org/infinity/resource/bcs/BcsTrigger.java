// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;

import org.infinity.resource.Profile;
import org.infinity.util.StringBufferStream;

/**
 * Handles BCS trigger (TR) byte code structures.
 */
public class BcsTrigger extends BcsStructureBase
{
  // Helpful constants for automating the byte code generation process:
  // Number/letter behind type: X=trigger id, N=negated, 1..7=t1..t7
  private static final String PARSE_CODE_BG = "X1N23456";
  private static final String PARSE_CODE_PST = "X1N237456";

  public final Signatures signatures; // lookup for trigger function signatures

  public int id;    // trigger id
  public boolean negated;
  public long t1;
  public long t2;
  public long t3;    // used by IWD2 only
  public String t4;
  public String t5;
  public BcsObject t6;
  public final Point t7;
  public BcsTrigger override;

  /** Constructs an empty trigger structure. */
  public BcsTrigger(Signatures signatures)
  {
    this.signatures = signatures;
    this.t4 = "";
    this.t5 = "";
    this.t6 = new BcsObject();
    this.t7 = new Point();
  }

  /** Constructs a fully initialized trigger structure from the data of the specified stream. */
  public BcsTrigger(StringBufferStream sbs, Signatures signatures) throws Exception
  {
    this.signatures = signatures;
    this.t7 = new Point();
    init(sbs);
  }

  /** Returns {@code true} if the current trigger is {@code NextTriggerObject()}. */
  public boolean isOverride()
  {
    Signatures.Function[] functions = signatures.getFunction(id);
    if (functions != null) {
      for (Signatures.Function f: functions) {
        if (f.getName().equalsIgnoreCase("NextTriggerObject") &&
            f.getNumParameters() == 1 &&
            f.getParameter(0).getType() == Signatures.Function.Parameter.TYPE_OBJECT) {
          return true;
        }
      }
    }
    return false;
  }

  /** Applies a {@code NextTriggerObject()} to the current trigger for combination purposes. */
  public void setOverride(BcsTrigger override)
  {
    if (override == null || override.isOverride()) {
      this.override = override;
    }
  }

  /** Returns an override trigger object attached to the current trigger. Returns {@code null} otherwise. */
  public BcsTrigger getOverride()
  {
    return this.override;
  }

  /** Returns {@code true} if the current trigger is {@code OR()} */
  public boolean isOR()
  {
    Signatures.Function[] functions = signatures.getFunction(id);
    if (functions != null) {
      for (Signatures.Function f: functions) {
        if (f.getName().equalsIgnoreCase("OR") &&
            f.getNumParameters() == 1 &&
            f.getParameter(0).getType() == Signatures.Function.Parameter.TYPE_INTEGER) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the number of subsequent triggers considered by OR().
   * Returns -1 if this is not an OR() trigger.
   */
  public long getORCount()
  {
    if (isOR()) {
      return t1;
    }
    return -1L;
  }

  /**
   * Returns the bytecode representation of the trigger.
   * @return The bytecode representation of the trigger.
   * @throws Exception on unresolvable error.
   */
  public String toByteCode() throws Exception
  {
    String parseCode = (Profile.getGame() == Profile.Game.PST) ? PARSE_CODE_PST : PARSE_CODE_BG;
    StringBuilder sb = new StringBuilder();
    sb.append("TR\n");
    for (int i = 0, cnt = parseCode.length(); i < cnt; i++) {
      char name = parseCode.charAt(i);
      switch (name) {
        case 'X': sb.append(id).append(' '); break;
        case 'N': sb.append(negated ? 1 : 0).append(' '); break;
        case '1': sb.append((int)t1).append(' '); break;
        case '2': sb.append((int)t2).append(' '); break;
        case '3': sb.append((int)t3).append(' '); break;
        case '4': sb.append('"').append(t4).append('"').append(' '); break;
        case '5': sb.append('"').append(t5).append('"').append(' '); break;
        case '6': sb.append(t6.toByteCode()); break;
        case '7': sb.append('[').append(t7.x).append(',').append(t7.y).append(']').append(' '); break;
        default: throw new Exception("Internal bytecode error (" + name + ")");
      }
    }
    sb.append("TR\n");
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
   * trigger parameters.
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
          int pi = 0, ps = 0, po = 0, pp = 0;
          for (int i = 0, cnt = f.getNumParameters(); i < cnt; i++) {
            Signatures.Function.Parameter param = f.getParameter(i);
            switch (param.getType()) {
              case Signatures.Function.Parameter.TYPE_INTEGER:
                pi++;
                break;
              case Signatures.Function.Parameter.TYPE_STRING:
                ps++;
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

          for (int i = 0; i < 4; i++) {
            try {
              if (!getStringParam(f, i).isEmpty()) {
                ps--;
              }
            } catch (IllegalArgumentException e) {
              break;
            }
          }

          if (!getObjectParam(0).isEmpty()) {
            po--;
          }

          if (getPointParam(0).x != 0 || getPointParam(0).y != 0) {
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
    return getStringParam(function, position, new String[]{t4, t5});
  }

  /**
   * Returns the numeric parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public long getNumericParam(int index)
  {
    switch (index) {
      case 0: return t1;
      case 1: return t2;
      case 2: return t3;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Sets a value to the parameter at the specified location.
   * @throws IllegalArgumentException if index is out of range.
   */
  public void setNumericParam(int index, long value)
  {
    switch (index) {
      case 0: t1 = value; break;
      case 1: t2 = value; break;
      case 2: t3 = value; break;
      default: throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the object parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public BcsObject getObjectParam(int index)
  {
    if (index == 0) {
      return t6;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Sets the object parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public void setObjectParam(int index, BcsObject obj)
  {
    if (index == 0) {
      t6 = obj;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Returns the point parameter at the specified index.
   * @throws IllegalArgumentException if index is out of range.
   */
  public Point getPointParam(int index)
  {
    if (index == 0) {
      return t7;
    } else {
      throw new IllegalArgumentException();
    }
  }


  private void init(StringBufferStream sbs) throws Exception
  {
    int cntNums= 0, cntStrings = 0, cntObjects = 0;
    for (int cnt = 0; !sbs.eos() && !sbs.skip("TR"); cnt++) {
      char ch = determineParamType(sbs);
      switch (ch) {
        case 'I':
          try {
            long n = parseNumber(sbs);
            switch (cntNums) {
              case 0: id = (int)n; break;
              case 1: t1 = n; break;
              case 2: negated = (n & 1) == 1; break;
              case 3: t2 = n; break;
              case 4: t3 = n; break;
            }
            cntNums++;
          } catch (Exception e) {
          }
          break;
        case 'S':
          try {
            String s = parseString(sbs);
            if (cntStrings == 0) {
              t4 = s;
            } else if (cntStrings == 1) {
              t5 = s;
            }
            cntStrings++;
          } catch (Exception e) {
          }
          break;
        case 'P':
          try {
            Point p = parsePoint(sbs);
            t7.x = p.x; t7.y = p.y;
          } catch (Exception e) {
          }
          break;
        case 'O':
          if (cntObjects == 0) {
            t6 = parseObject(sbs);
          }
          cntObjects++;
          break;
        default:
          throw new Exception("Invalid BCS trigger code at position " + cnt);
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
