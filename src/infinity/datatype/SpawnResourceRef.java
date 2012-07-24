// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.key.ResourceEntry;
import infinity.resource.ResourceFactory;
import infinity.resource.other.PlainTextResource;

import java.util.List;

public final class SpawnResourceRef extends ResourceRef
{
  public SpawnResourceRef(byte h_buffer[], int offset, String name)
  {
    super(h_buffer, offset, name, "CRE");
  }

  void addExtraEntries(List<ResourceRefEntry> entries)
  {
    ResourceEntry spawnRef = ResourceFactory.getInstance().getResourceEntry("SPAWNGRP.2DA");
    if (spawnRef != null) {
      PlainTextResource spawn = (PlainTextResource)ResourceFactory.getResource(spawnRef);
      List<String> headers = spawn.extract2DAHeaders();
      for (int i = 0; i < headers.size(); i++) {
        entries.add(new ResourceRefEntry(headers.get(i)));
      }
    }
  }

  public String getResName()
  {
    return resname;
  }
}

