// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

public interface SearchClient
{
  String getText(int nr);

  void hitFound(int nr);
}

