// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Mapping from several symbolic names to an integer and vice versa. Used for script
 * purposes and for mapping the file index to filename.
 */
public class IdsMapEntry implements Comparable<IdsMapEntry>, Iterable<String>
{
  /** Symbolic names that can be used in scripts for specifying {@link #id}. */
  private final ArrayDeque<String> symbols = new ArrayDeque<>();
  /** Value that used in compiled scripts. */
  private final long id;

  public IdsMapEntry(long id, String symbol)
  {
    this.id = id;
    addSymbol(symbol);
  }

  /** Returns the numeric value of the entry. */
  public long getID()
  {
    return id;
  }

  /** Returns number of available symbolic names. */
  public int getNumSymbols()
  {
    return symbols.size();
  }

  /** Returns the most recently added symbolic name. */
  public String getSymbol()
  {
    return symbols.peek();
  }

  /** Returns an iterator over the whole collection of available symbols. */
  @Override
  public Iterator<String> iterator()
  {
    return symbols.iterator();
  }

  /** Adds the specified symbolic name if it does not yet exist. */
  public void addSymbol(String symbol)
  {
    if (symbol == null) {
      throw new NullPointerException();
    }

    if (!symbol.isEmpty() && !symbols.contains(symbol)) {
      symbols.push(symbol);
    }
  }

  @Override
  public String toString()
  {
    return toString(getID(), getSymbol());
  }

  public static String toString(long id, String symbol)
  {
    return symbol + " - " + id;
  }

//--------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(IdsMapEntry o)
  {
    return toString().compareToIgnoreCase(o.toString());
  }

//--------------------- End Interface Comparable ---------------------
}
