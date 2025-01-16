// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.regex.Pattern;

import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.StringTable;
import org.tinylog.Logger;

public final class WavReferenceSearcher extends AbstractReferenceSearcher {
  public WavReferenceSearcher(ResourceEntry targetEntry, Component parent) {
    super(targetEntry, AbstractReferenceSearcher.FILE_TYPES, parent);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource) {
    if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct) resource);
    } else if (resource instanceof BcsResource) {
      searchScript(entry, (BcsResource) resource);
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, (PlainTextResource) resource);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct) {
    for (final StructEntry o : struct.getFields()) {
      if (o instanceof ResourceRef
          && ((ResourceRef) o).getResourceName().equalsIgnoreCase(targetEntry.getResourceName())) {
        addHit(entry, entry.getSearchString(), o);
      } else if (o instanceof StringRef && !StringTable.getSoundResource(((StringRef) o).getValue()).isEmpty()
          && targetEntry.getResourceName()
              .equalsIgnoreCase(StringTable.getSoundResource(((StringRef) o).getValue()) + ".WAV")) {
        addHit(entry, null, o);
      } else if (o instanceof AbstractStruct) {
        searchStruct(entry, (AbstractStruct) o);
      }
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text) {
    final String nameBase = getTargetEntry().getResourceRef();
    registerTextHits(entry, text.getText(), Pattern.compile("\\b" + nameBase + "\\b", Pattern.CASE_INSENSITIVE));
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
}
