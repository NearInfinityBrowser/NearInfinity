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

/**
 * Performs search of the specified dialogue state in other dialogues (including
 * dialogue in which state defined).
 */
public final class DialogStateReferenceSearcher extends AbstractReferenceSearcher
{
  /** Searched state. Together with {@link #targetEntry} makes subject to search. */
  private final int targetStateNr;

  /**
   * Creates finder that searches dialogue state (NPC reply) in the other dialogues.
   * Performs search only in the {@link DlgResource DLG} files.
   *
   * @param searchedDialog Pointer to the resource that contains searched state
   * @param searchedState Searched state number -- the NPC reply
   * @param parent GUI component that will be parent for results window
   */
  public DialogStateReferenceSearcher(ResourceEntry searchedDialog, int searchedState, Component parent)
  {
    super(searchedDialog, new String[]{"DLG"}, parent);
    targetStateNr = searchedState;
  }

  @Override
  void search(ResourceEntry entry, Resource resource)
  {
    final DlgResource dlg = (DlgResource)resource;
    final String name = targetEntry.getResourceName();
    for (int i = 0; i < dlg.getFieldCount(); i++) {
      StructEntry structEntry = dlg.getField(i);
      if (structEntry instanceof Transition) {
        Transition transition = (Transition)structEntry;
        if (transition.getNextDialog().getResourceName().equalsIgnoreCase(name) &&
            transition.getNextDialogState() == targetStateNr)
          addHit(entry, null, transition);
      }
    }
  }
}

