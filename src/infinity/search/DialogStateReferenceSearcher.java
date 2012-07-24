// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.dlg.DlgResource;
import infinity.resource.dlg.Transition;
import infinity.resource.key.ResourceEntry;

import java.awt.*;

public final class DialogStateReferenceSearcher extends AbstractReferenceSearcher
{
  private final int targetStateNr;

  public DialogStateReferenceSearcher(ResourceEntry targetEntry, int stateNr, Component parent)
  {
    super(targetEntry, new String[]{"DLG"}, parent);
    targetStateNr = stateNr;
  }

  void search(ResourceEntry entry, Resource resource)
  {
    DlgResource dlg = (DlgResource)resource;
    for (int i = 0; i < dlg.getRowCount(); i++) {
      StructEntry structEntry = dlg.getStructEntryAt(i);
      if (structEntry instanceof Transition) {
        Transition transition = (Transition)structEntry;
        if (transition.getNextDialog().getResourceName().equalsIgnoreCase(targetEntry.toString()) &&
            transition.getNextDialogState() == targetStateNr)
          addHit(entry, null, transition);
      }
    }
  }
}

