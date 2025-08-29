// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.datatype.IsTextual;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.Actor;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.tinylog.Logger;

/**
 * Performs search usages of the specified script in the {@link AreResource area}, {@link BcsResource script},
 * {@link CreResource characters and creatures}, {@link DlgResource dialogues} and the ini files.
 */
public final class ScriptReferenceSearcher extends AbstractReferenceSearcher {
  public ScriptReferenceSearcher(ResourceEntry bcsScript, Component parent) {
    super(bcsScript, getSupportedTypes(), parent);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource) {
    if (resource instanceof BcsResource) {
      searchScript(entry, (BcsResource) resource);
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, ((PlainTextResource) resource).getText());
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct) resource);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct) {
    final String name = targetEntry.getResourceName();
    for (final StructEntry o : struct.getFields()) {
      if (o instanceof ResourceRef && ((ResourceRef) o).getResourceName().equalsIgnoreCase(name)) {
        if (struct instanceof CreResource) {
          addHit(entry, entry.getSearchString(), o);
        } else if (struct instanceof Actor) {
          final IsTextual actorName = (IsTextual) struct.getAttribute(Actor.ARE_ACTOR_NAME);
          addHit(entry, actorName.getText(), o);
        } else {
          addHit(entry, null, o);
        }
      } else if (o instanceof AbstractCode) {
        searchScript(entry, ((AbstractCode) o).getText(), o);
      } else if (o instanceof AbstractStruct) {
        searchStruct(entry, (AbstractStruct) o);
      }
    }
  }

  private void searchText(ResourceEntry entry, String text) {
    final String name = targetEntry.getResourceRef();
    registerTextHits(entry, text, Pattern.compile("\\b" + name + "\\b", Pattern.CASE_INSENSITIVE));
  }

  private void searchScript(ResourceEntry entry, String script, StructEntry ref) {
    String name = targetEntry.getResourceRef();
    final Pattern p = Pattern.compile("\"" + name + "\"", Pattern.CASE_INSENSITIVE);
    final Matcher m = p.matcher(script);
    if (m.find()) {
      addHit(entry, entry.getSearchString(), ref);
    }
  }

  private void searchScript(ResourceEntry entry, BcsResource bcsFile) {
    final Decompiler decompiler = new Decompiler(bcsFile.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    try {
      final String script = decompiler.decompile();
      if (decompiler.getResourcesUsed().contains(targetEntry)) {
        registerTextHits(entry, script,
            Pattern.compile('"' + targetEntry.getResourceRef() + '"', Pattern.CASE_INSENSITIVE));
      }
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  /**
   * A helper method that returns an array of resource types for the reference search that are supported by the current
   * game.
   */
  private static String[] getSupportedTypes() {
    final ArrayList<String> list = new ArrayList<>();
    final String[] extensions = { "2DA", "ARE", "BCS", "CHR", "CRE", "DLG", "EFF", "GAM", "INI", "ITM", "SPL" };
    final List<String> availableExtensions = Arrays.asList(Profile.getAvailableResourceTypes());
    for (final String ext : extensions) {
      if (availableExtensions.contains(ext)) {
        list.add(ext);
      }
    }
    return list.toArray(new String[list.size()]);
  }
}
