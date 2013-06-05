// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.util.StringResource;

import java.awt.*;

public final class WavReferenceSearcher extends AbstractReferenceSearcher
{
  public WavReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "ITM", "PRO",
                                    "SPL", "STO", "VEF", "VVC", "WED", "WMP"}, parent);
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
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof StringRef &&
               targetEntry.toString().equalsIgnoreCase(
                       StringResource.getResource(((StringRef)o).getValue()) + ".WAV"))
        addHit(entry, null, o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }
}

