// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import java.awt.Component;

/**
 * References to the resource can be search for by {@code ReferenceSearcher}.
 */
public interface Referenceable
{
  /** Returns whether {@code ReferenceSearcher} is available for the current resource. */
  boolean isReferenceable();

  /** Invokes the reference search dialog for the resource. */
  void searchReferences(Component parent);
}
