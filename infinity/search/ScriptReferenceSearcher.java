// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ResourceRef;
import infinity.resource.*;
import infinity.resource.are.*;
import infinity.resource.are.Container;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;

import java.awt.*;

public final class ScriptReferenceSearcher extends AbstractReferenceSearcher
{
  public ScriptReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "CHR", "CRE"}, parent);
  }

  protected void search(ResourceEntry entry, Resource resource)
  {
    searchStruct(entry, (AbstractStruct)resource);
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getRowCount(); i++) {
      StructEntry o = struct.getStructEntryAt(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString())) {
        ResourceRef ref = (ResourceRef)o;
        if (struct instanceof CreResource)
          addHit(entry, entry.getSearchString(), ref);
        else if (struct instanceof Actor)
          addHit(entry, struct.getStructEntryAt(20).toString(), ref);
        else
          addHit(entry, null, ref);
      }
      else if (o instanceof Actor ||
               o instanceof Container ||
               o instanceof Door ||
               o instanceof ITEPoint)
        searchStruct(entry, (AbstractStruct)o);
    }
  }
}

