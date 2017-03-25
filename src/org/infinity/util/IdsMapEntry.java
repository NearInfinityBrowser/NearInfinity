// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.ArrayDeque;
import java.util.Iterator;

public class IdsMapEntry
{
  private final ArrayDeque<String> symbols = new ArrayDeque<>();
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
  public Iterator<String> getSymbols()
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
}
