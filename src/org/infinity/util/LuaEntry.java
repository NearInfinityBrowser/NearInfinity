// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a single Lua element.
 */
public class LuaEntry
{
  public ArrayList<LuaEntry> children; // for complex types (nested list of LuaEntry items)
  public String key; // index or name
  public Object value; // For basic types (either null, Integer, Boolean or String)

  /** Determine key from parent entry. */
  public LuaEntry(LuaEntry parent)
  {
    this((parent != null && parent.children != null) ? parent.children.size() : 0);
  }

  /** Use specified index as key. */
  public LuaEntry(int key)
  {
    this(Integer.toString(key));
  }

  /** Use specified name as key. */
  public LuaEntry(String key)
  {
    this.key = (key != null && !key.isEmpty()) ? key : "0";
    this.value = null;
    this.children = null;
  }

  /**
   * Return child LuaEntry matching the specified key.
   *
   * @param key
   *          The case-sensitive key string.
   * @param recursive
   *          Whether the search should be done recursively un subchilds as well.
   * @return The first LuaEntry item matching the specified parameter, {@code null} otherwise.
   */
  public LuaEntry findChild(String key, boolean recursive)
  {
    if (children != null) {
      for (LuaEntry le : children) {
        if (le.key.equals(key))
          return le;
        LuaEntry retVal = le.findChild(key, recursive);
        if (retVal != null)
          return retVal;
      }
    }
    return null;
  }

  @Override
  public String toString()
  {
    return toString(0);
  }

  private String toString(int indent)
  {
    // indentation
    char[] chars = new char[indent];
    Arrays.fill(chars, ' ');
    String space = new String(chars);

    StringBuilder sb = new StringBuilder();
    sb.append(space);

    if (!key.isEmpty() && key.charAt(0) < '0' || key.charAt(0) > '9') {
      sb.append(String.format("%s = ", key));
    }
    if (value != null) {
      if (value instanceof Boolean) {
        sb.append(Boolean.toString((Boolean) value));
      } else if (value instanceof Integer) {
        sb.append(Integer.toString((Integer) value));
      } else {
        sb.append('"').append(value.toString()).append('"');
      }
    } else if (children != null) {
      sb.append("{\n");
      for (LuaEntry luaEntry : children) {
        sb.append(luaEntry.toString(indent + 4)).append(",\n");
      }
      sb.append(space);
      sb.append("}\n");
    } else {
      sb.append("null");
    }
    return sb.toString();
  }
}
