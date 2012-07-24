// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

interface HasVertices
{
  void readVertices(byte buffer[], int offset) throws Exception;

  int updateVertices(int offset, int number);
}

