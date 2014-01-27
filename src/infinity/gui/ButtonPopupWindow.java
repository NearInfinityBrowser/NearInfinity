package infinity.gui;

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
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;


/**
 * Provides a button component that pops up an associated window when the button is pressed.
 * It works similar to <code>ButtonPopupMenu</code>.
 * @author argent77
 */
public class ButtonPopupWindow extends JButton
{
  /**
   * Use in {@link #setWindowAlignment(int)}. Places the popup window below the button control.
   */
  public static final int BOTTOM  = 0;    // default
  /**
   * Use in {@link #setWindowAlignment(int)}. Places the popup window on top of the button control.
   */
  public static final int TOP     = 1;
  /**
   * Use in {@link #setWindowAlignment(int)}. Places the popup window to the right of the button control.
   */
  public static final int RIGHT   = 2;
  /**
   * Use in {@link #setWindowAlignment(int)}. Places the popup window to the left of the button control.
   */
  public static final int LEFT    = 3;

  private final PopupWindow window = new PopupWindow(this);

  private PopupWindow ignoredWindow;    // used to determine whether to hide the current window on lost focus
  private int windowAlign;

  public ButtonPopupWindow()
  {
    super();
    init(null, BOTTOM);
  }

  public ButtonPopupWindow(Component content)
  {
    super();
    init(content, BOTTOM);
  }

  public ButtonPopupWindow(Component content, int align)
  {
    super();
    init(content, align);
  }

  public ButtonPopupWindow(Action a)
  {
    super(a);
    init(null, BOTTOM);
  }

  public ButtonPopupWindow(Action a, Component content)
  {
    super(a);
    init(content, BOTTOM);
  }

  public ButtonPopupWindow(Action a, Component content, int align)
  {
    super(a);
    init(content, align);
  }

  public ButtonPopupWindow(Icon icon)
  {
    super(icon);
    init(null, BOTTOM);
  }

  public ButtonPopupWindow(Icon icon, Component content)
  {
    super(icon);
    init(content, BOTTOM);
  }

  public ButtonPopupWindow(Icon icon, Component content, int align)
  {
    super(icon);
    init(content, align);
  }

  public ButtonPopupWindow(String text)
  {
    super(text);
    init(null, BOTTOM);
  }

  public ButtonPopupWindow(String text, Component content)
  {
    super(text);
    init(content, BOTTOM);
  }

  public ButtonPopupWindow(String text, Component content, int align)
  {
    super(text);
    init(content, align);
  }

  public ButtonPopupWindow(String text, Icon icon)
  {
    super(text, icon);
    init(null, BOTTOM);
  }

  public ButtonPopupWindow(String text, Icon icon, Component content)
  {
    super(text, icon);
    init(content, BOTTOM);
  }

  public ButtonPopupWindow(String text, Icon icon, Component content, int align)
  {
    super(text, icon);
    init(content, align);
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
    if (content != null) {
      window.getContentPane().add(content, BorderLayout.CENTER);
    }
    window.pack();
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
  public int getWindowAlignment()
  {
    return windowAlign;
  }

  /**
   * Specify a new default alignment of the popup window relative to the associated button control.
   * Use one of the constants (<code>ButtonPopupWindow.BOTTOM</code>, <code>ButtonPopupWindow.TOP</code>,
   * <code>ButtonPopupWindow.LEFT</code>, <code>ButtonPopupWindow.RIGHT</code>).
   * <code>ButtonPopupWindow.BOTTOM</code> is the default.
   * @param align The new default alignment of the popup window.
   */
  public void setWindowAlignment(int align)
  {
    switch (align) {
      case TOP:
      case LEFT:
      case RIGHT:
        windowAlign = align;
        break;
      default:
        windowAlign = BOTTOM;
    }
  }

  private void init(Component content, int align)
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

    if (windowAlign == RIGHT) {
      if (dimWin.width >= dimScreen.width - rectButton.x - rectButton.width) {
        // show left of the button
        location.x = rectButton.x - dimWin.width;
      } else {
        // show right of the button
        location.x = rectButton.x + rectButton.width;
      }
    } else if (windowAlign == LEFT) {
      if (dimWin.width > rectButton.x) {
        // show right of the button
        location.x = rectButton.x + rectButton.width;
      } else {
        // show left of the button
        location.x = rectButton.x - dimWin.width;
      }
    } else if (windowAlign == TOP) {
      if (dimWin.height > rectButton.y) {
        // show below button
        location.y = rectButton.y + rectButton.height;
      } else {
        // show below button
        location.y = rectButton.y - dimWin.height;
      }
    } else {    // defaults to BOTTOM
      if (dimWin.height >= dimScreen.height - rectButton.y - rectButton.height) {
        // show on top of button
        location.y = rectButton.y - dimWin.height;
      } else {
        // show below button
        location.y = rectButton.y + rectButton.height;
      }
    }

    if (windowAlign == RIGHT || windowAlign == LEFT) {
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

  private void hideWindow()
  {
    window.setVisible(false);
    window.getButton().requestFocusInWindow();
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
