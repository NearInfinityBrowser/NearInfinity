// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public final class NewChrSettings extends NewAbstractSettings {
  private final ChrConfig config;

  private JTextField tfName;

  public NewChrSettings(Window parent) {
    super(parent, "CHR settings");
    config = new ChrConfig();
    initDialog(parent);
  }

  public NewChrSettings(Window parent, String name) {
    super(parent, "CHR settings");
    config = new ChrConfig(name);
    initDialog(parent);
  }

  @Override
  public ChrConfig getConfig() {
    return config;
  }

  @Override
  protected void accept() {
    config.setName(tfName.getText());
    super.accept();
  }

  private void initDialog(Window parent) {
    tfName = new JTextField();
    tfName.setColumns(32); // visible space for 32 characters
    tfName.setDocument(new FixedDocument(tfName, 32));

    JLabel label = new JLabel("Enter character name (max. 32 chars):");
    label.setLabelFor(tfName);
    label.setDisplayedMnemonic(KeyEvent.VK_E);

    JPanel panel = new JPanel(new GridBagLayout());
    Container pane = getContentPane();
    pane.add(panel);
    GridBagConstraints gbc = new GridBagConstraints();

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
    buttonPanel.add(acceptButton());
    buttonPanel.add(rejectButton());

    gbc.insets = new Insets(10, 10, 3, 10);
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(label, gbc);

    gbc.insets = new Insets(0, 10, 10, 10);
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(tfName, gbc);

    gbc.insets = new Insets(0, 5, 5, 5);
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.SOUTHEAST;
    gbc.fill = GridBagConstraints.NONE;
    panel.add(buttonPanel, gbc);

    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(parent);
    setCancelOnEscape(true);
    setVisible(true);
  }

  // -------------------------- INNER CLASSES --------------------------

  public static class ChrConfig {
    private String name; // field at offset 0x08

    public ChrConfig() {
      setName("");
    }

    public ChrConfig(String newName) {
      setName(newName);
    }

    public String getName() {
      return name;
    }

    private void setName(String newName) {
      if (newName != null) {
        name = (newName.length() <= 32) ? newName : newName.substring(0, 32);
      } else {
        name = "";
      }
    }
  }

  // Ensures a size limit on byte level
  private static class FixedDocument extends PlainDocument {
    private final int maxLength;
    private final JTextField textField;

    FixedDocument(JTextField text, int length) {
      super();
      textField = text;
      maxLength = Math.max(length, 0);
    }

    @Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      if (str == null || textField == null
          || textField.getText().getBytes().length + str.getBytes().length > maxLength) {
        return;
      }
      super.insertString(offs, str, a);
    }
  }

}
