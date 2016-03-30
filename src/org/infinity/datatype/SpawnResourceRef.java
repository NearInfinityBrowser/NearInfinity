// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;
import java.util.List;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

public final class SpawnResourceRef extends ResourceRef
{
  public SpawnResourceRef(ByteBuffer h_buffer, int offset, String name)
  {
    this(null, h_buffer, offset, name);
  }

  public SpawnResourceRef(StructEntry parent, ByteBuffer h_buffer, int offset, String name)
  {
    super(parent, h_buffer, offset, name, "CRE");
  }

  @Override
  void addExtraEntries(List<Object> entries)
  {
    ResourceEntry spawnRef = ResourceFactory.getResourceEntry("SPAWNGRP.2DA");
    if (spawnRef != null) {
      PlainTextResource spawn = (PlainTextResource)ResourceFactory.getResource(spawnRef);
      List<String> headers = spawn.extract2DAHeaders();
      for (int i = 0; i < headers.size(); i++) {
        entries.add(new ResourceRefEntry(headers.get(i)));
      }
    }
  }
}

