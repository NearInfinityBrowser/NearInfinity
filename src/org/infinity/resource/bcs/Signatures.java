// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.StreamUtils;

/**
 * Manages action and trigger function signatures.
 */
public class Signatures
{
  private static HashMap<String, Signatures> instances = new HashMap<>();

  private final TreeMap<Integer, HashSet<Function>> functions;
  private final HashMap<String, Function> functionsByName;
  private final String resource;

  protected Signatures(String resource)
  {
    this.functions = new TreeMap<>();
    this.functionsByName = new HashMap<>(600);
    this.resource = resource;
  }

  /** Returns the IDS resource containing the function signatures. */
  public String getResource()
  {
    return resource;
  }

  /**
   * Returns the full set of available function identifiers which can be used to
   * query function signatures.
   */
  public Set<Integer> getFunctionIds()
  {
    return functions.keySet();
  }

  /** Returns the number of available function signatures. */
  public int getSize()
  {
    return functions.size();
  }

  /**
   * Returns the set of functions associated with the specified identifier.
   * Returns {@code null} if not available.
   */
  public Function[] getFunction(int id)
  {
    HashSet<Function> set = functions.get(Integer.valueOf(id));
    if (set != null) {
      Function[] retVal = new Function[set.size()];
      int idx = 0;
      for (Function f: set) {
        retVal[idx++] = f;
      }
      return retVal;
    }
    return null;
  }

  /**
   * Returns a function of matching name. The match is case-sensitive.
   * @param name Name of the requested function signature.
   * @return A function object on match, {@code null} otherwise.
   */
  public Function getFunction(String name)
  {
    return getFunction(name, true);
  }

  /**
   * Returns a function of matching name.
   * @param name Name of the requested function signature.
   * @param exact Indicates whether to check case-sensitive.
   * @return A function object on match, {@code null} otherwise.
   */
  public Function getFunction(String name, boolean exact)
  {
    Function retVal = null;
    if (name != null) {
      retVal = functionsByName.get(name.toLowerCase(Locale.ENGLISH));
      if (exact && retVal != null && !retVal.getName().equals(name)) {
        retVal = null;
      }
    }
    return retVal;
  }

  /**
   * Removes all function signatures from cache.
   */
  public static void clearCache()
  {
    instances.clear();
  }

  /** Convenience method for getting trigger signatures. */
  public static Signatures getTriggers()
  {
    return get("trigger.ids", true);
  }

  /** Convenience method for getting action signatures. */
  public static Signatures getActions()
  {
    return get("action.ids", false);
  }

  /**
   * Returns a set of function signatures defined by the specified IDS resource.
   * @param resource The IDS resource.
   * @param isTrigger Indicates whether function definitions are considered triggers ({@code true})
   *                  or actions ({@code false}).
   * @return a {@code Signatures} instance. Returns {@code null} on error.
   */
  public static Signatures get(String resource, boolean isTrigger)
  {
    Signatures retVal = null;

    resource = normalizedName(resource);
    retVal = instances.get(resource);
    if (retVal == null) {
      ResourceEntry entry = ResourceFactory.getResourceEntry(resource);
      if (entry == null) {
        return retVal;
      }

      // processing function signatures from IDS files
      try {
        ByteBuffer buffer = entry.getResourceBuffer();
        String data = StreamUtils.readString(buffer, buffer.limit());
        String[] lines = data.split("\\r?\\n");

        retVal = new Signatures(resource);
        for (int idx = 0; idx < lines.length; idx++) {
          Function f = Function.parse(lines[idx], isTrigger);
          if (f != null) {
            HashSet<Function> set = retVal.functions.get(Integer.valueOf(f.getId()));
            if (set == null) {
              set = new HashSet<>(8);
            }
            set.add(f);
            retVal.functions.put(Integer.valueOf(f.getId()), set);
            retVal.functionsByName.put(f.getName().toLowerCase(Locale.ENGLISH), f);
          }
        }
      } catch (Exception e) {
        return retVal;
      }

      // additional hardcoded function signatures that may not be found in IDS files
      ScriptInfo info = ScriptInfo.getInfo();
      List<String> hardcoded = info.getFunctionDefinitions(isTrigger ? Function.FunctionType.TRIGGER :
                                                                       Function.FunctionType.ACTION);
      for (final String line: hardcoded) {
        Function f = Function.parse(line, isTrigger);
        if (f != null) {
          HashSet<Function> set = retVal.functions.get(Integer.valueOf(f.getId()));
          if (set == null) {
            set = new HashSet<>(8);
          }
          set.add(f);
          retVal.functions.put(Integer.valueOf(f.getId()), set);
          retVal.functionsByName.put(f.getName().toLowerCase(Locale.ENGLISH), f);
        }
      }

      instances.put(resource, retVal);
    }
    return retVal;
  }

  // Makes sure IDS resource is lowercased and contains a file extension
  private static String normalizedName(String resource)
  {
    if (resource == null) {
      return "";
    }
    resource = resource.toLowerCase(Locale.ENGLISH);
    int p = resource.lastIndexOf('.');
    if (p < 0) {
      resource = resource + ".ids";
    }
    return resource;
  }




//-------------------------- INNER CLASSES --------------------------

  /** Full set of information about a single function definition. */
  public static class Function
  {
    public static enum FunctionType
    {
      /** Function signature belongs to an action. */
      ACTION,
      /** Function signature belongs to a trigger. */
      TRIGGER
    }

    /**
     * Default name for the convenience trigger TriggerOverride() derived from
     * artificial signature: NextTriggerObject(O:Target,T:Trigger)
     */
    public static final String TRIGGER_OVERRIDE_NAME = "TriggerOverride";
    /** Default name for the action function ActionOverride as defined in ACTION.IDS. */
    public static final String ACTION_OVERRIDE_NAME = "ActionOverride";

    private int id;                     // function identifier
    private String name;                // function name (without parentheses and parameters)
    private ArrayList<Parameter> param; // list of parameter definitions
    private final FunctionType type;    // function type

    public Function(int id, String name, FunctionType type)
    {
      setId(id);
      setName(name);
      this.type = type;
      param = new ArrayList<>();
    }

    public boolean isValid() { return (id >= 0) && !name.isEmpty(); }

    /** Returns the function identifier. */
    public int getId() { return id; }
    /** Assigns a new identifier to the function. */
    public void setId(int id) { this.id = id; }

    /** Returns the function name. */
    public String getName() { return name; }
    /** Assigns a new name to the function. */
    public void setName(String name) { this.name = (name != null) ? name : "Unknown" + id; }

    /** Returns the number of available parameters. */
    public int getNumParameters() { return param.size(); }

    /**
     * Returns the parameter entry at the specified position.
     * Throws an {@link IndexOutOfBoundsException} on error.
     */
    public Parameter getParameter(int index) throws IndexOutOfBoundsException { return param.get(index); }

    /**
     * Adds the specified parameter entry to the end of the parameter list.
     * Returns the new item position.
     */
    public int addParameter(Parameter p) { return insertParameter(getNumParameters(), p); }

    /**
     * Inserts a new parameter entry at the specified position to the parameter list.
     * Returns the new item position or -1 on error.
     */
    public int insertParameter(int index, Parameter p)
    {
      if (p != null) {
        p.setFunction(this);
        index = Math.min(param.size(), Math.max(0, index));
        param.add(index, p);
        return index;
      }
      return -1;
    }

    /** Returns the type of the function definition. */
    public FunctionType getFunctionType()
    {
      return type;
    }

    /** Returns whether the function is considered a trigger. */
    public boolean isTrigger()
    {
      return type == FunctionType.TRIGGER;
    }

    /** Returns whether the function is considered an action. */
    public boolean isAction()
    {
      return type == FunctionType.ACTION;
    }

    /**
     * Returns the number of parameters that define a namespace.
     * The result is independent of whether namespaces are stored separately.
     */
    public int countNamespaces()
    {
      int count = 0;
      for (Parameter p: param) {
        if (p.isNamespace()) {
          count++;
        }
      }
      return count;
    }

    /**
     * Returns the number of string parameters that are not listed as namespaces.
     */
    public int countStrings()
    {
      int count = 0;
      for (Parameter p: param) {
        if (p.getType() == Parameter.TYPE_STRING && !p.isNamespace()) {
          count++;
        }
      }
      return count;
    }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 31 * hash + id;
      hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
      hash = 31 * hash + ((param == null) ? 0 : param.hashCode());
      hash = 31 * hash + ((type == null) ? 0 : type.hashCode());
      return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (obj instanceof Function) {
        // Note: name is irrelevant for comparison
        return (id == ((Function)obj).id) && param.equals(((Function)obj).param);
      }
      return false;
    }

    @Override
    public String toString()
    {
      return toString(false);
    }

    public String toString(boolean verbose)
    {
      StringBuilder sb = new StringBuilder(128);
      if (getFunctionType() == FunctionType.TRIGGER) {
        sb.append(String.format("0x%04X", getId()));
      } else {
        sb.append(getId());
      }
      sb.append(' ').append(getName()).append('(');
      for (int i = 0, count = getNumParameters(); i < count; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(getParameter(i).toString(verbose));
      }
      sb.append(')');
      return sb.toString();
    }

    /**
     * Attempts to parse the specified line into a Function object.
     * @param line A single function definition (e.g. from ACTION.IDS or TRIGGER.IDS).
     * @param isTrigger Function type (TRIGGER or not)
     * @return The parsed function as {@code Function} object, or {@code null} on error.
     */
    private static Function parse(String line, boolean isTrigger)
    {
      Function retVal = null;

      if (line == null) {
        return retVal;
      }

      line = line.trim();
      if (line.isEmpty()) {
        return retVal;
      }

      FunctionType funcType = isTrigger ? FunctionType.TRIGGER : FunctionType.ACTION;

      final Pattern patValue        = Pattern.compile("[0-9]+");
      final Pattern patValueHex     = Pattern.compile("0[xX][0-9a-fA-F]+");
      final Pattern patIdentifier   = Pattern.compile("[a-zA-Z_#][0-9a-zA-Z_#]*");
      final Pattern patBrackOpen    = Pattern.compile("\\(");
      final Pattern patBrackClosed  = Pattern.compile("\\)");
      boolean success = false;
      int pos = 0;

      // getting function id
      int id = -1;
      Matcher m = patValueHex.matcher(line);
      if (success = m.find(pos) && (m.start() == pos)) {
        String strValue = m.group();
        id = Integer.parseInt(strValue.substring(2), 16);
        pos = m.end();
      }
      if (!success) {
        m = patValue.matcher(line);
        if (success = m.find(pos) && (m.start() == pos)) {
          String strValue = m.group();
          id = Integer.parseInt(strValue);
          pos = m.end();
        }
      }
      if (!success) {
        // No function id found
        return retVal;
      }

      pos = skipWhitespace(pos, line);

      // getting function name
      String name = null;
      m = patIdentifier.matcher(line);
      if (success = m.find(pos) && (m.start() == pos)) {
        name = m.group();
        pos = m.end();
      }
      if (!success) {
        // No function name found
        return retVal;
      }

      pos = skipWhitespace(pos, line);

      // getting function parameters
      String params = null;
      m = patBrackOpen.matcher(line);
      if (success = m.find(pos) && (m.start() == pos)) {
        int paramStart = m.start();
        pos = m.end();
        m = patBrackClosed.matcher(line);
        if (success == m.find(pos)) {
          params = line.substring(paramStart, m.end());
        }
      }

      if (success) {
        List<Parameter> paramList = Parameter.parse(params, id, funcType);
        if (paramList != null) {
          retVal = new Function(id, name, funcType);
          for (Parameter p: paramList) {
            retVal.addParameter(p);
          }
        }
      }

      return retVal;
    }

    private static int skipWhitespace(int pos, String line)
    {
      final Pattern patWS = Pattern.compile("\\s*");
      if (line != null && pos >= 0 && pos < line.length()) {
        Matcher m = patWS.matcher(line);
        if (m.find(pos) && m.start() == pos) {
          pos = m.end();
        }
      }
      return pos;
    }


    /** Provides information about a single function parameter. */
    public static class Parameter
    {
      /** Parameter type A (action) */
      public static final char  TYPE_ACTION   = 'A';
      /** Parameter type T (trigger) */
      public static final char  TYPE_TRIGGER  = 'T';
      /** Parameter type I (numeric value) */
      public static final char  TYPE_INTEGER  = 'I';
      /** Parameter type O (object specifier) */
      public static final char  TYPE_OBJECT   = 'O';
      /** Parameter type P (point structure) */
      public static final char  TYPE_POINT    = 'P';
      /** Parameter type S (literal string) */
      public static final char  TYPE_STRING   = 'S';

      /** Indicates a creature's script name */
      public static final String RESTYPE_SCRIPT     = "script";
      /** Indicates a sequence of spell codes */
      public static final String RESTYPE_SPELL_LIST = "spelllist";

      private Function function;
      private char type;              // Uppercase letter([IOS] for triggers, [AIOPS] for actions)
      private String name;            // Parameter name without trailing asterisk (*)
      private String idsRef;          // Lowercased optional IDS reference
      private String resType;         // Optional list of resource types referenced by this parameter
      private boolean combinedString; // String argument is part of combined string parameter?
      private boolean colonSeparated; // String argument is part of colon-separated string parameter?

      public Parameter()
      {
        this.function = null;
        setType('\0');
        setName(null);
        setIdsRef(null);
        resType = "";
        combinedString = colonSeparated = false;
      }

      public Parameter(char type, String name, String idsRef)
      {
        this();
        setType(type);
        setName(name);
        setIdsRef(idsRef);
      }

      /** Returns the associated function, or {@code null} if not defined. */
      public Function getFunction() { return function; }
      /** Assigns a new function to the parameter object. */
      public void setFunction(Function function) { this.function = function; }

      /** Returns uppercased letter ([IOS] for triggers, [AIOPS] for actions). */
      public char getType() { return type; }
      /** Sets uppercased letter for parameter type, or '\0' for undefined type. */
      public void setType(char type) { this.type = (type == 0) ? type : Character.toUpperCase(type); }

      /** Returns parameter name (without trailing asterisk '*'). */
      public String getName() { return name; }
      /** Sets parameter name (without trailing asterisk '*'). */
      public void setName(String name)
      {
        if (name == null || name.length() == 0) {
          this.name = "";
        } else {
          int idx = name.indexOf('*');
          this.name = (idx < 0) ? name : name.substring(0, idx);
        }
      }

      /** Returns lowercased IDS reference. Returns empty string if unused. */
      public String getIdsRef() { return idsRef; }
      /** Sets lowercased IDS reference. */
      public void setIdsRef(String idsRef)
      {
        if (idsRef == null || idsRef.length() == 0) {
          this.idsRef = "";
        } else {
          this.idsRef = idsRef.toLowerCase(Locale.ENGLISH);
        }
      }

      /**
       * Returns a list of resource types referenced by the parameter.
       * Returns empty list if no resource type is referenced.
       */
      public String[] getResourceType()
      {
        if (resType != null && !resType.isEmpty()) {
          return resType.split(":");
        }
        return new String[]{};
      }

      /** Returns whether the parameter defines a global, local or area scope. */
      public boolean isNamespace()
      {
        return (type == TYPE_STRING && resType.contains("ARE"));
      }

      /**
       * Returns whether the current string argument has to be splitted from a string parameter.
       * Always returns {@code false} for non-string parameters.
       */
      public boolean isCombinedString()
      {
        return combinedString;
      }

      private void setCombinedString(boolean b)
      {
        combinedString = b;
      }

      /**
       * Returns whether the current string argument is included in a colon-separated string parameter.
       * Always returns {@code false} for non-string parameters.
       */
      public boolean isColonSeparatedString()
      {
        return colonSeparated;
      }

      private void setColonSeparatedString(boolean b)
      {
        colonSeparated = b;
      }

      @Override
      public int hashCode()
      {
        int hash = 7;
        hash = 31 * hash + type;
        hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
        hash = 31 * hash + ((idsRef == null) ? 0 : idsRef.hashCode());
        hash = 31 * hash + ((resType == null) ? 0 : resType.hashCode());
        hash = 31 * hash + Boolean.hashCode(combinedString);
        hash = 31 * hash + Boolean.hashCode(colonSeparated);
        return hash;
      }

      @Override
      public boolean equals(Object obj)
      {
        if (obj instanceof Parameter) {
          // Note: name and reference are irrelevant for comparison
          return (type == ((Parameter)obj).type);
        }
        return false;
      }

      @Override
      public String toString()
      {
        return toString(false);
      }

      public String toString(boolean verbose)
      {
        StringBuilder sb = new StringBuilder(32);
        sb.append(getType()).append(':').append(getName()).append('*').append(getIdsRef().toUpperCase(Locale.ENGLISH));
        if (verbose && resType != null &&
            !resType.isEmpty() &&
            !Character.isLowerCase(resType.charAt(0))) { // don't include internal keywords
          sb.append('[').append(resType).append(']');
        }
        return sb.toString();
      }

      private void setResourceType(String resType)
      {
        if (resType == null) {
          this.resType = "";
        } else {
          this.resType = resType;
        }
      }

      /**
       * Attempts to parse all parameter definitions available in the specified string.
       * @param param String containing zero, one or more parameter definitions.
       * @param id Function id.
       * @param funcType The function type.
       * @return A list of {@code Parameter} objects, or {@code null} on error.
       */
      private static List<Parameter> parse(String param, int id, FunctionType funcType)
      {
        List<Parameter> retVal = new ArrayList<>();

        if (param == null) {
          return retVal;
        }

        // preparations
        param = param.trim().replace('\t', ' ');
        int pos = param.indexOf('(');
        if (pos >= 0) {
          param = param.substring(pos + 1);
        }
        pos = param.indexOf(')');
        if (pos >= 0) {
          param = param.substring(0, pos);
        }

        final Pattern patParam    = Pattern.compile("([AIO0PST]):([0-9a-zA-Z_# ]+)\\*?([0-9a-zA-Z_#]*)");
        final Pattern patParamSep = Pattern.compile(" *, *");
        pos = 0;

        while (pos < param.length()) {
          Matcher m = patParam.matcher(param);
          if (m.find(pos) && m.groupCount() >= 2) {
            // parsing parameter
            pos = m.end();
            char type = 0;
            String name = null, idsRef = null;
            String s = m.group(1);
            if (s.length() == 1) {
              type = Character.toUpperCase(s.charAt(0));
              if (type == '0') {
                // fix for incorrect definitions
                type = 'O';
              }
            }
            s = m.group(2);
            if (s.length() > 0) {
              name = s.replace(" ", "");
            }
            if (m.groupCount() > 2) {
              s = m.group(3);
              if (s.length() > 0) {
                idsRef = s;
              }
            }
            if (type != 0 && name != null) {
              Parameter p = new Parameter(type, name, idsRef);
              ScriptInfo info = ScriptInfo.getInfo();
              p.setResourceType(info.getResType(funcType, id, p, retVal.size()));
              retVal.add(p);

              // preparing next parameter match
              m = patParamSep.matcher(param);
              if (m.find(pos)) {
                pos = m.end();
              } else {
                pos = param.length();
              }
            }
          } else {
            // no parameter found
            pos = param.length();
          }
        }

        // finalizations
        int numStrings = 0;
        for (final Parameter p: retVal) {
          ScriptInfo info = ScriptInfo.getInfo();
          if (p.getType() == Parameter.TYPE_STRING) {
            p.setCombinedString(info.isCombinedString(id, numStrings, retVal.size()));
            p.setColonSeparatedString(info.isColonSeparatedString(id, numStrings, retVal.size()));
            numStrings++;
          }
        }

        return retVal;
      }
    }
  }
}
