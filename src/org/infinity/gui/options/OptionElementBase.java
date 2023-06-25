// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.infinity.AppOption;

/**
 * Common base class for individual options the user can specify.
 * <p>
 * The parent of an option must always be an {@code OptionGroup} instance.
 * </p>
 */
public abstract class OptionElementBase extends OptionBase {
  private final List<AppOption> secondaryOptions = new ArrayList<>();
  private final List<Component> uiComponents = new ArrayList<>();
  private final AppOption option;

  private String description;
  private boolean enabled;

  protected OptionElementBase(Object id, String label, String desc, AppOption option, AppOption... secondaryOptions) {
    super(id, label);
    this.option = option;
    this.secondaryOptions.addAll(Arrays.asList(secondaryOptions));
    this.description = Objects.toString(desc, "");
    this.enabled = true;
  }

  /** Returns the associated {@link AppOption} instance. */
  public AppOption getOption() {
    return option;
  }

  /** Returns a list of secondary {@link AppOption} instances that are related to the primary option. */
  public List<AppOption> getSecondaryOptions() {
    return Collections.unmodifiableList(secondaryOptions);
  }

  /** Returns a description string associated with the option. */
  public String getDescription() {
    return description;
  }

  /** Assigns a new description string to the option. */
  public OptionElementBase setDescription(String desc) {
    this.description = Objects.toString(desc, "");
    return this;
  }

  /** Returns whether the option is enabled. The "enabled" state defines user access of the option. */
  public boolean isEnabled() {
    return enabled;
  }

  /** Specifies the "enabled" state of the option. */
  public OptionElementBase setEnabled(boolean enable) {
    if (enable != this.enabled) {
      this.enabled = enable;
      setUiEnabled(enable);
    }
    return this;
  }

  /**
   * Sets the enabled state of the associated UI elements. The method is used internally by {@link #setEnabled(boolean)}.
   */
  protected OptionElementBase setUiEnabled(boolean enable) {
    uiComponents.forEach(c -> c.setEnabled(enable));
    return this;
  }

  @Override
  public OptionGroup getParent() {
    return (OptionGroup) super.getParent();
  }

  @Override
  protected OptionBase setParent(OptionBase parent) {
    if (parent == null || parent instanceof OptionGroup) {
      super.setParent(parent);
      return this;
    } else {
      throw new IllegalArgumentException("Argument of type OptionGroup expected");
    }
  }

  /** Returns a list of all UI {@link Component} instances associated with the Option. */
  public List<Component> getUiComponents() {
    return Collections.unmodifiableList(uiComponents);
  }

  /** Returns the number of available UI {@link Component} instances associated with the Option. */
  public int getUiComponentCount() {
    return uiComponents.size();
  }

  /** Returns the {@link Component} at the specified index. */
  public Component getUiComponent(int index) throws IndexOutOfBoundsException {
    return uiComponents.get(index);
  }

  /** Adds a new {@link Component} to the list of associated UI components. */
  protected OptionElementBase addUiComponent(Component comp) throws NullPointerException {
    if (comp != null) {
      uiComponents.add(comp);
    }
    return this;
  }

  /** Removes the UI {@link Component} at the specified index from the list of UI components. */
  protected OptionElementBase removeUiComponent(int index) throws IndexOutOfBoundsException {
    uiComponents.remove(index);
    return this;
  }

  /** Removes the specified UI {@link Component} from the list of UI components. */
  protected OptionElementBase removeUiComponent(Component comp) {
    if (comp != null) {
      uiComponents.remove(comp);
    }
    return this;
  }

  /**
   * This method is called right before the associated UI elements are initialized and can therefore be used
   * to update option properties.
   *
   * <p>
   * If there is no onInit function available then the value from the associated {@link AppOption} instance is
   * used to initialize the option value.
   * </p>
   */
  public abstract void fireOnInit();

  /**
   * This method is called after all UI components have been created and initialized, but before they are shown
   * on the dialog. It can be used to make final adjustments to the UI components.
   */
  public abstract void fireOnCreated();

  /**
   * This method is called right after the user accepted the changes made in the options dialog and can therefore
   * be used to evaluate the option properties and update the associated {@link AppOption} instance if available.
   *
   * <p>
   * If there is no onAccept function available then the associated {@link AppOption} instance is updated with the
   * current option value.
   * </p>
   */
  public abstract void fireOnAccept();
}