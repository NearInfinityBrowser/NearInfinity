// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

final class LinkButton extends JLabel implements MouseListener, ActionListener
{
  private final List<ActionListener> listeners = new ArrayList<ActionListener>();
  private ResourceEntry entry;

  LinkButton(ResourceRef resourceRef)
  {
    this(resourceRef.toString());
    entry = ResourceFactory.getInstance().getResourceEntry(resourceRef.getResourceName());
    setHorizontalAlignment(SwingConstants.LEFT);
    addActionListener(this);
    if (entry == null) {
      setText(resourceRef.toString());
      setEnabled(false);
      setToolTipText("Resource not found");
      setCursor(null);
      removeMouseListener(this);
    }
  }

  private LinkButton(String buttonText)
  {
    super("<html><a href=" + buttonText + '>' + buttonText + "</a></html");
    addMouseListener(this);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent e)
  {
    String cmd = e.getActionCommand();
    if ((cmd == null) || cmd.equals("OPEN_NEW")) {
      new ViewFrame(((LinkButton)e.getSource()).getTopLevelAncestor(),
                    ResourceFactory.getResource(entry));
    }
    else if (cmd.equals("OPEN")) {
      NearInfinity.getInstance().showResourceEntry(entry);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface MouseListener ---------------------

  public void mouseClicked(MouseEvent e)
  {
    String cmd = "OPEN_NEW";
    if ((e.getButton() == MouseEvent.BUTTON2)
     || (e.getButton() == MouseEvent.BUTTON3)) {
      cmd = "OPEN";
    }

    ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd);
    for (int i = 0; i < listeners.size(); i++)
      listeners.get(i).actionPerformed(event);
  }

  public void mousePressed(MouseEvent e)
  {
  }

  public void mouseReleased(MouseEvent e)
  {
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

// --------------------- End Interface MouseListener ---------------------

  public void removeActionListener(ActionListener listener)
  {
    listeners.remove(listener);
  }

  private void addActionListener(ActionListener listener)
  {
    listeners.add(listener);
  }
}

