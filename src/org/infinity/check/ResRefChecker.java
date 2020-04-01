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

    final ResourceEntry spawnRef = ResourceFactory.getResourceEntry("SPAWNGRP.2DA");
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
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof AbstractStruct) {
        search(entry, (AbstractStruct)resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct)
  {
    for (final StructEntry e : struct.getFlatFields()) {
      if (!(e instanceof ResourceRef)) { continue; }

      final ResourceRef ref = (ResourceRef)e;
      final String resourceName = ref.getResourceName();

      //TODO: when getResourceName() will return null check on null instead of this
      if (resourceName.equalsIgnoreCase("None")) { continue; }

      // For spawn refs skip values from SPAWNGRP.2DA
      if (e instanceof SpawnResourceRef) {
        if (extraValues != null && extraValues.contains(ref.getText())) {
          continue;
        }
      } else {
        if (struct instanceof CreResource && resourceName.length() >= 3 && resourceName.substring(0, 3).equalsIgnoreCase("rnd")) {
          continue;
        }
      }

      final ResourceEntry resource = ResourceFactory.getResourceEntry(resourceName);
      if (!ref.isLegalEntry(resource)) {
        synchronized (hitFrame) {
          hitFrame.addHit(entry, entry.getSearchString(), ref);
        }
      }
    }
  }
}
