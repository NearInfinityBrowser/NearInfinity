// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.datatype.Readable;

public interface StructEntry extends Comparable<StructEntry>, Cloneable, Writeable, Readable
{
  Object clone() throws CloneNotSupportedException;

  void copyNameAndOffset(StructEntry fromEntry);

  String getName();

  int getOffset();

  StructEntry getParent();

  int getSize();

  /** Attempts to retrieve the data of this datatype and returns it as ByteBuffer object. */
  ByteBuffer getDataBuffer();

  /** Creates a list of StructEntry object, starting from root up to this object. */
  List<StructEntry> getStructChain();

  void setOffset(int newoffset);

  void setParent(StructEntry parent);
}

