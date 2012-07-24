// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.util.StructClipboard;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

final class ClipboardViewer extends ChildFrame implements ActionListener, ChangeListener
{
  private final JButton bclearclipboard = new JButton("Clear", Icons.getIcon("New16.gif"));
  private final JTextArea taClipBoard = new JTextArea();

  ClipboardViewer()
  {
    super("Clipboard");
    setIconImage(Icons.getIcon("Paste16.gif").getImage());
    bclearclipboard.setMnemonic('c');
    bclearclipboard.addActionListener(this);
    taClipBoard.setEditable(false);
    StructClipboard.getInstance().addChangeListener(this);

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    lowerpanel.add(bclearclipboard);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new JScrollPane(taClipBoard), BorderLayout.CENTER);
    pane.add(lowerpanel, BorderLayout.SOUTH);
    setSize(300, 400);

    Center.center(this, NearInfinity.getInstance().getBounds());
    taClipBoard.setText(StructClipboard.getInstance().toString());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bclearclipboard) {
      StructClipboard.getInstance().clear();
      taClipBoard.setText(StructClipboard.getInstance().toString());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent event)
  {
    taClipBoard.setText(StructClipboard.getInstance().toString());
  }

// --------------------- End Interface ChangeListener ---------------------
}

