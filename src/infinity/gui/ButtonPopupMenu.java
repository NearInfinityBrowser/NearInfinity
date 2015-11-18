// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public final class ButtonPopupMenu extends JButton
{
  private static final Comparator<JMenuItem> menuItemComparator = new Comparator<JMenuItem>() {
    @Override
    public int compare(JMenuItem item1, JMenuItem item2)
    {
      return item1.getText().compareToIgnoreCase(item2.getText());
    }
  };

  private final JPopupMenu menu = new JPopupMenu();
  private List<JMenuItem> items = new ArrayList<JMenuItem>();
  private JMenuItem selected;

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * @param text Text label for the button.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, JMenuItem[] menuItems)
  {
    this(text, menuItems, true);
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * @param text Text label for the button.
   * @param menuItems List of menu items.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, JMenuItem menuItems[], boolean sorted)
  {
    super(text);
    setMenuItems(menuItems, sorted);
    addMouseListener(new PopupListener());
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * @param text Text label for the button.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, List<JMenuItem> menuItems)
  {
    this(text, menuItems, true);
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * @param text Text label for the button.
   * @param menuItems List of menu items.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, List<JMenuItem> menuItems, boolean sorted)
  {
    super(text);
    setMenuItems(menuItems, sorted);
    addMouseListener(new PopupListener());
  }

  public JMenuItem getSelectedItem()
  {
    return selected;
  }

  /**
   * Replaces current list of menu items with the given list.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public void setMenuItems(JMenuItem[] menuItems)
  {
    setMenuItems(menuItems, true);
  }

  /**
   * Replaces current list of menu items with the given list.
   * @param menuItems List of menu items.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public void setMenuItems(JMenuItem[] menuItems, boolean sorted)
  {
    List<JMenuItem> list = new ArrayList<JMenuItem>();
    if (menuItems != null) {
      for (int i = 0; i < menuItems.length; i++) {
        list.add(menuItems[i]);
      }
    }
    setMenuItems(list, sorted);
  }

  /**
   * Replaces current list of menu items with the given list.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public void setMenuItems(List<JMenuItem> menuItems)
  {
    setMenuItems(menuItems, true);
  }

  /**
   * Replaces current list of menu items with the given list.
   * @param menuItems List of menu items.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public void setMenuItems(List<JMenuItem> menuItems, boolean sorted)
  {
    menu.removeAll();
    items.clear();
    if (menuItems != null) {
      for (int i = 0, count = menuItems.size(); i < count; i++) {
        items.add(menuItems.get(i));
      }
      if (sorted) {
        Collections.sort(items, menuItemComparator);
      }
    }
    MouseListener listener = new PopupItemListener();
    for (int i = 0, count = items.size(); i < count; i++) {
      final JMenuItem item = items.get(i);
      menu.add(item);
      item.addMouseListener(listener);
    }
  }

  /** Returns a read-only list of menu items. */
  public List<JMenuItem> getItems()
  {
    return Collections.unmodifiableList(items);
  }

  private void menuItemSelected(JMenuItem item)
  {
    if (item.isEnabled()) {
      selected = item;
      // Why won't the following line work?
      fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, selected, ItemEvent.SELECTED));
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private final class PopupItemListener extends MouseAdapter
  {
    private PopupItemListener()
    {
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      if (!e.isPopupTrigger() && ((JMenuItem)e.getSource()).contains(e.getX(), e.getY()))
        menuItemSelected((JMenuItem)e.getSource());
      menu.setVisible(false);
    }

    @Override
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

    @Override
    public void mousePressed(MouseEvent e)
    {
      if (!e.isPopupTrigger() && e.getComponent().isEnabled())
        menu.show(e.getComponent(), 0, -(int)menu.getPreferredSize().getHeight());
    }

    @Override
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

