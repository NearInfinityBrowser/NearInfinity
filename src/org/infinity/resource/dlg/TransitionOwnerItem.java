// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.Enumeration;

/** Auxiliary class, being the parent for transitions, for a type safety. */
public abstract class TransitionOwnerItem extends ItemBase implements Iterable<TransitionItem> {
  @Override
  public abstract TransitionItem getChildAt(int childIndex);

  @Override
  public abstract Enumeration<? extends TransitionItem> children();
}
