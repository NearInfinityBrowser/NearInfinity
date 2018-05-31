// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

public final class StatusBar extends JPanel implements CaretListener
{
  private final JLabel messageLabel = new JLabel();
  private final JLabel cursorLabel = new JLabel();

  public StatusBar()
  {
    super(new BorderLayout(3, 0));
    messageLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                                                              BorderFactory.createLineBorder(
                                                                      UIManager.getColor("controlShadow"))));
    cursorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1),
                                                             BorderFactory.createLineBorder(
                                                                     UIManager.getColor("controlShadow"))));
    cursorLabel.setPreferredSize(new Dimension(120, cursorLabel.getPreferredSize().height));
    add(messageLabel, BorderLayout.CENTER);
    add(cursorLabel, BorderLayout.EAST);
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
  }

// --------------------- Begin Interface CaretListener ---------------------

  @Override
  public void caretUpdate(CaretEvent event)
  {
    if (event.getSource() instanceof JTextArea) {
      JTextArea source = (JTextArea)event.getSource();
      int position = event.getDot();
      try {
        int linenr = source.getLineOfOffset(position);
        cursorLabel.setText(String.format(" %d:%d", linenr + 1,
                                          1 + position - source.getLineStartOffset(linenr)));
      } catch (BadLocationException e) {
        cursorLabel.setText("");
      }
    }
  }

// --------------------- End Interface CaretListener ---------------------

  public void setCursorText(String text)
  {
    cursorLabel.setText(' ' + text);
  }

  public String getCursorText()
  {
    String text = cursorLabel.getText();
    if (text.length() > 0) {
      return text.substring(1);
    } else {
      return "";
    }
  }

  public void setMessage(String msg)
  {
    messageLabel.setText(' ' + msg);
    cursorLabel.setText("");
  }

  public String getMessage()
  {
    String text = messageLabel.getText();
    if (text.length() > 0)
      return text.substring(1);
    else
      return "";
  }
}

