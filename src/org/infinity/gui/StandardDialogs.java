// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.util.Arrays;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.infinity.util.tuples.Couple;

/**
 * Expands several standard dialog methods of the {@link JOptionPane} class by optional checkboxes.
 * They are  primarily intended for <code>"Do not show this message again."</code>-kind of options.
 */
public class StandardDialogs extends JOptionPane {
  /**
   * Brings up an information-message dialog titled "Message". An additional {@code JCheckBox} element is shown below
   * the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A boolean that indicates the selection state of the checkbox.
   */
  public static boolean showMessageDialogExtra(Component parentComponent,
      Object message, Extra extra) throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createMessageCheckBox() : null;
    showMessageDialog(parentComponent, createMessageObject(message, cbOption));
    return cbOption != null && cbOption.isSelected();
  }

  /**
   * Brings up a dialog that displays a message using a default icon determined by the {@code messageType} parameter. An
   * additional {@code JCheckBox} element is shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param messageType     the type of message to be displayed: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
   *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A boolean that indicates the selection state of the checkbox.
   */
  public static boolean showMessageDialogExtra(Component parentComponent,
      Object message, String title, int messageType, Extra extra)
      throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createMessageCheckBox() : null;
    showMessageDialog(parentComponent, createMessageObject(message, cbOption), title, messageType);
    return cbOption != null && cbOption.isSelected();
  }

  /**
   * Brings up a dialog displaying a message, specifying all parameters. An additional {@code JCheckBox} element is
   * shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param messageType     the type of message to be displayed: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
   *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}.
   * @param icon            an icon to display in the dialog that helps the user identify the kind of message that is
   *                        being displayed.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A boolean that indicates the selection state of the checkbox.
   */
  public static boolean showMessageDialogExtra(Component parentComponent,
      Object message, String title, int messageType, Icon icon, Extra extra)
      throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createMessageCheckBox() : null;
    showMessageDialog(parentComponent, createMessageObject(message, cbOption), title, messageType, icon);
    return cbOption != null && cbOption.isSelected();
  }

  /**
   * Brings up a dialog with the options <i>Yes</i>, <i>No</i> and <i>Cancel</i>; with the title, <b>Select an
   * Option</b>. An additional {@code JCheckBox} element is shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A tuple consisting of an {@code Integer} indicating the option selected by the user and the selection state
   *         of the checkbox, represented by a {@code Boolean}.
   */
  public static Couple<Integer, Boolean> showConfirmDialogExtra(Component parentComponent, Object message, Extra extra)
      throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createConfirmCheckBox() : null;
    int result = showConfirmDialog(parentComponent, createMessageObject(message, cbOption));
    return getDialogResult(result, cbOption);
  }

  /**
   * Brings up a dialog where the number of choices is determined by the {@code optionType} parameter. An additional
   * {@code JCheckBox} element is shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
   *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A tuple consisting of an {@code Integer} indicating the option selected by the user and the selection state
   *         of the checkbox, represented by a {@code Boolean}.
   */
  public static Couple<Integer, Boolean> showConfirmDialogExtra(Component parentComponent, Object message, String title,
      int optionType, Extra extra) throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createConfirmCheckBox() : null;
    int result = showConfirmDialog(parentComponent, createMessageObject(message, cbOption), title, optionType);
    return getDialogResult(result, cbOption);
  }

  /**
   * Brings up a dialog where the number of choices is determined by the {@code optionType} parameter, where the
   * {@code messageType} parameter determines the icon to display. The {@code messageType} parameter is primarily used
   * to supply a default icon from the Look and Feel. An additional {@code JCheckBox} element is shown below the dialog
   * message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
   *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}.
   * @param messageType     an integer designating the kind of message this is; primarily used to determine the icon
   *                        from the pluggable Look and Feel: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
   *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A tuple consisting of an {@code Integer} indicating the option selected by the user and the selection state
   *         of the checkbox, represented by a {@code Boolean}.
   */
  public static Couple<Integer, Boolean> showConfirmDialogExtra(Component parentComponent, Object message, String title,
      int optionType, int messageType, Extra extra) throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createConfirmCheckBox() : null;
    int result = showConfirmDialog(parentComponent, createMessageObject(message, cbOption), title, optionType,
        messageType);
    return getDialogResult(result, cbOption);
  }

  /**
   * Brings up a dialog with a specified icon, where the number of choices is determined by the {@code optionType}
   * parameter. The {@code messageType} parameter is primarily used to supply a default icon from the look and feel. An
   * additional {@code JCheckBox} element is shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
   *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}.
   * @param messageType     an integer designating the kind of message this is; primarily used to determine the icon
   *                        from the pluggable Look and Feel: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
   *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}.
   * @param icon            the icon to display in the dialog.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A tuple consisting of an {@code Integer} indicating the option selected by the user and the selection state
   *         of the checkbox, represented by a {@code Boolean}.
   */
  public static Couple<Integer, Boolean> showConfirmDialogExtra(Component parentComponent, Object message, String title,
      int optionType, int messageType, Icon icon, Extra extra) throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createConfirmCheckBox() : null;
    int result = showConfirmDialog(parentComponent, createMessageObject(message, cbOption), title, optionType,
        messageType, icon);
    return getDialogResult(result, cbOption);
  }

  /**
   * Brings up a dialog with a specified icon, where the initial choice is determined by the {@code initialValue}
   * parameter and the number of choices is determined by the {@code optionType} parameter.
   * <p>
   * If {@code optionType} is {@code YES_NO_OPTION}, or {@code YES_NO_CANCEL_OPTION} and the {@code options} parameter
   * is {@code null}, then the options are supplied by the look and feel.
   * <p>
   * The {@code messageType} parameter is primarily used to supply a default icon from the look and feel.
   * <p>
   * An additional {@code JCheckBox} element is shown below the dialog message.
   *
   * @param parentComponent determines the {@code Frame} in which the dialog is displayed; if {@code null}, or if the
   *                        {@code parentComponent} has no {@code Frame}, a default {@code Frame} is used.
   * @param message         the {@code Object} to display.
   * @param title           the title string for the dialog.
   * @param optionType      an int designating the options available on the dialog: {@code YES_NO_OPTION},
   *                        {@code YES_NO_CANCEL_OPTION}, or {@code OK_CANCEL_OPTION}.
   * @param messageType     an integer designating the kind of message this is; primarily used to determine the icon
   *                        from the pluggable Look and Feel: {@code ERROR_MESSAGE}, {@code INFORMATION_MESSAGE},
   *                        {@code WARNING_MESSAGE}, {@code QUESTION_MESSAGE}, or {@code PLAIN_MESSAGE}.
   * @param icon            the icon to display in the dialog.
   * @param options         an array of objects indicating the possible choices the user can make; if the objects are
   *                        components, they are rendered properly; non-{@code String} objects are rendered using their
   *                        {@code toString} methods; if this parameter is {@code null}, the options are determined by
   *                        the Look and Feel.
   * @param initialValue    the object that represents the default selection for the dialog; only meaningful if
   *                        {@code options} is used; can be {@code null}.
   * @param extra           An optional {@link Extra} object containing definitions for the extra checkbox.
   * @return A tuple consisting of an {@code Integer} indicating the option selected by the user and the selection state
   *         of the checkbox, represented by a {@code Boolean}.
   */
  public static Couple<Integer, Boolean> showOptionDialogExtra(Component parentComponent, Object message, String title,
      int optionType, int messageType, Icon icon, Object[] options, Object initialValue, Extra extra)
      throws HeadlessException {
    final JCheckBox cbOption = extra != null ? extra.createOptionCheckBox() : null;
    int result = showOptionDialog(parentComponent, createMessageObject(message, cbOption), title, optionType,
        messageType, icon, options, initialValue);
    return getDialogResult(result, cbOption);
  }

  /** Returns the appropriate message object for the dialog methods, depending on the given parameters. */
  private static Object createMessageObject(Object message, Object newItem) {
    if (newItem == null) {
      return message;
    }

    if (message == null) {
      return newItem;
    } else if (message.getClass().isArray()) {
      Object[] array = Arrays.copyOf((Object[]) message, ((Object[]) message).length + 1);
      array[array.length - 1] = newItem;
      return array;
    } else {
      return new Object[] { message, newItem };
    }
  }

  /** Ensures a valid result object for confirmation dialogs. */
  private static Couple<Integer, Boolean> getDialogResult(int result, JCheckBox cbOption) {
    return Couple.with(result, cbOption != null ? cbOption.isSelected() : false);
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * This class handles configuration and creation of the extra checkbox that can be added to the standard dialogs by
   * the {@code StandardDialogs.show*DialogExtra()} methods.
   */
  public static class Extra {
    /** A standard message for use in {@link StandardDialogs#showMessageDialogExtra}. */
    public static final String MESSAGE_DO_NOT_SHOW_MESSAGE = "Do not show this message again.";

    /** A standard message for use in {@link StandardDialogs#showConfirmDialogExtra}. */
    public static final String MESSAGE_DO_NOT_SHOW_PROMPT = "Do not show this prompt again.";

    /** A standard message for use in {@link StandardDialogs#showOptionDialogExtra}. */
    public static final String MESSAGE_DO_NOT_SHOW_DIALOG = "Do not show this dialog again.";

    private String text;
    private String tooltip;
    private boolean selected;
    private boolean small;
    private boolean padded;

    /**
     * Prepares a {@link JCheckBox} instance with a standard text suitable for the dialog type. The checkbox will use
     * smaller text and is added with extra padding on the top border. It is initially unselected.
     * <p>
     * The following standard messages are considered:
     * <ul>
     * <li>for message dialogs: {@link #MESSAGE_DO_NOT_SHOW_MESSAGE}</li>
     * <li>for confirmation dialogs: {@link #MESSAGE_DO_NOT_SHOW_PROMPT}</li>
     * <li>for option dialogs: {@link #MESSAGE_DO_NOT_SHOW_DIALOG}</li>
     * </ul>
     *
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra withDefaults() {
      return new Extra(null, null, false, true, true);
    }

    /**
     * Prepares a {@link JCheckBox} instance with the specified {@code text}. The checkbox will use smaller text and is
     * added with extra padding on the top border. It is initially unselected.
     *
     * @param text the text of the checkbox.
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra with(String text) {
      return new Extra(text, null, false, true, true);
    }

    /**
     * Prepares a {@link JCheckBox} instance with the specified {@code text} and {@code tooltip}. The checkbox will use
     * smaller text and is added with extra padding on the top border. It is initially unselected.
     *
     * @param text    the text of the checkbox.
     * @param tooltip the tooltip for the checkbox.
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra with(String text, String tooltip) {
      return new Extra(text, tooltip, false, true, true);
    }

    /**
     * Prepares a {@link JCheckBox} instance with the specified {@code text} and {@code tooltip}. The checkbox will use
     * smaller text and is added with extra padding on the top border.
     *
     * @param text     the text of the checkbox.
     * @param tooltip  the tooltip for the checkbox.
     * @param selected whether the checkbox is initially selected.
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra with(String text, String tooltip, boolean selected) {
      return new Extra(text, tooltip, selected, true, true);
    }

    /**
     * Prepares a {@link JCheckBox} instance with the specified {@code text} and {@code tooltip}. The checkbox is added
     * with extra padding on the top border.
     *
     * @param text     the text of the checkbox.
     * @param tooltip  the tooltip for the checkbox.
     * @param selected whether the checkbox is initially selected.
     * @param small    whether the checkbox text is produced in a slightly smaller font size.
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra with(String text, String tooltip, boolean selected, boolean small) {
      return new Extra(text, tooltip, selected, small, true);
    }

    /**
     * Prepares a {@link JCheckBox} instance with the specified {@code text} and {@code tooltip}.
     *
     * @param text     the text of the checkbox.
     * @param tooltip  the tooltip for the checkbox.
     * @param selected whether the checkbox is initially selected.
     * @param small    whether the checkbox text is produced in a slightly smaller font size.
     * @param padded   whether the checkbox adds extra padding to the top border.
     * @return {@link StandardDialogs.Extra} object for use with the {@code StandardDialogs.show*DialogExtra()} methods.
     */
    public static Extra with(String text, String tooltip, boolean selected, boolean small, boolean padded) {
      return new Extra(text, tooltip, selected, small, padded);
    }

    /** Returns the checkbox text. */
    public String getText() {
      return text;
    }

    /**
     * Sets the checkbox text. Specify {@code null} to use a standard text, depending on use with a message or
     * confirmation dialog.
     */
    public void setText(String text) {
      this.text = text;
    }

    /** Returns the checkbox tooltip. */
    public String getTooltip() {
      return tooltip;
    }

    /** Sets the checkbox tooltip. Specify {@code null} to disable the tooltip. */
    public void setTooltip(String tooltip) {
      this.tooltip = tooltip;
    }

    /** Returns whether the checkbox is initially selected. */
    public boolean isSelected() {
      return selected;
    }

    /** Sets whether the checkbox is initially selected. */
    public void setSelected(boolean selected) {
      this.selected = selected;
    }

    /** Returns whether checkbox text is produced in a smaller font size. */
    public boolean isSmall() {
      return small;
    }

    /** Sets whether checkbox text is produced in a smaller font size. */
    public void setSmall(boolean small) {
      this.small = small;
    }

    /** Returns whether the checkbox adds extra padding to the top border. */
    public boolean isPadded() {
      return padded;
    }

    /** Sets whether the checkbox should add extra padding to the top border. */
    public void setPadded(boolean padded) {
      this.padded = padded;
    }

    private Extra(String text, String tooltip, boolean selected, boolean small, boolean padded) {
      this.text = text;
      this.tooltip = tooltip;
      this.selected = selected;
      this.small = small;
      this.padded = padded;
    }

    /** Returns a checkbox intended for a message dialog. */
    private JCheckBox createMessageCheckBox() {
      return createCheckBox(MESSAGE_DO_NOT_SHOW_MESSAGE);
    }

    /** Returns a checkbox intended for a confirmation dialog. */
    private JCheckBox createConfirmCheckBox() {
      return createCheckBox(MESSAGE_DO_NOT_SHOW_PROMPT);
    }

    /** Returns a checkbox intended for an option dialog. */
    private JCheckBox createOptionCheckBox() {
      return createCheckBox(MESSAGE_DO_NOT_SHOW_DIALOG);
    }

    /**
     * Creates a {@link JCheckBox} object, based on the current settings.
     *
     * @param textOverride Checkbox text to use if {@code text} property is {@code null}.
     * @return A {@code JCheckBox} object.
     */
    private JCheckBox createCheckBox(String textOverride) {
      if (textOverride == null) {
        textOverride = MESSAGE_DO_NOT_SHOW_DIALOG;
      }

      final JCheckBox cbOption = new JCheckBox(text != null ? text : textOverride, selected);

      if (tooltip != null) {
        cbOption.setToolTipText(tooltip);
      }

      if (small) {
        cbOption.setFont(cbOption.getFont().deriveFont(cbOption.getFont().getSize2D() * 0.85f));
      }

      if (padded) {
        Insets insets = cbOption.getMargin();
        if (insets == null) {
          insets = new Insets(0, 0, 0, 0);
        }
        insets.top = cbOption.getFont().getSize() * 2 / 3;
        cbOption.setMargin(insets);
      }

      return cbOption;
    }
  }
}
