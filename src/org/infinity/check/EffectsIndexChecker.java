// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import org.infinity.NearInfinity;
import org.infinity.datatype.IsNumeric;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.spl.SplResource;
import org.infinity.search.ReferenceHitFrame;

public class EffectsIndexChecker extends AbstractChecker
{
  /** Window with check results. */
  private final ReferenceHitFrame hitFrame;

  public EffectsIndexChecker()
  {
    super("Effects Index Checker", "EffectsIndexChecker",
          new String[]{"ITM", "SPL"});
    hitFrame = new ReferenceHitFrame("Mis-indexed Effects", NearInfinity.getInstance());
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
    final int numGlobalEffects = ((IsNumeric) struct.getAttribute(SplResource.SPL_NUM_GLOBAL_EFFECTS)).getValue();
    int expectedEffectsIndex = numGlobalEffects;
    for (final StructEntry e : struct.getFields()) {
      if (e instanceof AbstractAbility) {
        final AbstractAbility abil = (AbstractAbility) e;
        final int effectsIndex = ((IsNumeric) abil.getAttribute(AbstractAbility.ABILITY_FIRST_EFFECT_INDEX)).getValue();
        if (effectsIndex != expectedEffectsIndex) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), abil);
          }
        }
        expectedEffectsIndex += abil.getEffectsCount();
      }
    }
  }
}
