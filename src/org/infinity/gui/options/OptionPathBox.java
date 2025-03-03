// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

import org.infinity.AppOption;
import org.infinity.resource.Profile;
import org.tinylog.Logger;

/**
 * Definition of a file or folder path.
 * <p>
 * This type is represented by a {@code JTextField} for the path string, a {@code JButton} to open a path selection
 * dialog and an associated label.
 * </p>
 */
public class OptionPathBox extends OptionElementBase implements ActionListener {
  private final boolean chooseFolder;
  private final boolean showClearButton;
  private final FileFilter fileFilter;

  private String pathString;
  private String prevPathString;

  private JLabel label;
  private JTextField textField;
  private JButton selectButton;
  private JButton clearButton;
  private JFileChooser fileChooser;

  private Consumer<OptionPathBox> onInit;
  private Consumer<OptionPathBox> onCreated;
  private Consumer<OptionPathBox> onAccept;
  private Function<OptionPathBox, Boolean> onChanged;

  public static OptionPathBox folderCreate(Object id, String label, String desc, String defPath,
      boolean showClearButton, AppOption option) {
    return new OptionPathBox(id, label, desc, defPath, true, null, showClearButton, option);
  }

  public static OptionPathBox fileCreate(Object id, String label, String desc, String defPath, FileFilter filter,
      boolean showClearButton, AppOption option) {
    return new OptionPathBox(id, label, desc, defPath, false, filter, showClearButton, option);
  }

  /**
   * Creates a new {@code OptionPathBox} object with the specified parameters.
   *
   * @param id              An identifier for this option element.
   * @param label           A short descriptive string used as label for the combobox with group box elements.
   * @param desc            A more detailed description string for display in an info panel.
   * @param defPath         Initial path string.
   * @param chooseFolder    Specify {@code true} to choose folder paths. Specify {@code false} to choose file paths.
   * @param filter          {@link FileFilter} instance to filter file paths. Specify {@code null} to use the default
   *                          filter.
   * @param showClearButton Indicates whether a "Clear" button should be available to remove a selected path.
   * @param option          An {@code AppOption} instance for retrieving and storing the option value.
   */
  protected OptionPathBox(Object id, String label, String desc, String defPath, boolean chooseFolder, FileFilter filter,
      boolean showClearButton, AppOption option) {
    super(id, label, desc, option);

    this.fileFilter = filter;
    this.chooseFolder = chooseFolder;
    this.showClearButton = showClearButton;
    setPath(defPath);
  }

  /** Returns the currently set path. Returns {@code null} if no path is set. */
  public Path getPath() {
    try {
      return Paths.get(pathString);
    } catch (InvalidPathException e) {
      Logger.warn(e);
    }
    return null;
  }

  /** Sets the current path as {@code String} and triggers an {@code onChanged} event. */
  public void setPath(String path) {
    prevPathString = pathString;
    if (path != null) {
      pathString = path.toString();
    } else {
      pathString = "";
    }
    if (textField != null) {
      textField.setText(pathString);
      if (pathString.isEmpty()) {
        textField.setToolTipText(null);
      } else {
        textField.setToolTipText(pathString);
      }
    }
    updateButtons();
  }

  /** Sets the current path as {@code Path} and triggers an {@code onChanged} event. */
  public void setPath(Path path) {
    setPath(path != null ? path.toString() : null);
  }

  /** Reverts the last {@code setPath()} call. */
  public void undoPath() {
    if (prevPathString != null) {
      pathString = prevPathString;
    } else {
      pathString = "";
    }
    textField.setText(pathString);
  }

  /**
   * Returns the function that will be executed right before the UI elements for the option are created.
   */
  public Consumer<OptionPathBox> getOnInit() {
    return onInit;
  }

  /**
   * Specifies a function that will be executed right before the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionPathBox} instance as parameter.
   */
  public OptionPathBox setOnInit(Consumer<OptionPathBox> proc) {
    onInit = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the UI elements for the option are created.
   */
  public Consumer<OptionPathBox> getOnCreated() {
    return onCreated;
  }

  /**
   * Specifies a function that will be executed right after the UI elements for the option are created.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionPathBox} instance as parameter.
   */
  public OptionPathBox setOnCreated(Consumer<OptionPathBox> proc) {
    onCreated = proc;
    return this;
  }

  /**
   * Returns the function that will be executed right after the user accepted the changes made in the options dialog.
   */
  public Consumer<OptionPathBox> getOnAccept() {
    return onAccept;
  }

  /**
   * Specifies a function that will be executed right after the user accepted the changes made in the options dialog.
   *
   * @param proc {@link Consumer} instance which accepts this {@code OptionPathBox} instance as parameter.
   */
  public OptionPathBox setOnAccept(Consumer<OptionPathBox> proc) {
    onAccept = proc;
    return this;
  }

  /** Returns the function that is executed whenever the current path changed. */
  public Function<OptionPathBox, Boolean> getOnChanged() {
    return onChanged;
  }

  /**
   * Specifies the function that is executed whenever the current path changed.
   *
   * @param changed {@link Function} instance which is called with the {@code OptionPathBox} instance as parameter. The
   *                  function should return {@code true} if the current path should be retained, and {@code false} if
   *                  the current path should be reverted to the previous value.
   */
  public OptionPathBox setOnChanged(Function<OptionPathBox, Boolean> changed) {
    onChanged = changed;
    return this;
  }

  /** Updates all UI-related content with data from this instance. UI components are created if needed. */
  public OptionPathBox updateUi() {
    if (label == null) {
      label = new JLabel();
    }
    label.setText(getLabel() + ":");
    label.setEnabled(isEnabled());
    addUiComponent(label);

    if (textField == null) {
      textField = new JTextField(20); // keep default width manageable
      textField.setEditable(false);
    }
    textField.setText(Objects.toString(getPath(), ""));
    if (!textField.getText().isEmpty()) {
      textField.setToolTipText(textField.getText());
    }
    textField.setEnabled(isEnabled());
    addUiComponent(textField);

    if (selectButton == null) {
      selectButton = new JButton("Choose...");
      selectButton.addActionListener(this);
    }
    selectButton.setEnabled(isEnabled());
    addUiComponent(selectButton);

    if (showClearButton) {
      if (clearButton == null) {
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this);
      }
      clearButton.setEnabled(isEnabled());
      addUiComponent(clearButton);
    }

    updateButtons();

    return this;
  }

  /** Returns the {@code JLabel} component of the OptionPathBox UI. */
  public JLabel getUiLabel() {
    return label;
  }

  /** Sets the {@code JLabel} component of the OptionPathBox UI. */
  public OptionPathBox setUiLabel(JLabel label) {
    removeUiComponent(this.label);
    this.label = label;
    addUiComponent(this.label);
    return this;
  }

  /** Returns the {@code JTextField} component of the OptionPathBox UI. */
  public JTextField getUiTextField() {
    return textField;
  }

  /** Sets the {@code JTextField} component of the OptionPathBox UI. */
  public OptionPathBox setUiTextField(JTextField textField) {
    removeUiComponent(this.textField);
    this.textField = textField;
    addUiComponent(this.textField);
    return this;
  }

  /** Returns the select {@code JButton} component of the OptionPathBox UI. */
  public JButton getUiSelectButton() {
    return selectButton;
  }

  /** Sets the select {@code JButton} component of the OptionPathBox UI. */
  public OptionPathBox setUiSelectButton(JButton button) {
    removeUiComponent(this.selectButton);
    this.selectButton = button;
    addUiComponent(this.selectButton);
    return this;
  }

  /** Returns the clear {@code JButton} component of the OptionPathBox UI. */
  public JButton getUiClearButton() {
    return clearButton;
  }

  /** Sets the clear {@code JButton} component of the OptionPathBox UI. */
  public OptionPathBox setUiClearButton(JButton button) {
    if (this.clearButton != null) {
      removeUiComponent(this.clearButton);
    }
    this.clearButton = button;
    if (this.clearButton != null) {
      addUiComponent(this.clearButton);
    }
    return this;
  }

  @Override
  public void fireOnInit() {
    if (getOnInit() != null) {
      getOnInit().accept(this);
    } else if (getOption() != null) {
      // fall-back option
      setPath(getOption().getStringValue());
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
    } else if (getOption() != null) {
      getOption().setValue(Objects.toString(getPath(), ""));
    }
  }

  /** This method is called whenever the current path has been updated. */
  public void fireOnChanged() {
    if (onChanged != null) {
      if (!onChanged.apply(this)) {
        undoPath();
      }
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == selectButton) {
      if (selectPath()) {
        fireOnChanged();
      }
    } else if (e.getSource() == clearButton) {
      if (!Objects.toString(getPath(), "").isEmpty()) {
        setPath("");
        fireOnChanged();
      }
    }
  }

  /** Allows the user to choose a path interactively. */
  private boolean selectPath() {
    if (fileChooser == null) {
      fileChooser = new JFileChooser();
      fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
      fileChooser.setMultiSelectionEnabled(false);

      final int selectionMode;
      if (chooseFolder) {
        selectionMode = JFileChooser.DIRECTORIES_ONLY;
        fileChooser.setDialogTitle("Choose folder");
      } else {
        selectionMode = JFileChooser.FILES_ONLY;
        fileChooser.setDialogTitle("Choose file");
      }
      fileChooser.setFileSelectionMode(selectionMode);

      fileChooser.addChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
      if (fileFilter != null) {
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
      }
    }

    final File curPath;
    final File curFile;
    final Path path = getPath();
    if (path != null && path.getNameCount() > 1) {
      curPath = path.getParent().toFile();
      curFile = path.toFile();
    } else {
      curPath = Profile.getGameRoot().toFile();
      curFile = null;
    }
    fileChooser.setCurrentDirectory(curPath);
    if (curFile != null) {
      fileChooser.setSelectedFile(curFile);
    }

    final int result = fileChooser.showOpenDialog(selectButton.getTopLevelAncestor());
    if (result == JFileChooser.APPROVE_OPTION) {
      setPath(fileChooser.getSelectedFile().toPath());
      return true;
    }

    return false;
  }

  private void updateButtons() {
    if (textField != null) {
      if (clearButton != null) {
        clearButton.setEnabled(!textField.getText().isEmpty());
      }
    }
  }
}
