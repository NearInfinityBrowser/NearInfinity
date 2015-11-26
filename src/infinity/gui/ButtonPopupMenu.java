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
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

/**
 * Provides a button component that pops up an associated menu when the button is pressed.
 */
public final class ButtonPopupMenu extends JButton
{
  public enum Align {
    /** Show the menu below the button. */
    Top,
    /** Show the menu on top of the button. */
    Bottom,
  }

  private static final Comparator<JComponent> menuItemComparator = new Comparator<JComponent>() {
    @Override
    public int compare(JComponent item1, JComponent item2)
    {
      if (item1 instanceof JMenuItem && item2 instanceof JMenuItem) {
        return ((JMenuItem)item1).getText().compareToIgnoreCase(((JMenuItem)item2).getText());
      } else {
        return 0;
      }
    }
  };

  private final JPopupMenu menu = new JPopupMenu();
  private List<JComponent> items = new ArrayList<JComponent>();
  private JMenuItem selected;
  private Align menuAlign;

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param text Text label for the button.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, JComponent[] menuItems)
  {
    this(text, menuItems, true, Align.Top);
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param text Text label for the button.
   * @param menuItems List of menu items or separators.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, JComponent menuItems[], boolean sorted, Align align)
  {
    super(text);
    this.menuAlign = align;
    setMenuItems(menuItems, sorted);
    addMouseListener(new PopupListener());
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param text Text label for the button.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, List<? extends JComponent> menuItems)
  {
    this(text, menuItems, true, Align.Top);
  }

  /**
   * Constructs a new ButtonPopupMenu control with the given menu items.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param text Text label for the button.
   * @param menuItems List of menu items.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public ButtonPopupMenu(String text, List<? extends JComponent> menuItems, boolean sorted, Align align)
  {
    super(text);
    this.menuAlign = align;
    setMenuItems(menuItems, sorted);
    addMouseListener(new PopupListener());
  }

  public JMenuItem getSelectedItem()
  {
    return selected;
  }

  /**
   * Replaces current list of menu items with the given list.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param menuItems List of menu items. Items will be sorted alphabetically before adding to the button.
   */
  public void setMenuItems(JComponent[] menuItems)
  {
    setMenuItems(menuItems, true);
  }

  /**
   * Replaces current list of menu items with the given list.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param menuItems List of menu items or separators.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public void setMenuItems(JComponent[] menuItems, boolean sorted)
  {
    List<JComponent> list = new ArrayList<JComponent>();
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
  public void setMenuItems(List<? extends JComponent> menuItems)
  {
    setMenuItems(menuItems, true);
  }

  /**
   * Replaces current list of menu items with the given list.
   * <b>Note:</b> Only JMenuItem and JSeparator instances are supported.
   * @param menuItems List of menu items or separators.
   * @param sorted Indicates whether to sort items alphabetically before adding to the button.
   */
  public void setMenuItems(List<? extends JComponent> menuItems, boolean sorted)
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
      final JComponent item = items.get(i);
      if (item instanceof JMenuItem || item instanceof JSeparator) {
        menu.add(item);
        if (item instanceof JMenuItem) {
          item.addMouseListener(listener);
        }
      }
    }
  }

  /** Returns an unfiltered read-only list of menu items and separators. */
  public List<? extends JComponent> getItems()
  {
    return Collections.unmodifiableList(items);
  }

  /** Returns a filtered read-only list of menu items. JSeparator instances will be filtered out. */
  public List<JMenuItem> getMenuItems()
  {
    List<JMenuItem> list = new ArrayList<JMenuItem>(items.size());
    for (final JComponent c: items) {
      if (c instanceof JMenuItem) {
        list.add((JMenuItem)c);
      }
    }
    return list;
  }

  /** Returns the alignment of the menu relative to the button. */
  public Align getMenuAlignment()
  {
    return menuAlign;
  }

  /** Sets how the menu should be aligned relative to the button. */
  public void setMenuAlignment(Align align)
  {
    this.menuAlign = align;
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
      if (!e.isPopupTrigger() && e.getComponent().isEnabled()) {
        if (getMenuAlignment() == Align.Bottom) {
          menu.show(e.getComponent(), 0, e.getComponent().getSize().height);
        } else {
          menu.show(e.getComponent(), 0, -menu.getPreferredSize().height);
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      if (!e.isPopupTrigger() && ((JButton)e.getSource()).contains(e.getX(), e.getY()) &&
          e.getComponent().isEnabled()) {
        if (getMenuAlignment() == Align.Bottom) {
          menu.show(e.getComponent(), 0, e.getComponent().getSize().height);
        } else {
          menu.show(e.getComponent(), 0, -menu.getPreferredSize().height);
        }
      } else {
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

