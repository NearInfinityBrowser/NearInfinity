// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.util;

public final class IdsMapEntry implements Comparable
{
  private final String string;
  private final String parameters;
  private final long id;

  public IdsMapEntry(long id, String string, String parameters)
  {
    this.id = id;
    this.string = string;
    this.parameters = parameters;
  }

// --------------------- Begin Interface Comparable ---------------------

  public int compareTo(Object o)
  {
    return toString().compareToIgnoreCase(o.toString());
  }

// --------------------- End Interface Comparable ---------------------

  public String toString()
  {
    if (parameters == null)
      return string + " - " + id;
    return string + parameters + ") - " + id;
  }

  public long getID()
  {
    return id;
  }

  public String getParameters()
  {
    return parameters;
  }

  public String getString()
  {
    return string;
  }
}

