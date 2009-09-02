// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

public interface TextResource extends Resource
{
  String getText();
  void highlightText(int linenr, String text);
}

