// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

public interface SearchClient
{
  /**
   * Returns text of this object that will be matched with searched text.
   * When returns {@code null}, search finished
   *
   * @param nr Index of the searched object
   * @return {@code null} if index is invalid, text for match otherwise
   */
  String getText(int nr);

  /**
   * Called when match found.
   *
   * @param nr Index of the matched object
   */
  void hitFound(int nr);
}

