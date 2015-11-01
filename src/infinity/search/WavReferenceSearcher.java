// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ResourceRef;
import infinity.datatype.StringRef;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.resource.text.PlainTextResource;
import infinity.util.StringResource;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WavReferenceSearcher extends AbstractReferenceSearcher
{
  public WavReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, AbstractReferenceSearcher.FILE_TYPES, parent);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, (PlainTextResource)resource);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof StringRef &&
               targetEntry.toString().equalsIgnoreCase(
                       StringResource.getWavResource(((StringRef)o).getValue()) + ".WAV"))
        addHit(entry, null, o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text)
  {
    String name = getTargetEntry().getResourceName();
    int idx = name.lastIndexOf('.');
    if (idx > 0) {
      String nameBase = name.substring(0, idx);
      Pattern p = Pattern.compile("\\b" + nameBase + "\\b", Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(text.getText());
      if (m.find()) {
        addHit(entry, null, null);
      }
    }
  }
}

