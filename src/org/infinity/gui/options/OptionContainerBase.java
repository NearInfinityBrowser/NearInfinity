// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Base class for option containers. */
public abstract class OptionContainerBase extends OptionBase {
  private final List<OptionBase> children = new ArrayList<>();

  protected OptionContainerBase(Object id, String label, OptionBase... children) {
    super(id, label);
    for (int i = 0; i < children.length; i++) {
      addChild(children[i]);
    }
  }

  /** Returns whether this container instance is an anonymous default container. */
  public boolean isDefault() {
    return getLabel().isEmpty();
  }

  /** Returns a list of first-level children objects. */
  public List<OptionBase> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /** Returns the number of available first-level children objects. */
  public int getChildCount() {
    return children.size();
  }

  /** Returns the child at the specified index. */
  public OptionBase getChild(int index) throws IndexOutOfBoundsException {
    return children.get(index);
  }

  /** Adds a new {@code OptionBase} instance to the list of children. */
  public OptionContainerBase addChild(OptionBase child) throws NullPointerException {
    children.add(Objects.requireNonNull(child.setParent(this)));
    return this;
  }

  /** Inserts a new {@code OptionBase} instance at the specified index to the list of children. */
  public OptionContainerBase insertChild(int index, OptionBase child)
      throws IndexOutOfBoundsException, NullPointerException {
    children.add(index, Objects.requireNonNull(child).setParent(this));
    return this;
  }

  /** Removes the child at the specified index from the list of children. */
  public OptionContainerBase removeChild(int index) throws IndexOutOfBoundsException {
    children.remove(index).setParent(null);
    return this;
  }

  /** Removes the specified child from the list of children. */
  public OptionContainerBase removeChild(OptionBase child) {
    if (child != null && children.remove(child)) {
      child.setParent(null);
    }
    return this;
  }

  /** Removes all children from the list of chilren. */
  public OptionContainerBase clearChildren() {
    for (int idx = children.size() - 1; idx >= 0; idx--) {
      removeChild(idx);
    }
    return this;
  }
}