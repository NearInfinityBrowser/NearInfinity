// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;

import org.infinity.resource.Resource;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.State;
import org.infinity.resource.key.ResourceEntry;

/**
 * Performs search of the specified dialogue state in other dialogues (including
 * dialogue in which state defined).
 */
public final class DialogStateReferenceSearcher extends AbstractReferenceSearcher
{
  /** Searched state. Together with {@link #targetEntry} makes subject to search. */
  private final State targetState;

  /**
   * Creates finder that searches dialogue state (NPC reply) in the other dialogues.
   * Performs search only in the {@link DlgResource DLG} files.
   *
   * @param searchedDialog Pointer to the resource that contains searched state
   * @param searchedState Searched state number -- the NPC reply
   * @param parent GUI component that will be parent for results window
   */
  public DialogStateReferenceSearcher(ResourceEntry searchedDialog, State searchedState, Component parent)
  {
    super(searchedDialog, new String[]{"DLG"}, parent);
    targetState = searchedState;
  }

  @Override
  void search(ResourceEntry entry, Resource resource)
  {
    // If resource has DLG extension but not DLG resource
    if (!(resource instanceof DlgResource)) return;

    final DlgResource dlg = (DlgResource)resource;
    dlg.findUsages(targetState, t -> addHit(entry, null, t));
  }
}
