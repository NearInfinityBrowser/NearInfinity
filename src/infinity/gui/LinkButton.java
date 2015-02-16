// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.datatype.ResourceRef;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

final public class LinkButton extends JLabel implements MouseListener, ActionListener
{
  private final List<ActionListener> listeners = new ArrayList<ActionListener>();
  private ResourceEntry entry;

  public LinkButton(ResourceRef resourceRef)
  {
    super();
    setHorizontalAlignment(SwingConstants.LEFT);
    setResource(resourceRef);
  }

  public LinkButton(String resourceName)
  {
    super();
    setHorizontalAlignment(SwingConstants.LEFT);
    setResource(resourceName);
  }

  /** Creates a link from the specified resource reference. */
  public void setResource(ResourceRef resourceRef)
  {
    if (resourceRef != null) {
      setResource(resourceRef, resourceRef.toString());
    } else {
      setResource(null, "");
    }
  }

  /** Attempts to create a link from the specified resource name. */
  public void setResource(String resourceName)
  {
    setResource(null, resourceName);
  }

  // Uses either ResourceRef or String to create a link
  private void setResource(ResourceRef resource, String resourceName)
  {
    removeActionListener(this);
    if (resource != null) {
      entry = ResourceFactory.getResourceEntry(resource.getResourceName());
      if (resourceName == null) {
        resourceName = resource.toString();
      }
    } else {
      entry = ResourceFactory.getResourceEntry(resourceName);
      if (resourceName == null) {
        resourceName = "";
      }
    }
    if (entry != null) {
      addActionListener(this);
      setLink(resourceName, entry.getResourceName(), true);
      setEnabled(true);
      setToolTipText(null);
    } else {
      setLink(resourceName, null, false);
      setEnabled(false);
      setToolTipText("Resource not found");
    }
  }

  // Sets link or label text, depending on arguments
  private void setLink(String text, String resource, boolean asLink)
  {
    removeMouseListener(this);
    setCursor(null);

    if (text == null) {
      text = resource;
    }

    if (!asLink) {
      setText(text);
    } else if (resource != null && !resource.isEmpty()) {
      setText("<html><a href=" + resource + '>' + text + "</a></html");
      addMouseListener(this);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    } else {
      setText("");
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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

  @Override
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

  @Override
  public void mousePressed(MouseEvent e)
  {
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
  }

  @Override
  public void mouseEntered(MouseEvent e)
  {
  }

  @Override
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

