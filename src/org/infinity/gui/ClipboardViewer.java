// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.util.Misc;
import org.infinity.util.StructClipboard;

final class ClipboardViewer extends ChildFrame implements ActionListener, ChangeListener
{
  private final JButton bclearclipboard = new JButton("Clear", Icons.getIcon(Icons.ICON_NEW_16));
  private final InfinityTextArea taClipBoard = new InfinityTextArea(false);

  ClipboardViewer()
  {
    super("Clipboard");
    setIconImage(Icons.getIcon(Icons.ICON_PASTE_16).getImage());
    bclearclipboard.setMnemonic('c');
    bclearclipboard.addActionListener(this);
    taClipBoard.setHighlightCurrentLine(false);
    taClipBoard.setEditable(false);
    StructClipboard.getInstance().addChangeListener(this);

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    lowerpanel.add(bclearclipboard);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new InfinityScrollPane(taClipBoard, false), BorderLayout.CENTER);
    pane.add(lowerpanel, BorderLayout.SOUTH);
    setSize(Misc.getScaledValue(300), Misc.getScaledValue(400));

    Center.center(this, NearInfinity.getInstance().getBounds());
    taClipBoard.setText(StructClipboard.getInstance().toString());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bclearclipboard) {
      StructClipboard.getInstance().clear();
      taClipBoard.setText(StructClipboard.getInstance().toString());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    taClipBoard.setText(StructClipboard.getInstance().toString());
  }

// --------------------- End Interface ChangeListener ---------------------
}

