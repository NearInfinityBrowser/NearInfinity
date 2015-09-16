// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.are.Actor;
import infinity.resource.are.Container;
import infinity.resource.are.Door;
import infinity.resource.are.ITEPoint;
import infinity.resource.bcs.BcsResource;
import infinity.resource.cre.CreResource;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.key.ResourceEntry;
import infinity.resource.text.PlainTextResource;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ScriptReferenceSearcher extends AbstractReferenceSearcher
{
  private String targetResRef;
  private Pattern cutscene;


  public ScriptReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, new String[]{"ARE", "BCS", "CHR", "CRE", "DLG", "INI"}, parent);
    this.targetResRef = targetEntry.getResourceName().substring(0,
                          targetEntry.getResourceName().indexOf('.'));
    this.cutscene = Pattern.compile("StartCutScene(\""
                       + targetResRef + "\")", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof BcsResource) {
      // TODO: avoid decompilation
      String text = ((BcsResource) resource).getText();
      if (cutscene.matcher(text).find()) {
        addHit(entry, null, null);
      }
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, (PlainTextResource)resource);
    } else {
      searchStruct(entry, (AbstractStruct)resource);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString())) {
        ResourceRef ref = (ResourceRef)o;
        if (struct instanceof CreResource) {
          addHit(entry, entry.getSearchString(), ref);
        } else if (struct instanceof Actor) {
          addHit(entry, struct.getField(20).toString(), ref);
        } else {
          addHit(entry, null, ref);
        }
      }
      else if (o instanceof Actor ||
               o instanceof Container ||
               o instanceof Door ||
               o instanceof ITEPoint) {
        searchStruct(entry, (AbstractStruct)o);
      }
      else if (o instanceof AbstractCode) {
        String text = o.toString();
        if (cutscene.matcher(text).find()) {
          addHit(entry, o.getName(), o);
        }
      }
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text)
  {
    String name = getTargetEntry().getResourceName();
    int idx = name.lastIndexOf('.');
    if (idx > 0) {
      name = name.substring(0, idx);
    }
    Pattern p = Pattern.compile("\\b" + name + "\\b", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text.getText());
    if (m.find()) {
      addHit(entry, entry.getSearchString(), null);
    }
  }
}

