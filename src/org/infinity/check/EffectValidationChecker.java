// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.util.ArrayList;
import java.util.List;

import org.infinity.NearInfinity;
import org.infinity.datatype.EffectType;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.effects.BaseOpcode;
import org.infinity.resource.effects.DefaultOpcode;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceHitFrame;

/**
 * Checks for invalid effect opcodes.
 */
public class EffectValidationChecker extends AbstractChecker {
  /** Window with check results. */
  private final ReferenceHitFrame hitFrame;

  public EffectValidationChecker() {
    super("Effects Validation Checker", getSupportedResourceTypes());
    hitFrame = new ReferenceHitFrame("Invalid Effect Opcodes", NearInfinity.getInstance());
  }

  @Override
  public void run() {
    if (runCheck(getFiles())) {
      hitFrame.close();
    } else {
      hitFrame.setVisible(true);
    }
  }

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof AbstractStruct) {
        search(entry, (AbstractStruct) resource);
      }
      advanceProgress();
    };
  }

  private void search(ResourceEntry entry, AbstractStruct struct) {
    for (final StructEntry field : struct.getFlatFields()) {
      if (field instanceof EffectType) {
        int value = ((EffectType) field).getValue();
        final BaseOpcode opcode = BaseOpcode.getOpcode(value);
        if (opcode instanceof DefaultOpcode) {
          synchronized (hitFrame) {
            hitFrame.addHit(entry, entry.getSearchString(), field);
          }
        }
      }
    }
  }

  private static String[] getSupportedResourceTypes() {
    final List<String> retVal = new ArrayList<>();
    for (final String type : new String[] { "CRE", "EFF", "ITM", "SPL" }) {
      if (Profile.isResourceTypeSupported(type)) {
        retVal.add(type);
      }
    }
    return retVal.toArray(new String[0]);
  }
}
