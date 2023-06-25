// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.infinity.AppOption;

/**
 * Definition of a grouped selection.
 * <p>
 * This type is represented by a {@code JComboBox} and an associated label.
 * </p>
 */
public class OptionGroupBox extends OptionElementBase {
  private final List<Object> items = new ArrayList<>();

  private String selectedDesc;
  private int prevIndex;
  private int selectedIndex;

  private JLabel label;
  private JComboBox<Object> comboBox;

  private Consumer<OptionGroupBox> onInit;
  private Consumer<OptionGroupBox> onCreated;
  private Consumer<OptionGroupBox> onAccept;
  private Function<OptionGroupBox, Boolean> onSelect;

  public static OptionGroupBox create(Object id, String label, String desc, int selectedIndex, Object[] items) {
    return new OptionGroupBox(id, label, desc, selectedIndex, items, null);
  }

  public static OptionGroupBox create(Object id, String label, String desc, int selectedIndex, Object[] items, AppOption option) {
    return new OptionGroupBox(id, label, desc, selectedIndex, items, option);
  }

  /**
   * Creates a new OptionGroupBox with the specified parameters.
   *
   * @param id An identifier for this option element.
   * @param label A short descriptive string used as label for the combobox with group box elements.
   * @param desc A more detailed description string for display in an info panel.
   * @param selectedIndex Initial selected item index. This value can be updated by a specified onInit function if available,
   * or is automatically updated by the specified {@code AppOption} value, otherwise.
   * <b>Note:</b> You have to use the {@code onInit} function if the AppOption value cannot be directly mapped to an item index.
   * @param items List of group box items for use as elements in the associated combobox.
   * @param option An {@code AppOption} instance for retrieving and storing the option value.
   */
  protected OptionGroupBox(Object id, String label, String desc, int selectedIndex, Object[] items, AppOption option) {
    super(id, label, desc, option);

    if (items != null) {
      for (int i = 0; i < items.length; i++) {
        this.items.add(Objects.requireNonNull(items[i]));
      }
    }

    setSelectedDesc(desc);
    setSelectedIndex(selectedIndex);
    this.prevIndex = this.selectedIndex;
  }

  /** Returns the previously selected item index of the group box. */
  public int getPreviousSelectedIndex() {
    return prevIndex;
  }

  /** Returns the currently selected item index of the group box. */
  public int getSelectedIndex() {
    return selectedIndex;
  }

  /** Specifies the index of the selected item of the group box. */
  public OptionGroupBox setSelectedIndex(int index) {
    this.prevIndex = this.selectedIndex;
    this.selectedIndex = Math.max(-1, Math.min(items.size() - 1, index));
    return this;
  }

  /** Returns a list of available items. */
  public List<Object> getItems() {
    return Collections.unmodifiableList(items);
  }

  /** Returns the number of available items. */
  public int getItemCount() {
    return items.size();
  }

  /** Returns the item at the specified index. */
  public Object getItem(int index) throws IndexOutOfBoundsException {
    return items.get(index);
  }

  /** Adds a new item to the group box. */
  public OptionGroupBox addItem(Object item) throws NullPointerException {
    items.add(Objects.requireNonNull(item));
    return this;
  }

  /** Insert a new item at the specified index in the group box. */
  public OptionGroupBox insertItem(int index, Object item)
      throws IndexOutOfBoundsException, NullPointerException {
    items.add(index, item);
    return this;
  }

  /** Removes the group box item at the specified index. */
  public OptionGroupBox removeItem(int index) throws IndexOutOfBoundsException {
    items.remove(index);
    return this;
  }

  /** Removes the specified group box item. */
  public OptionGroupBox removeItem(Objects item) {
    items.remove(item);
    return this;
  }

  /** Removes all group box items at once. */
  public OptionGroupBox clearItems() {
    items.clear();
    return this;
  }

  /**
   * Returns the description string for display in the info box when the user hovers the mouse over the combobox.
   * <p>
   * Returns the default description string if the selected description string is empty or {@code null}.
   * </p>
   */
  public String getSelectedDesc() {
    if (selectedDesc.isEmpty()) {
      return getDescription();
    } else {
      return selectedDesc;
    }
  }

  /**
   * Defines the description string for display when the user hovers the mouse over the combobox.
   * <p>
   * Specify empty string or {@code null} to display the generic description string instead.
   * </p>
   */
  public OptionGroupBox setSelectedDesc(String desc) {
    this.selectedDesc = Objects.toString(desc, "");
    return this;
  }

  @Override
  protected OptionGroupBox setUiEnabled(boolean enable) {
    if (label != null) {
      label.setEnabled(enable);
    }
    if (comboBox != null) {
      comboBox.setEnabled(enable);
    }
    return this;
  }

  /**
   * Returns the function that will be executed right before the UI elements for the option are created.
   */
  public Consumer<OptionGroupBox> getOnInit() {
    return onInit;
  }

  /**
   * Specifies a function that will be executed right before the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionGroupBox setOnInit(Consumer<OptionGroupBox> proc) {
    onInit = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the UI elements for the option are created.
   */
  public Consumer<OptionGroupBox> getOnCreated() {
    return onCreated;
  }

  /**
   * Specifies a function that will be executed right after the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionGroupBox setOnCreated(Consumer<OptionGroupBox> proc) {
    onCreated = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the user accepted the changes made in the options dialog.
   */
  public Consumer<OptionGroupBox> getOnAccept() {
    return onAccept;
  }

  /**
   * Specifies a function that will be executed right after the user accepted the changes made in the options dialog.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionCheckBox} instance as parameter.
   */
  public OptionGroupBox setOnAccept(Consumer<OptionGroupBox> proc) {
    onAccept = proc;
    return this;
  }

  /**
   * Returns the function that will be executed whenever the user changed the selected item of the associated combobox.
   */
  public Function<OptionGroupBox, Boolean> getOnSelect() {
    return onSelect;
  }

  /**
   * Specifies a function that will be executed whenever the user changed the selected item of the associated combobox.
   *
   * @param action {@link Function} instance which is called with this {@code OptionGroupBox} instance as parameter.
   * The function should return {@code true} if the current combobox selection state should be retained and
   * {@code false} if the selection state should be reverted to the previous value.
   */
  public OptionGroupBox setOnSelect(Function<OptionGroupBox, Boolean> select) {
    this.onSelect = select;
    return this;
  }

  /**
   * Updates all UI-related content with data from this instance. UI components are created if needed.
   */
  public OptionGroupBox updateUi() {
    if (label == null) {
      label = new JLabel();
    }
    label.setText(getLabel() + ":");
    label.setEnabled(isEnabled());

    if (comboBox == null) {
      comboBox = new JComboBox<>();
    }
    int index = getSelectedIndex();
    comboBox.setModel(new DefaultComboBoxModel<>(items.toArray()));
    int newIndex = Math.max(-1, Math.min(items.size() - 1, index));
    comboBox.setSelectedIndex(newIndex);
    comboBox.setEnabled(isEnabled());

    return this;
  }

  /** Returns the {@code JLabel} component of the OptionGroupBox UI. */
  public JLabel getUiLabel() {
    return label;
  }

  /** Sets the {@code JLabel} component of the OptionGroupBox UI. */
  public OptionGroupBox setUiLabel(JLabel label) {
    this.label = label;
    return this;
  }

  /** Returns the {@code JComboBox} component of the OptionGroupBox UI. */
  public JComboBox<Object> getUiComboBox() {
    return comboBox;
  }

  /** Sets the {@code JComboBox} component of the OptionGroupBox UI. */
  public OptionGroupBox setUiComboBox(JComboBox<Object> comboBox) {
    this.comboBox = comboBox;
    return this;
  }

  @Override
  public void fireOnInit() {
    if (getOnInit() != null) {
      getOnInit().accept(this);
    } else if (getOption() != null && getOption().isNumericValue()) {
      // fall-back option
      setSelectedIndex(getOption().getIntValue());
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
    } else if (getOption() != null && getOption().getDefault() instanceof Number) {
      getOption().setValue(Integer.valueOf(getSelectedIndex()));
    }
  }

  /**
   * This method is called after the user changed the selected item in the associated combobox and can therefore
   * be used to evaluate or revert the change.
   */
  public void fireOnSelect() {
    if (onSelect != null) {
      if (!onSelect.apply(this)) {
        comboBox.setSelectedIndex(prevIndex);
      }
    }
  }
}