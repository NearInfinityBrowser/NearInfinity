// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

/**
 * Widget that allow check resource types and get back list of selected resource
 * pointers.
 *
 * @author Mingun
 */
public class FileTypeSelector extends JPanel implements ActionListener
{
  /**
   * Stores last chosed array of selected checkboxes for each resource type.
   * Each array has length equals to length of {@link #filetypes}
   */
  private static final HashMap<String, boolean[]> LAST_SELECTION = new HashMap<>();

  /** Extensions of files with resources in which it is possible to search. */
  private final String[] filetypes;
  /** Initial and default values for each file type. Length of this array is the same as for {@link #filetypes}. */
  private final boolean[] defaultStates;
  /** Checkboxes for each file type. Length of this array is the same as for {@link #filetypes}. */
  private final JCheckBox[] boxes;
  /** Button that clears all checkboxes. */
  private final JButton bClear   = new JButton("Clear");
  /** Button that check all checkboxes. */
  private final JButton bSet     = new JButton("Set");
  /** Button that resets checkbox checked state to their default values. */
  private final JButton bDefault = new JButton("Default", Icons.getIcon(Icons.ICON_UNDO_16));
  /** Button that inverts check state of each checkbox. */
  private final JButton bInvert  = new JButton("Invert", Icons.getIcon(Icons.ICON_REFRESH_16));

  /**
   * Creates panel with tho columns of checkboxex with abilities to check/unckeck all,
   * invert current selection and reset to default selection.
   *
   * @param title Title for border around checkboxes
   * @param key Key for last selected values
   * @param filetypes List of possible file types for selection. Must not be {@code null}
   * @param defaultStates Initial and default values for each file type. If length is
   *        less that length of {@code filetypes}, tail values assumed to be {@code false}.
   *        If {@code null}, all values assumed to be {@code true}
   */
  public FileTypeSelector(String title, String key, String[] filetypes, boolean[] defaultStates) {
    super(new BorderLayout());
    if (defaultStates == null) {
      defaultStates = new boolean[filetypes.length];
      Arrays.fill(defaultStates, true);
    }
    this.filetypes = filetypes;
    this.defaultStates = defaultStates.length < filetypes.length
                       ? Arrays.copyOf(defaultStates, filetypes.length)
                       : defaultStates;
    this.boxes = new JCheckBox[filetypes.length];

    final JPanel boxpanel = new JPanel(new GridLayout(0, 2, 3, 3));
    for (int i = 0; i < filetypes.length; ++i) {
      boxes[i] = new JCheckBox(filetypes[i], this.defaultStates[i]);
      boxpanel.add(boxes[i]);
    }

    // Restore last used configuration
    final boolean[] selection = LAST_SELECTION.get(key);
    if (selection != null) {
      for (int i = 0; i < Math.min(selection.length, boxes.length); ++i) {
        boxes[i].setSelected(selection[i]);
      }
    }

    final JPanel buttons = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    buttons.add(bClear, gbc);

    gbc.gridx  = 1;
    gbc.insets = new Insets(0, 8, 0, 0);
    buttons.add(bSet, gbc);

    gbc.gridx  = 0;
    gbc.gridy  = 1;
    gbc.insets = new Insets(4, 0, 0, 0);
    buttons.add(bDefault, gbc);

    gbc.gridx  = 1;
    gbc.insets = new Insets(4, 8, 0, 0);
    buttons.add(bInvert, gbc);

    add(boxpanel, BorderLayout.CENTER);
    add(buttons, BorderLayout.SOUTH);
    setBorder(BorderFactory.createTitledBorder(title));

    bInvert.setMnemonic('i');
    bDefault.setMnemonic('d');

    bClear.addActionListener(this);
    bSet.addActionListener(this);
    bDefault.addActionListener(this);
    bInvert.addActionListener(this);
  }

  /**
   * Gets all selected resource pointers.
   *
   * @param key Key for store last selected values
   * @return List with selected values. Never {@code null}
   */
  public List<ResourceEntry> getResources(String key) {
    boolean[] selection = LAST_SELECTION.get(key);
    if (selection == null) {
      selection = new boolean[filetypes.length];
      LAST_SELECTION.put(key, selection);
    }

    final List<ResourceEntry> result = new ArrayList<>();
    for (int i = 0; i < filetypes.length; ++i) {
      selection[i] = boxes[i].isSelected();
      if (selection[i]) {
        result.addAll(ResourceFactory.getResources(filetypes[i]));
      }
    }
    return result;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final Object src = e.getSource();

    if (src == bClear) {
      for (final JCheckBox box : boxes) {
        box.setSelected(false);
      }
    } else
    if (src == bSet || src == bDefault && defaultStates == null) {
      for (final JCheckBox box : boxes) {
        box.setSelected(true);
      }
    } else
    if (src == bInvert) {
      for (final JCheckBox box : boxes) {
        box.setSelected(!box.isSelected());
      }
    } else
    if (src == bDefault) {
      for (int i = 0; i < boxes.length; i++) {
        boxes[i].setSelected(defaultStates[i]);
      }
    }
  }
}
