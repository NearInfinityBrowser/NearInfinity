// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.infinity.datatype.IsTextual;

import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Container;
import org.infinity.resource.are.Door;
import org.infinity.resource.are.ITEPoint;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;

/**
 * Performs search usages of the specified script in the {@link AreResource area},
 * {@link BcsResource script}, {@link CreResource characters and creatures},
 * {@link DlgResource dialogues} and the ini files.
 */
public final class ScriptReferenceSearcher extends AbstractReferenceSearcher
{
  public ScriptReferenceSearcher(ResourceEntry bcsScript, Component parent)
  {
    super(bcsScript, new String[]{"ARE", "BCS", "CHR", "CRE", "DLG", "INI"}, parent);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof BcsResource) {
      // passing raw bytecode to improve performance
      searchScript(entry, ((BcsResource)resource).getCode(), null);
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, ((PlainTextResource)resource).getText());
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    final String name = targetEntry.getResourceName();
    for (final StructEntry o : struct.getFields()) {
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(name)) {
        if (struct instanceof CreResource) {
          addHit(entry, entry.getSearchString(), o);
        } else if (struct instanceof Actor) {
          final IsTextual actorName = (IsTextual)struct.getAttribute(Actor.ARE_ACTOR_NAME);
          addHit(entry, actorName.getText(), o);
        } else {
          addHit(entry, null, o);
        }
      }
      else if (o instanceof Actor ||
               o instanceof Container ||
               o instanceof Door ||
               o instanceof ITEPoint) {
        searchStruct(entry, (AbstractStruct)o);
      }
      else if (o instanceof AbstractCode) {
        searchScript(entry, ((AbstractCode)o).getText(), o);
      }
    }
  }

  private void searchText(ResourceEntry entry, String text)
  {
    String name = targetEntry.getResourceRef();
    final Pattern p = Pattern.compile("\\b" + name + "\\b", Pattern.CASE_INSENSITIVE);
    final Matcher m = p.matcher(text);
    if (m.find()) {
      addHit(entry, entry.getSearchString(), null);
    }
  }

  private void searchScript(ResourceEntry entry, String script, StructEntry ref)
  {
    String name = targetEntry.getResourceRef();
    final Pattern p = Pattern.compile("\"" + name + "\"", Pattern.CASE_INSENSITIVE);
    final Matcher m = p.matcher(script);
    if (m.find()) {
      addHit(entry, entry.getSearchString(), ref);
    }
  }
}
