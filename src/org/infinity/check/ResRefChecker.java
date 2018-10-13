// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.util.List;
import org.infinity.NearInfinity;

import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SpawnResourceRef;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.ReferenceHitFrame;

public final class ResRefChecker extends AbstractChecker
{
  private static final String[] FILETYPES = {"ARE", "CHR", "CHU", "CRE", "DLG", "EFF", "GAM", "ITM", "PRO",
                                             "SPL", "STO", "VEF", "VVC", "WED", "WMP"};
  /** Window with check results. */
  private final ReferenceHitFrame hitFrame;
  private List<String> extraValues;

  public ResRefChecker()
  {
    super("ResRef Checker", "ResRefChecker", FILETYPES);
    hitFrame = new ReferenceHitFrame("Illegal ResourceRefs", NearInfinity.getInstance());

    ResourceEntry spawnRef = ResourceFactory.getResourceEntry("SPAWNGRP.2DA");
    if (spawnRef != null) {
      PlainTextResource spawn = (PlainTextResource)ResourceFactory.getResource(spawnRef);
      extraValues = spawn.extract2DAHeaders();
    }
  }

// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    if (runCheck(files)) {
      hitFrame.close();
    } else {
      hitFrame.setVisible(true);
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        search(entry, (AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    for (StructEntry e : struct.getFlatList()) {
      if (e instanceof SpawnResourceRef) {
        final SpawnResourceRef ref = (SpawnResourceRef)e;
        final String resourceName = ref.getResourceName();
        if (resourceName.equalsIgnoreCase("None")) {//FIXME: find reason fo this check and remove it
          // ignore
        } else if (extraValues != null && extraValues.contains(ref.getText())) {
          // ignore
        } else if (!ResourceFactory.resourceExists(resourceName)) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), ref);
          }
        } else if (!ref.isLegalEntry(ResourceFactory.getResourceEntry(resourceName))) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), ref);
          }
        }
      }
      else if (e instanceof ResourceRef) {
        final ResourceRef ref = (ResourceRef)e;
        final String resourceName = ref.getResourceName();
        if (resourceName.equalsIgnoreCase("None")) {//FIXME: find reason fo this check and remove it
          // ignore
        } else if (struct instanceof CreResource && resourceName.substring(0, 3).equalsIgnoreCase("rnd")) {
          // ignore
        } else if (!ResourceFactory.resourceExists(resourceName)) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), ref);
          }
        } else if (!ref.isLegalEntry(ResourceFactory.getResourceEntry(resourceName))) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), ref);
          }
        }
      }
    }
  }
}
