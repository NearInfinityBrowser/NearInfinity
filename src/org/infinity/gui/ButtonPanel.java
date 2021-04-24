// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.infinity.icon.Icons;

/**
 * Encapsulates a flexible button panel that can be used to streamline the bottom panel present
 * in almost all resource views.
 */
public class ButtonPanel extends JPanel
{
  /** Predefined controls that can be added or inserted into the button panel. */
  public enum Control {
    /** "Find..." (JButton) */
    FIND_BUTTON,
    /** "Find..." (ButtonPopupMenu) */
    FIND_MENU,
    /** "Find references..." (JButton) */
    FIND_REFERENCES,
    /** "View/Edit..." (JButton) */
    VIEW_EDIT,
    /** "Print..." (JButton) */
    PRINT,
    /** "Export..." (JButton) */
    EXPORT_BUTTON,
    /** "Export..." (ButtonPopupMenu) */
    EXPORT_MENU,
    /** "Save" (JButton) */
    SAVE,
    /** "Add..." (ButtonPopupMenu) */
    ADD,
    /** "Remove" (JButton) */
    REMOVE,
    /** "Trim spaces" (JButton) */
    TRIM_SPACES,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_1,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_2,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_3,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_4,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_5,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_6,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_7,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_8,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_9,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_10,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_11,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_12,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_13,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_14,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_15,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_16,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_17,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_18,
    /** Unused control type. Can be used to identify custom controls. */
    CUSTOM_19,
  }

  private static final int DefaultGapSize = 4;

  private final List<Entry> listControls = new ArrayList<>();

  private int gapSize;

  /**
   * Creates a component of the specified type.
   * @param type One of the predefined types defined in the enum {@code Control}.
   * @return The resulting component.
   */
  public static JComponent createControl(Control type)
  {
    JComponent retVal = null;
    switch (type) {
      case ADD:
      {
        ButtonPopupMenu bpm = new ButtonPopupMenu("Add...", new JMenuItem[]{});
        bpm.setIcon(Icons.getIcon(Icons.ICON_ADD_16));
        retVal = bpm;
        break;
      }
      case EXPORT_BUTTON:
      {
        JButton b = new JButton("Export...", Icons.getIcon(Icons.ICON_EXPORT_16));
        b.setToolTipText("NB! Will export last *saved* version");
        b.setMnemonic('e');
        retVal = b;
        break;
      }
      case EXPORT_MENU:
      {
        ButtonPopupMenu bpm = new ButtonPopupMenu("Export...", new JMenuItem[]{});
        bpm.setIcon(Icons.getIcon(Icons.ICON_EXPORT_16));
        retVal = bpm;
        break;
      }
      case FIND_BUTTON:
      {
        JButton b = new JButton("Find...", Icons.getIcon(Icons.ICON_FIND_16));
        b.setMnemonic('f');
        retVal = b;
        break;
      }
      case FIND_MENU:
      {
        ButtonPopupMenu bpm = new ButtonPopupMenu("Find...", new JMenuItem[]{});
        bpm.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
        retVal = bpm;
        break;
      }
      case FIND_REFERENCES:
      {
        JButton b = new JButton("Find references...", Icons.getIcon(Icons.ICON_FIND_16));
        b.setMnemonic('f');
        retVal = b;
        break;
      }
      case PRINT:
      {
        JButton b =  new JButton(Icons.getIcon(Icons.ICON_PRINT_16));
        b.setMargin(new Insets(b.getMargin().top, 3, b.getMargin().bottom, 3));
        b.setToolTipText("Print");
        retVal = b;
        break;
      }
      case REMOVE:
      {
        JButton b = new JButton("Remove", Icons.getIcon(Icons.ICON_REMOVE_16));
        b.setMnemonic('r');
        retVal = b;
        break;
      }
      case SAVE:
      {
        JButton b = new JButton("Save", Icons.getIcon(Icons.ICON_SAVE_16));
        b.setMnemonic('a');
        retVal = b;
        break;
      }
      case TRIM_SPACES:
      {
        retVal = new JButton("Trim spaces", Icons.getIcon(Icons.ICON_REFRESH_16));
        break;
      }
      case VIEW_EDIT:
      {
        JButton b = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
        b.setMnemonic('v');
        retVal = b;
        break;
      }
      default:
    }
    return retVal;
  }


  public ButtonPanel()
  {
    gapSize = DefaultGapSize;
    setLayout(new FlowLayout(FlowLayout.CENTER, gapSize, DefaultGapSize));
  }

  /** Returns the gap size between the components */
  public int getGapSize()
  {
    return gapSize;
  }

  /** Sets the gap size between the components. */
  public void setGapSize(int size)
  {
    if (size < 0) size = DefaultGapSize;
    if (size != getGapSize()) {
      gapSize = size;
      updateButtonBar();
    }
  }

  /**
   * Adds one of the predefined controls to the button panel.
   * @param type The predefined control to add.
   * @return The added component, or {@code null} on error.
   */
  public JComponent addControl(Control type)
  {
    return addControl(getControlCount(), type);
  }

  /**
   * Adds the specified component to the button panel.
   * @param component The custom component to add.
   * @return The added component, or {@code null} on error.
   */
  public JComponent addControl(JComponent component)
  {
    return addControl(getControlCount(), component);
  }

  /**
   * Adds the specified component to the button panel and associates it with a specific type.
   * @param component The custom component to add.
   * @param type The {@code Control} type to attach.
   * @return The added component, or {@code null} on error.
   */
  public JComponent addControl(JComponent component, Control type)
  {
    return addControl(getControlCount(), component, type);
  }

  /**
   * Inserts one of the predefined control into the button panel at the specified position if possible.
   * @param position The requested position where to place the control (ordered from left to right)
   * @param type The predefined control to add.
   * @return The inserted component, or {@code null} on error.
   */
  public JComponent addControl(int position, Control type)
  {
    return addControl(position, createControl(type), type);
  }

  /**
   * Inserts the specified control into the button panel at the specified position if possible.
   * @param position The requested position where to place the control (ordered from left to right)
   * @param component The custom component to add.
   * @return The inserted component, or {@code null} on error.
   */
  public JComponent addControl(int position, JComponent component)
  {
    return addControl(position, component, null);
  }

  /**
   * Inserts the specified control into the button panel at the specified position if possible and
   * associates it with a specific type.
   * @param position The requested position where to place the control (ordered from left to right)
   * @param component The custom component to add.
   * @param type The {@code Control} type to attach.
   * @return The inserted component, or {@code null} on error.
   */
  public JComponent addControl(int position, JComponent component, Control type)
  {
    if (component != null && position >= 0 && position <= listControls.size()) {
      listControls.add(position, new Entry(component, type));
      updateButtonBar();
      return component;
    }
    return null;
  }


  /** Returns the number of assigned controls in the button panel. */
  public int getControlCount()
  {
    return listControls.size();
  }

  /**
   * Returns the position of the specified control in the button panel.
   * Returns -1 if the control does not exist.
   */
  public int getControlPosition(JComponent component)
  {
    int retVal = -1;
    if (component != null) {
      retVal = getControlIndex(component);
    }
    return retVal;
  }

  /**
   * Returns the component at the specified index.
   * @param index The index of the component.
   * @return The component or {@code null} if index is out of bounds.
   */
  public JComponent getControl(int index)
  {
    if (index >= 0 && index < listControls.size()) {
      return listControls.get(index).getComponent();
    }
    return null;
  }

  /**
   * Returns the first available component of the specified type.
   * @param type One of the predefined types defined in the enum {@code Control}.
   *             Specifying {@code null} will return the first available non-predefined component.
   * @return The component or {@code null} if not found.
   */
  public JComponent getControlByType(Control type)
  {
    for (int i = 0; i < listControls.size(); i++) {
      if (listControls.get(i).getType() == type) {
        return listControls.get(i).getComponent();
      }
    }
    return null;
  }

  /**
   * Returns all available components of the specified type.
   * @param type One of the predefined types defined in the enum {@code Control}.
   * @return An array of matching components. The array will be empty if no component has been found.
   */
  public JComponent[] getControlsByType(Control type)
  {
    // determining number of available components
    int max = 0;
    for (int i = 0; i < listControls.size(); i++) {
      if (listControls.get(i).getType() == type) {
        max++;
      }
    }

    // getting components
    JComponent[] retVal = new JComponent[max];
    int cnt = 0;
    for (int i = 0; i < listControls.size() && cnt < max; i++) {
      if (listControls.get(i).getType() == type) {
        retVal[cnt] = listControls.get(i).getComponent();
        cnt++;
      }
    }

    return retVal;
  }

  /** Removes the control at the specified position from the button panel. */
  public void removeControl(int position)
  {
    if (position >= 0 && position < listControls.size()) {
      listControls.remove(position);
    }
    updateButtonBar();
  }

  /** Removes the specified control from the button panel. */
  public void removeControl(JComponent control)
  {
    if (control != null) {
      int idx = getControlIndex(control);
      removeControl(idx);
    }
  }

  /** Removes all controls from the button panel. */
  public void removeAllControls()
  {
    listControls.clear();
    updateButtonBar();
  }

  /**
   * Moves the specified control to another position.
   * @param control The control to move.
   * @param newPosition The new position of the control.
   */
  public void moveControl(JComponent control, int newPosition)
  {
    if (control != null) {
      int idx = getControlIndex(control);
      moveControl(idx, newPosition);
    }
  }

  /**
   * Moves a control to another position.
   * @param curPosition The position of the control to move.
   * @param newPosition The new position of the control.
   */
  public void moveControl(int curPosition, int newPosition)
  {
    if (curPosition >= 0 && curPosition < listControls.size()) {
      if (newPosition < 0) newPosition = 0;
      if (newPosition >= listControls.size()) newPosition = listControls.size() - 1;
      if (curPosition != newPosition) {
        Entry e = listControls.get(curPosition);
        listControls.remove(curPosition);
        listControls.add(newPosition, e);
      }
    }
  }


  // Recreates the button panel from the available components
  private void updateButtonBar()
  {
    removeAll();

    setLayout(new FlowLayout(FlowLayout.CENTER, gapSize, DefaultGapSize));
    for (int i = 0; i < listControls.size(); i++) {
      add(listControls.get(i).getComponent());
    }

    revalidate();
  }

  // Returns the list index of the specified component, returns -1 if not found.
  private int getControlIndex(JComponent comp)
  {
    int retVal = -1;
    if (comp != null) {
      for (int i = 0; i < listControls.size(); i++) {
        if (listControls.get(i).getComponent() == comp) {
          retVal = i;
          break;
        }
      }
    }
    return retVal;
  }


//-------------------------- INNER CLASSES --------------------------

  private class Entry
  {
    private JComponent component;
    private Control type;

    public Entry(JComponent component, Control type)
    {
      this.component = component;
      this.type = type;
    }

    public JComponent getComponent() { return component; }

    public Control getType() { return type; }
  }
}
