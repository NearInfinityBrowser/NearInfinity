// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.infinity.datatype.IsTextual;
import org.infinity.resource.AbstractVariable;

/**
 * Container with variables, that allows do fast lookup variables by their name.
 *
 * @author Mingun
 */
public class Variables
{
  /** Storage of variables. */
  private final HashMap<String, AbstractVariable> vars = new HashMap<>();

  /**
   * Appends specified variable to the variable set. If variable with such
   * {@link AbstractVariable#VAR_NAME name} already registered, replaces it.
   *
   * @param var Variable to add
   */
  public void add(AbstractVariable var)
  {
    vars.put(name(var), var);
  }
  /**
   * Removes specified variable from the set.
   *
   * @param var Variable to remove
   */
  public void remove(AbstractVariable var)
  {
    vars.remove(name(var));
  }
  /**
   * Returns integer variable value with specified name. If such variable is not
   * exist or not storing integer value, {@code 0} is returned.
   *
   * @param name Name of variable to get
   * @return Integer variable value
   */
  public int getInt(String name)
  {
    final AbstractVariable var = vars.get(key(name));
    if (var != null) {
      final Integer value = (Integer)var.getValue(AbstractVariable.Type.Integer);
      if (value != null) {
        return value.intValue();
      }
    }
    return 0;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder("Variables{\n");
    for (final Map.Entry<String, AbstractVariable> e : vars.entrySet()) {
      sb.append("  ").append(e.getKey()).append('=').append(e.getValue().getValue()).append('\n');
    }
    return sb.append('}').toString();
  }

  private static String name(AbstractVariable var)
  {
    final IsTextual attr = (IsTextual)var.getAttribute(AbstractVariable.VAR_NAME, false);
    return key(attr.getText());
  }
  private static String key(String name) { return name.toUpperCase(Locale.ENGLISH); }
}
