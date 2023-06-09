// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.infinity.icon.Icons;

// Common base class for settings dialogs, used to customize the creation of new resources
public abstract class NewAbstractSettings extends JDialog implements ActionListener {
  private static final KeyStroke ESC_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

  private boolean accepted;
  private JButton okButton;
  private JButton cancelButton;

  protected NewAbstractSettings(Window parent, String title) {
    super(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
    initDialog(parent);
  }

  // public interface to resource-specific configuration data
  public abstract Object getConfig();

  // incquire the return state of the dialog
  public boolean isAccepted() {
    return accepted;
  }

  // override to implement special actions performed when clicking the "Create" button
  protected void accept() {
    accepted = true;
    setVisible(false);
  }

  // override to implement special actions performed when clicking the "Cancel" button
  protected void reject() {
    accepted = false;
    setVisible(false);
  }

  protected JButton acceptButton() {
    return okButton;
  }

  protected JButton rejectButton() {
    return cancelButton;
  }

  private void initDialog(Window parent) {
    accepted = false;

    okButton = new JButton("Create", Icons.ICON_NEW_16.getIcon());
    okButton.addActionListener(this);
    getRootPane().setDefaultButton(okButton);

    cancelButton = new JButton("Cancel", Icons.ICON_DELETE_16.getIcon());
    cancelButton.addActionListener(this);

    getRootPane().getActionMap().put("NIclose", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        reject();
      }
    });
  }

  protected boolean isCancelOnEscape() {
    return (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESC_KEY) != null);
  }

  protected void setCancelOnEscape(boolean bEnable) {
    if (bEnable && getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESC_KEY) == null) {
      getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESC_KEY, "NIclose");
    } else if (!bEnable && getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ESC_KEY) != null) {
      getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(ESC_KEY);
    }
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == acceptButton()) {
      accept();
    } else if (event.getSource() == rejectButton()) {
      reject();
    }
  }

  // --------------------- End Interface ActionListener ---------------------
}
