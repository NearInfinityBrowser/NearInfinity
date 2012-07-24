// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;

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
    cursorLabel.setPreferredSize(new Dimension(75, cursorLabel.getPreferredSize().height));
    add(messageLabel, BorderLayout.CENTER);
    add(cursorLabel, BorderLayout.EAST);
    setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
  }

// --------------------- Begin Interface CaretListener ---------------------

  public void caretUpdate(CaretEvent event)
  {
    if (event.getSource() instanceof JTextArea) {
      JTextArea source = (JTextArea)event.getSource();
      int position = event.getDot();
      try {
        int linenr = source.getLineOfOffset(position);
        cursorLabel.setText(' ' + String.valueOf(linenr + 1) + ':' +
                            String.valueOf(1 + position - source.getLineStartOffset(linenr)));
      } catch (BadLocationException e) {
        cursorLabel.setText("");
      }
    }
  }

// --------------------- End Interface CaretListener ---------------------

  public void setMessage(String msg)
  {
    messageLabel.setText(' ' + msg);
    cursorLabel.setText("");
  }

  public String getMessage()
  {
    String text = messageLabel.getText();
    if (text.length() > 0)
      return messageLabel.getText().substring(1);
    else
      return "";
  }
}

