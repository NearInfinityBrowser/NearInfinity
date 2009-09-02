// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

public interface StructEntry extends Comparable<StructEntry>, Cloneable, Writeable
{
  Object clone() throws CloneNotSupportedException;

  void copyNameAndOffset(StructEntry fromEntry);
  String getName();

  int getOffset();

  int getSize();

  void setOffset(int newoffset);
}

