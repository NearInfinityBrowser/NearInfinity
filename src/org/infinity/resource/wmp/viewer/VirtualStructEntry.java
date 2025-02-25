// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

/**
 * Common base class for table-based worldmap area definitions that provides a minimal {@link StructEntry}
 * implementation.
 */
public abstract class VirtualStructEntry extends PlainTextResource implements StructEntry {
  private static final String EMPTY_TABLE =
      "2DA V1.0\n*\nAREA SCRIPT FLAGS ICON LOCX LOCY LABEL NAME LINK_2DA N_LINKS E_LINKS S_LINKS W_LINKS LINKS_TO\n";
  protected static final ResourceEntry EMPTY_RESOURCE =
      new BufferedResourceEntry(ByteBuffer.wrap(EMPTY_TABLE.getBytes()), "NONE.2DA");

  protected VirtualStructEntry(ResourceEntry entry) throws Exception {
    super(Objects.nonNull(entry) ? entry : EMPTY_RESOURCE);
  }

  /** Returns whether the class instance holds an empty placeholder entry. */
  public boolean isEmpty() {
    return (getResourceEntry() == EMPTY_RESOURCE);
  }

  @Override
  public StructEntry clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  @Override
  public int compareTo(StructEntry o) {
    return (o == this) ? 1 : 0;
  }

  @Override
  public void write(OutputStream os) throws IOException {
    throw new IOException("Unsupported operation");
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    return (getResourceEntry() != null) ? (int) getResourceEntry().getResourceSize() : 0;
  }

  @Override
  public void copyNameAndOffset(StructEntry fromEntry) {
  }

  @Override
  public String getName() {
    return (getResourceEntry() != null) ? getResourceEntry().getResourceName() : "NONE";
  }

  @Override
  public void setName(String newName) {
  }

  @Override
  public int getOffset() {
    return 0;
  }

  @Override
  public AbstractStruct getParent() {
    return null;
  }

  @Override
  public int getSize() {
    return (getResourceEntry() != null) ? (int) getResourceEntry().getResourceSize() : 0;
  }

  @Override
  public ByteBuffer getDataBuffer() {
    try {
      if (getResourceEntry() != null) {
        getResourceEntry().getResourceBuffer();
      }
    } catch (Exception e) {
    }
    return null;
  }

  @Override
  public List<StructEntry> getStructChain() {
    final List<StructEntry> list = new ArrayList<>();
    list.add(this);
    return list;
  }

  @Override
  public void setOffset(int newoffset) {
  }

  @Override
  public void setParent(AbstractStruct parent) {
  }

  @Override
  public int hashCode() {
    return getResourceEntry().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj instanceof VirtualStructEntry) {
      final VirtualStructEntry vse = (VirtualStructEntry)obj;
      return getResourceEntry().equals(vse.getResourceEntry());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    return getName();
  }
}