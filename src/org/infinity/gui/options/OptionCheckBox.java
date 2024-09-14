// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JCheckBox;

import org.infinity.AppOption;

/**
 * Definition of a boolean option.
 * <p>
 * This type is represented by a {@code JCheckBox} element.
 * </p>
 */
public class OptionCheckBox extends OptionElementBase {
  private boolean value;

  private JCheckBox checkBox;

  private Consumer<OptionCheckBox> onInit;
  private Consumer<OptionCheckBox> onCreated;
  private Consumer<OptionCheckBox> onAccept;
  private Function<OptionCheckBox, Boolean> onAction;

  public static OptionCheckBox create(Object id, String label, String desc) {
    return new OptionCheckBox(id, label, desc, false, null);
  }

  public static OptionCheckBox create(Object id, String label, String desc, AppOption option) {
    return new OptionCheckBox(id, label, desc, false, option);
  }

  public static OptionCheckBox create(Object id, String label, String desc, boolean value) {
    return new OptionCheckBox(id, label, desc, value, null);
  }

  /**
   * Creates a new OptionCheckBox with the specified parameters.
   *
   * @param id An identifier for this option element.
   * @param label A short descriptive string used as checkbox label.
   * @param desc A more detailed description string for display in an info panel.
   * @param value Initial value for this option. This value can be updated by a specified onInit function if available,
   * or is automatically updated by the specified {@code AppOption} value, otherwise.
   * <b>Note:</b> You have to use the {@code onInit} function if the AppOption value cannot be directly mapped to a boolean value.
   * @param option An {@code AppOption} instance for retrieving and storing the option value.
   */
  protected OptionCheckBox(Object id, String label, String desc, boolean value, AppOption option) {
    super(id, label, desc, option);
    this.value = value;
  }

  /** Returns the selection state of the checkbox option. */
  public boolean getValue() {
    return value;
  }

  /** Defines the selection state of the checkbox option. */
  public OptionCheckBox setValue(boolean value) {
    this.value = value;
    return this;
  }

  /**
   * Returns the function that will be executed right before the UI elements for the option are created.
   */
  public Consumer<OptionCheckBox> getOnInit() {
    return onInit;
  }

  /**
   * Specifies a function that will be executed right before the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionCheckBox setOnInit(Consumer<OptionCheckBox> proc) {
    onInit = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the UI elements for the option are created.
   */
  public Consumer<OptionCheckBox> getOnCreated() {
    return onCreated;
  }

  /**
   * Specifies a function that will be executed right after the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionCheckBox setOnCreated(Consumer<OptionCheckBox> proc) {
    onCreated = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the user accepted the changes made in the options dialog.
   */
  public Consumer<OptionCheckBox> getOnAccept() {
    return onAccept;
  }

  /**
   * Specifies a function that will be executed right after the user accepted the changes made in the options dialog.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionCheckBox setOnAccept(Consumer<OptionCheckBox> proc) {
    onAccept = proc;
    return this;
  }

  /**
   * Returns the function that will be executed whenever the user changed the selected state of the associated checkbox.
   */
  public Function<OptionCheckBox, Boolean> getOnAction() {
    return onAction;
  }

  /**
   * Specifies a function that will be executed whenever the user changed the selected state of the associated checkbox.
   *
   * @param action {@link Function} instance which is called with this {@code OptionCheckBox} instance as parameter.
   * The function should return {@code true} if the current checkbox selection state should be retained and
   * {@code false} if the selection state should be reverted.
   */
  public OptionCheckBox setOnAction(Function<OptionCheckBox, Boolean> action) {
    this.onAction = action;
    return this;
  }

  /**
   * Updates all UI-related content with data from this instance. UI components are created if needed.
   */
  public OptionCheckBox updateUi() {
    if (checkBox == null) {
      checkBox = new JCheckBox();
    }
    checkBox.setText(getLabel());
    checkBox.setSelected(getValue());
    checkBox.setEnabled(isEnabled());
    addUiComponent(checkBox);

    return this;
  }

  /** Returns the {@code JCheckBox} component of the OptionCheckBox UI. */
  public JCheckBox getUiCheckBox() {
    return checkBox;
  }

  /** Sets the {@code JCheckBox} component of the OptionCheckBox UI. */
  public OptionCheckBox setUiCheckBox(JCheckBox checkBox) {
    removeUiComponent(this.checkBox);
    this.checkBox = checkBox;
    addUiComponent(this.checkBox);
    return this;
  }

  @Override
  public void fireOnInit() {
    if (getOnInit() != null) {
      getOnInit().accept(this);
    } else if (getOption() != null && getOption().isBoolValue()) {
      setValue(getOption().getBoolValue());
    }
  }

  @Override
  public void fireOnCreated() {
    if (getOnCreated() != null) {
      getOnCreated().accept(this);
    }
  }

  @Override
  public void fireOnAccept() {
    if (getOnAccept() != null) {
      getOnAccept().accept(this);
    } else if (getOption() != null && getOption().getDefault() instanceof Boolean) {
      getOption().setValue(getValue());
    }
  }

  /**
   * This method is called after the user changed the selection state of the associated checkbox and can therefore
   * be used to evaluate or revert the change.
   */
  public void fireOnAction() {
    if (onAction != null) {
      if (!onAction.apply(this)) {
        // reverting action
        checkBox.setSelected(!checkBox.isSelected());
      }
    }
  }
}
