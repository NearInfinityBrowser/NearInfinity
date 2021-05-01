// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;


/**
 * Provides a button component that pops up an associated window when the button is pressed.
 * It works similar to {@code ButtonPopupMenu}.
 */
public class ButtonPopupWindow extends JButton
{
  public enum Align {
    /** Use in {@link #setWindowAlignment(Align)}. Places the popup window below the button control. */
    BOTTOM,
    /** Use in {@link #setWindowAlignment(Align)}. Places the popup window on top of the button control. */
    TOP,
    /** Use in {@link #setWindowAlignment(Align)}. Places the popup window to the right of the button control. */
    RIGHT,
    /** Use in {@link #setWindowAlignment(Align)}. Places the popup window to the left of the button control. */
    LEFT,
  }

  private final PopupWindow window = new PopupWindow(this);
  private final List<PopupWindowListener> listeners = new ArrayList<>();

  private PopupWindow ignoredWindow;    // used to determine whether to hide the current window on lost focus
  private Align windowAlign;
  private Component content;

  public ButtonPopupWindow()
  {
    super();
    init(null, Align.BOTTOM);
  }

  public ButtonPopupWindow(Component content)
  {
    super();
    init(content, Align.BOTTOM);
  }

  public ButtonPopupWindow(Component content, Align align)
  {
    super();
    init(content, align);
  }

  public ButtonPopupWindow(Action a)
  {
    super(a);
    init(null, Align.BOTTOM);
  }

  public ButtonPopupWindow(Action a, Component content)
  {
    super(a);
    init(content, Align.BOTTOM);
  }

  public ButtonPopupWindow(Action a, Component content, Align align)
  {
    super(a);
    init(content, align);
  }

  public ButtonPopupWindow(Icon icon)
  {
    super(icon);
    init(null, Align.BOTTOM);
  }

  public ButtonPopupWindow(Icon icon, Component content)
  {
    super(icon);
    init(content, Align.BOTTOM);
  }

  public ButtonPopupWindow(Icon icon, Component content, Align align)
  {
    super(icon);
    init(content, align);
  }

  public ButtonPopupWindow(String text)
  {
    super(text);
    init(null, Align.BOTTOM);
  }

  public ButtonPopupWindow(String text, Component content)
  {
    super(text);
    init(content, Align.BOTTOM);
  }

  public ButtonPopupWindow(String text, Component content, Align align)
  {
    super(text);
    init(content, align);
  }

  public ButtonPopupWindow(String text, Icon icon)
  {
    super(text, icon);
    init(null, Align.BOTTOM);
  }

  public ButtonPopupWindow(String text, Icon icon, Component content)
  {
    super(text, icon);
    init(content, Align.BOTTOM);
  }

  public ButtonPopupWindow(String text, Icon icon, Component content, Align align)
  {
    super(text, icon);
    init(content, align);
  }

  /** Adds a new PopupWindowListener to this component. */
  public void addPopupWindowListener(PopupWindowListener listener)
  {
    if (listener != null) {
      if (listeners.indexOf(listener) < 0) {
        listeners.add(listener);
      }
    }
  }

  /** Returns all registered PopupWindowListener object. */
  public PopupWindowListener[] getPopupWindowListeners()
  {
    PopupWindowListener[] retVal = new PopupWindowListener[listeners.size()];
    for (int i = 0, size = listeners.size(); i < size; i++) {
      retVal[i] = listeners.get(i);
    }
    return retVal;
  }

  /** Removes a PopupWindowListener from this component. */
  public void removePopupWindowListener(PopupWindowListener listener)
  {
    if (listener != null) {
      int idx = listeners.indexOf(listener);
      if (idx >= 0) {
        listeners.remove(idx);
      }
    }
  }

  /**
   * Returns the popup window.
   * @return The popup window.
   */
  public Window getPopupWindow()
  {
    return window;
  }

  /**
   * Sets new content to the popup window. Old content will be removed.
   * @param content The new content of the popup window.
   */
  public void setContent(Component content)
  {
    displayWindow(false);
    window.getContentPane().removeAll();
    this.content = content;
    if (this.content != null) {
      window.getContentPane().add(this.content, BorderLayout.CENTER);
    }
    window.pack();
  }

  /**
   * Returns the currently assigned content of the popup window.
   * @return Current content of the popup window. Can be {@code null}.
   */
  public Component getContent()
  {
    return content;
  }

  /**
   * Shows the popup window if it hasn't been activated already.
   */
  public void showPopupWindow()
  {
    if (!window.isVisible()) {
      displayWindow(true);
    }
  }

  /**
   * Hides the popup window if it isn't hidden already.
   */
  public void hidePopupWindow()
  {
    if (window.isVisible()) {
      displayWindow(false);
    }
  }

  /**
   * Returns the default alignment of the popup window relative to the associated button control.
   * @return The default alignment of the popup window.
   */
  public Align getWindowAlignment()
  {
    return windowAlign;
  }

  /**
   * Specify a new default alignment of the popup window relative to the associated button control.
   * Use one of the constants ({@code Align.Bottom}, {@code Align.Top},
   * {@code Align.Left}, {@code Align.Right}).
   * {@code ButtonPopupWindow.BOTTOM} is the default.
   * @param align The new default alignment of the popup window.
   */
  public void setWindowAlignment(Align align)
  {
    switch (align) {
      case TOP:
      case LEFT:
      case RIGHT:
        windowAlign = align;
        break;
      default:
        windowAlign = Align.BOTTOM;
    }
  }

  /**
   * Registers a custom action for a specific keystroke.
   * @param key A unique key to link the keystroke to the action.
   * @param keyStroke The keystroke object defining the keyboard input sequence.
   * @param action The action to process.
   */
  public void addGlobalKeyStroke(Object key, KeyStroke keyStroke, Action action)
  {
    if (key != null && keyStroke != null && action != null) {
      final InputMap inputMap = window.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW);
      final ActionMap actionMap = window.getRootPane().getActionMap();
      inputMap.put(keyStroke, key);
      actionMap.put(key, action);
    }
  }

  /**
   * Removes a keystroke action from the window.
   * @param key The key which identifies the action.
   * @param keyStroke The keystroke which triggers the action.
   */
  public void removeGlobalKeyStroke(Object key, KeyStroke keyStroke)
  {
    if (key != null && keyStroke != null) {
      final InputMap inputMap = window.getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW);
      final ActionMap actionMap = window.getRootPane().getActionMap();
      inputMap.remove(keyStroke);
      actionMap.remove(key);
    }
  }

  protected void firePopupWindowListener(boolean becomeVisible)
  {
    PopupWindowEvent event = null;
    for (int i = 0, size = listeners.size(); i < size; i++) {
      if (event == null) {
        event = new PopupWindowEvent(this);
      }
      if (becomeVisible) {
        listeners.get(i).popupWindowWillBecomeVisible(event);
      } else {
        listeners.get(i).popupWindowWillBecomeInvisible(event);
      }
    }
  }

  private void init(Component content, Align align)
  {
    ignoredWindow = window;
    window.addWindowFocusListener(new ButtonWindowListener());

    setWindowAlignment(align);

    // close popup window on ESC
    JRootPane pane = window.getRootPane();
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), pane);
    pane.getActionMap().put(pane, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event)
      {
        displayWindow(false);
      }
    });

    setContent(content);
    addMouseListener(new ButtonPopupListener());
  }

  private void displayWindow(boolean state)
  {
    if (state == true) {
      showWindow();
    } else {
      hideWindow();
    }
  }

  private void showWindow()
  {
    if (!window.isVisible()) {
      firePopupWindowListener(true);

      // notify the parent window to stay open if of type PopupWindow
      PopupWindow parent = getParentPopupWindow(window);
      if (parent != null) {
        parent.getButton().setIgnoredWindow(window);
      }

      // determine correct window location
      DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
      Dimension dimScreen = new Dimension(dm.getWidth(), dm.getHeight());
      Rectangle rectButton = new Rectangle(getLocationOnScreen(), getSize());
      Dimension dimWin = window.getSize();
      Point location = new Point();

      if (windowAlign == Align.RIGHT) {
        if (dimWin.width >= dimScreen.width - rectButton.x - rectButton.width) {
          // show left of the button
          location.x = rectButton.x - dimWin.width;
        } else {
          // show right of the button
          location.x = rectButton.x + rectButton.width;
        }
      } else if (windowAlign == Align.LEFT) {
        if (dimWin.width > rectButton.x) {
          // show right of the button
          location.x = rectButton.x + rectButton.width;
        } else {
          // show left of the button
          location.x = rectButton.x - dimWin.width;
        }
      } else if (windowAlign == Align.TOP) {
        if (dimWin.height > rectButton.y) {
          // show below button
          location.y = rectButton.y + rectButton.height;
        } else {
          // show below button
          location.y = rectButton.y - dimWin.height;
        }
      } else {    // defaults to Align.Bottom
        if (dimWin.height >= dimScreen.height - rectButton.y - rectButton.height) {
          // show on top of button
          location.y = rectButton.y - dimWin.height;
        } else {
          // show below button
          location.y = rectButton.y + rectButton.height;
        }
      }

      if (windowAlign == Align.RIGHT || windowAlign == Align.LEFT) {
        if (dimWin.height < dimScreen.height - rectButton.y) {
          // align with button vertically
          location.y = rectButton.y;
        } else {
          location.y = dimScreen.height - dimWin.height;
        }
      } else {
        // considering locale-specific horizontal orientations
        if (ComponentOrientation.getOrientation(Locale.getDefault()) == ComponentOrientation.RIGHT_TO_LEFT) {
          if (rectButton.x + rectButton.width >= dimWin.width) {
            // align with button horizontally
            location.x = rectButton.x + rectButton.width - dimWin.width;
          } else {
            location.x = 0;
          }
        } else {    // default: left-to-right orientation
          if (dimWin.width < dimScreen.width - rectButton.x) {
            // align with button horizontally
            location.x = rectButton.x;
          } else {
            location.x = dimScreen.width - dimWin.width;
          }
        }
      }

      // translate absolute to relative coordinates
      if (window.getParent() != null) {
        location.x -= window.getParent().getLocation().x;
        location.y -= window.getParent().getLocation().y;
      }
      window.setLocation(location);

      window.setVisible(true);
      window.requestFocusInWindow();
    }
  }

  private void hideWindow()
  {
    if (window.isVisible()) {
      firePopupWindowListener(false);
      window.setVisible(false);
      window.getButton().requestFocusInWindow();
    }
  }

  // Returns the direct parent of the specified window if of type PopupWindow, or null if not available
  private PopupWindow getParentPopupWindow(PopupWindow wnd)
  {
    if (wnd != null) {
      Window parent = SwingUtilities.getWindowAncestor(wnd.getButton());
      if (parent != null && parent instanceof PopupWindow) {
        return (PopupWindow)parent;
      }
    }
    return null;
  }

  private void setIgnoredWindow(PopupWindow wnd)
  {
    ignoredWindow = wnd;
  }

  private void notifyCloseWindow(Window wnd)
  {
    if (wnd != window) {
      displayWindow(false);
      PopupWindow parent = getParentPopupWindow(window);
      if (parent != null) {
        parent.getButton().notifyCloseWindow(wnd);
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private static final class PopupWindow extends JFrame
  {
    private ButtonPopupWindow button;

    public PopupWindow(ButtonPopupWindow button)
    {
      this.button = button;
      setUndecorated(true);
      getRootPane().setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
    }

    public ButtonPopupWindow getButton()
    {
      return button;
    }
  }


  private final class ButtonPopupListener extends MouseAdapter
  {
    public ButtonPopupListener() { super(); }

    @Override
    public void mousePressed(MouseEvent event)
    {
      if (event.getSource() instanceof ButtonPopupWindow &&
          event.getButton() == MouseEvent.BUTTON1 &&
          !event.isPopupTrigger() &&
          event.getComponent().isEnabled() &&
          window != null) {
        displayWindow(!window.isVisible());
      }
    }
  }


  private final class ButtonWindowListener extends WindowAdapter
  {
    public ButtonWindowListener() { super(); }

    @Override
    public void windowLostFocus(WindowEvent event)
    {
      if (event.getWindow() == window &&
          event.getOppositeWindow() != ignoredWindow) {
        notifyCloseWindow(event.getOppositeWindow());
      }
      ignoredWindow = window;   // reset state
    }
  }
}
