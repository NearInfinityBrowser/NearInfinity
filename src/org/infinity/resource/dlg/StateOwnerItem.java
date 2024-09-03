// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.util.Enumeration;

/** Auxiliary class, being the parent for states, for a type safety. */
public abstract class StateOwnerItem extends ItemBase {
  @Override
  public abstract StateItem getChildAt(int childIndex);

  @Override
  public abstract Enumeration<? extends StateItem> children();
}
