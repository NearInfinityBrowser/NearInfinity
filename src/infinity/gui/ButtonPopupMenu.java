// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Comparator;

public final class ButtonPopupMenu extends JButton
{
  private final JPopupMenu menu = new JPopupMenu();
  private JMenuItem selected;

  public ButtonPopupMenu(String text, JMenuItem menuItems[])
  {
    super(text);
    setMenuItems(menuItems);
    addMouseListener(new PopupListener());
  }

  public JMenuItem getSelectedItem()
  {
    return selected;
  }

  public void setMenuItems(JMenuItem menuItems[])
  {
    menu.removeAll();
    Arrays.sort(menuItems, new Comparator<JMenuItem>()
    {
      public int compare(JMenuItem jMenuItem, JMenuItem jMenuItem1)
      {
        return jMenuItem.toString().compareToIgnoreCase(jMenuItem1.toString());
      }

      public boolean equals(Object obj)
      {
        return this == obj;
      }
    });
    MouseListener listener = new PopupItemListener();
    for (final JMenuItem menuItem : menuItems) {
      menu.add(menuItem);
      menuItem.addMouseListener(listener);
    }
//    menu.addSeparator();
  }

  private void menuItemSelected(JMenuItem item)
  {
    if (item.isEnabled()) {
      selected = item;
      // Why won't the following line work?
      fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, item, ItemEvent.SELECTED));
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private final class PopupItemListener extends MouseAdapter
  {
    private PopupItemListener()
    {
    }

    public void mouseReleased(MouseEvent e)
    {
      if (!e.isPopupTrigger() && ((JMenuItem)e.getSource()).contains(e.getX(), e.getY()))
        menuItemSelected((JMenuItem)e.getSource());
      menu.setVisible(false);
    }

    public void mouseExited(MouseEvent e)
    {
      JMenuItem item = (JMenuItem)e.getSource();
      item.setArmed(false);
      item.repaint();
    }
  }

  private final class PopupListener extends MouseAdapter
  {
    private PopupListener()
    {
    }

    public void mousePressed(MouseEvent e)
    {
      if (!e.isPopupTrigger() && e.getComponent().isEnabled())
        menu.show(e.getComponent(), 0, -(int)menu.getPreferredSize().getHeight());
    }

    public void mouseReleased(MouseEvent e)
    {
      if (!e.isPopupTrigger() && ((JButton)e.getSource()).contains(e.getX(), e.getY()) &&
          e.getComponent().isEnabled())
        menu.show(e.getComponent(), 0, -(int)menu.getPreferredSize().getHeight());
      else {
        menu.setVisible(false);
        Component components[] = menu.getComponents();
        for (final Component component : components) {
          if (component instanceof JMenuItem) {
            JMenuItem item = (JMenuItem)component;
            if (item.isArmed()) {
              menuItemSelected(item);
              break;
            }
          }
        }
      }
    }
  }
}

