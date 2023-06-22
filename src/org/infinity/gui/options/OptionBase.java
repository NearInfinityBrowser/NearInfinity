// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.util.Objects;

/** Common base for options and option containers. */
public abstract class OptionBase {
  /**
   * Placeholder identifier for OptionBase instances that don't require a unique id.
   * This identifier is not considered a match by id search functions.
   */
  public static final Object DEFAULT_ID = new Object();

  private final Object id;

  private OptionBase parent;
  private String label;

  protected OptionBase(Object id, String label) {
    this.id = Objects.requireNonNull(id);
    this.label = Objects.toString(label, "");
  }

  /** Returns the identifier of this object. */
  public Object getId() {
    return id;
  }

  /** Returns the parent option instance. {@code null} indicates the root option element. */
  public OptionBase getParent() {
    return parent;
  }

  /** Internally used to assign the parent {@code OptionBase} instance. */
  protected OptionBase setParent(OptionBase parent) {
    if (this.parent instanceof OptionContainerBase) {
      ((OptionContainerBase) this.parent).removeChild(this);
    }
    this.parent = parent;
    return this;
  }

  /** Returns a descriptive label string. Display depends on the specific {@code OptionBase} type. */
  public String getLabel() {
    return label;
  }

  /** Assigns a new label string to the {@code OptionBase}. */
  public OptionBase setLabel(String label) {
    this.label = Objects.toString(label, "");
    return this;
  }

  /** Returns the root {@code OptionBase} element of this options tree. */
  public OptionBase getRoot() {
    OptionBase retVal = this;
    while (retVal.parent != null) {
      retVal = retVal.parent;
    }
    return retVal;
  }

  /**
   * Searches for the first matching instance of the specified identifier and returns it.
   *
   * <p>
   * The search starts at the current node and searches all child nodes. Call this method from {@link #getRoot()}
   * to search all available nodes of this tree.
   * </p>
   *
   * @param id The identifier to find. {@code null} and {@link #DEFAULT_ID} will always return {@code null}.
   * @return A {@code OptionBase} instance matching the given identifier, or {@code null} if no matching instance
   * could be found.
   */
  public OptionBase findOption(Object id) {
    if (id == null || id == DEFAULT_ID) {
      return null;
    }
    return findOptionRecursive(this, id);
  }

  private OptionBase findOptionRecursive(OptionBase option, Object id) {
    if (option != null) {
      if (option.id.equals(id)) {
        return option;
      }
      if (option instanceof OptionContainerBase) {
        for (final OptionBase child : ((OptionContainerBase) option).getChildren()) {
          final OptionBase retVal = findOptionRecursive(child, id);
          if (retVal != null) {
            return retVal;
          }
        }
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return label;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    OptionBase other = (OptionBase) obj;
    return Objects.equals(id, other.id) && Objects.equals(label, other.label);
  }
}