// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.Vector;

/**
 * A customized FocusTraversalPolicy class that uses a fixed list of components it will focus on.
 */
public class FixedFocusTraversalPolicy extends FocusTraversalPolicy
{
  private final Vector<Component> order = new Vector<>();
  private int defaultIndex;

  /**
   * Constructs a customized FocusTraversalPolicy class that uses a fixed list of components
   * it will focus on.
   * @param list The list of components to focus.
   */
  public FixedFocusTraversalPolicy(Component[] list)
  {
    this(list, null);
  }

  /**
   * Constructs a customized FocusTraversalPolicy class that uses a fixed list of components
   * it will focus on.
   * @param list The list of components to focus.
   * @param defaultComponent An optional default component. Defaults to the first component in
   *                         {@code list} if not specified.
   */
  public FixedFocusTraversalPolicy(Component[] list, Component defaultComponent)
  {
    super();
    setComponents(list);
    setDefaultComponent(defaultComponent);
  }

  /**
   * Initializes a new list of components that can be focused. Resets default component to the first entry.
   */
  public void setComponents(Component[] list)
  {
    if (list == null) {
      throw new NullPointerException();
    } else if (list.length == 0) {
      throw new IllegalArgumentException("No components specified");
    }
    order.clear();
    for (int i = 0; i < list.length; i++) {
      if (list[i] != null) {
        order.add(list[i]);
      }
    }
    defaultIndex = 0;

    if (order.isEmpty()) {
      throw new IllegalArgumentException("No components specified");
    }
  }

  /** Sets the default component to focus. The component has to be in the list of components
   *  specified either in the constructor or in {@link #setComponents(Component[])}. */
  public void setDefaultComponent(Component c)
  {
    if (c != null) {
      int idx = order.indexOf(c);
      if (idx >= 0) {
        defaultIndex = idx;
      } else {
        defaultIndex = 0;
      }
    } else {
      defaultIndex = 0;
    }
  }

  @Override
  public Component getComponentAfter(Container aContainer, Component aComponent)
  {
    int idx = (order.indexOf(aComponent) + 1) % order.size();
    return order.get(idx);
  }

  @Override
  public Component getComponentBefore(Container aContainer, Component aComponent)
  {
    int idx = order.indexOf(aComponent) - 1;
    if (idx < 0) idx = order.size() - 1;
    return order.get(idx);
  }

  @Override
  public Component getFirstComponent(Container aContainer)
  {
    return order.get(0);
  }

  @Override
  public Component getLastComponent(Container aContainer)
  {
    return order.get(order.size() - 1);
  }

  @Override
  public Component getDefaultComponent(Container aContainer)
  {
    return order.get(defaultIndex);
  }
}
