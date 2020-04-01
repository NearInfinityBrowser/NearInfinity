// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

/** Represents resource that is just plain text in some format. */
public interface TextResource extends Resource
{
  /** Returns text representation of the resource. */
  String getText();

  /** Select text of specified {@code linenr}, optionally limited to matching {@code text}. */
  void highlightText(int linenr, String text);

  /**
   * Select all text from {@code startOfs} (inclusively) to {@code endOfs} (exclusively).
   * Preferred method when selecting text spanning multiple lines.
   */
  void highlightText(int startOfs, int endOfs);
}

