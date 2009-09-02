// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ResourceRef;
import infinity.resource.*;
import infinity.resource.are.*;
import infinity.resource.are.Container;
import infinity.resource.bcs.BcsResource;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;

import java.awt.*;
import java.util.regex.Pattern;

public final class ScriptReferenceSearcher extends AbstractReferenceSearcher
{
  private String targetResRef;
  private Pattern cutscene;


  public ScriptReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "BCS", "CHR", "CRE"}, parent);
    this.targetResRef = targetEntry.getResourceName().substring(0,
                          targetEntry.getResourceName().indexOf('.'));
    this.cutscene = Pattern.compile("StartCutScene(\""
                       + targetResRef + "\")", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
  }

  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof BcsResource) {
      // TODO: avoid decompilation
      String text = ((BcsResource) resource).getText();
      if (cutscene.matcher(text).find()) {
        addHit(entry, null, null);
      }
    }
    else {
      searchStruct(entry, (AbstractStruct)resource);
    }
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

