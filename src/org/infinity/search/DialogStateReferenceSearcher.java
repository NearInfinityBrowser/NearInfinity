// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;

import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.Transition;
import org.infinity.resource.key.ResourceEntry;

public final class DialogStateReferenceSearcher extends AbstractReferenceSearcher
{
  private final int targetStateNr;

  public DialogStateReferenceSearcher(ResourceEntry targetEntry, int stateNr, Component parent)
  {
    super(targetEntry, new String[]{"DLG"}, parent);
    targetStateNr = stateNr;
  }

  @Override
  void search(ResourceEntry entry, Resource resource)
  {
    DlgResource dlg = (DlgResource)resource;
    for (int i = 0; i < dlg.getFieldCount(); i++) {
      StructEntry structEntry = dlg.getField(i);
      if (structEntry instanceof Transition) {
        Transition transition = (Transition)structEntry;
        if (transition.getNextDialog().getResourceName().equalsIgnoreCase(targetEntry.toString()) &&
            transition.getNextDialogState() == targetStateNr)
          addHit(entry, null, transition);
      }
    }
  }
}

