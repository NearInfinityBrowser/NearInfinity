// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.infinity.NearInfinity;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Closeable;
import org.infinity.resource.TextResource;
import org.infinity.resource.Viewable;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.graphics.BamResource;
import org.tinylog.Logger;

public class ChildFrame extends JFrame {
  private static final List<ChildFrame> WINDOWS = new ArrayList<>();

  // Storage for size and placement of the last created child frame
  private static Dimension lastFrameSize = null;
  private static Point lastFrameLocation = null;

  private final boolean closeOnInvisible;

  /** Closes all child windows. */
  public static int closeWindows() {
    return closeWindow((Class<ChildFrame>)null);
  }

  /** Closes all windows of the specified window class. */
  public static int closeWindow(Class<ChildFrame> frameClass) {
    int retVal = 0;
    WindowEvent event = new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING);
    for (Iterator<ChildFrame> i = WINDOWS.iterator(); i.hasNext();) {
      ChildFrame frame = i.next();
      if (frameClass == null || frame.getClass() == frameClass) {
        i.remove();
        retVal++;
        closeWindow(frame, event);
      }
    }
    return retVal;
  }

  /** Closes the specified child window. */
  public static boolean closeWindow(ChildFrame frame) {
    boolean retVal = false;
    if (frame != null) {
      retVal = WINDOWS.remove(frame);
      if (retVal) {
        closeWindow(frame, new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING));
      }
    }
    return retVal;
  }

  /**
   * Returns first window with specified class or {@code null} if such window do not exist.
   *
   * @param frameClass Runtime class of window to find
   * @param <T>        Class of window to find
   *
   * @return Finded window or {@code null}
   */
  public static <T extends ChildFrame> T getFirstFrame(Class<T> frameClass) {
    for (final ChildFrame frame : WINDOWS) {
      if (frame.getClass() == frameClass) {
        return frameClass.cast(frame);
      }
    }
    return null;
  }

  /**
   * Returns an iterator over the registered {@code ChildFrame} instances that are compatible with the specified class
   * type.
   *
   * @param <T>        Class of the filtered instances
   * @param frameClass Runtime class of the windows to filter
   * @return An iterator over matching instances. Returns {@code null} if the parameter is {@code null}.
   */
  public static <T extends ChildFrame> Iterator<T> getFrameIterator(Class<T> frameClass) {
    if (frameClass != null) {
      return WINDOWS.stream().filter(frameClass::isInstance).map(frameClass::cast).iterator();
    }
    return null;
  }

  /**
   * Returns an iterator over the registered {@code ChildFrame} instances that are matching the given predicate.
   *
   * @param pred Predicate used to filter registered windows
   * @return An iterator over matching {@code ChildFrame} instances
   */
  public static Iterator<ChildFrame> getFrameIterator(Predicate<ChildFrame> pred) {
    if (pred != null) {
      return WINDOWS.stream().filter(pred).iterator();
    } else {
      return WINDOWS.iterator();
    }
  }

  /**
   * Shows first window of specified class. If window do not yet exists, create it with {@code init} function and then
   * shows.
   *
   * @param frameClass Runtime class of window to show
   * @param init       Function that will be called, if window with specified class no not exist
   * @param <T>        Class of window to show
   *
   * @return Finded or created window
   *
   * @see #setVisible(Class, boolean, Supplier)
   */
  public static <T extends ChildFrame> T show(Class<T> frameClass, Supplier<T> init) {
    return setVisible(frameClass, true, init);
  }

  /**
   * Sets visibility to first window with specified class. If such window do not yet exist and it need to be show
   * ({@code isVisible == true}) then it created with {@code init} function. Otherwise nothing is do
   *
   * @param frameClass Runtime class of window to show
   * @param isVisible  New visibility of specified window
   * @param init       Function that will be called, if window with specified class no not exist
   * @param <T>        Class of window to show
   *
   * @return Finded of created window. May be {@code null} if window do not exist and it must be hidden
   *
   * @see #show(Class, Supplier)
   */
  public static <T extends ChildFrame> T setVisible(Class<T> frameClass, boolean isVisible, Supplier<T> init) {
    T frame = getFirstFrame(frameClass);
    if (frame == null && isVisible) {
      frame = init.get();
    }
    if (frame != null) {
      frame.setVisible(isVisible);
    }
    return frame;
  }

  public static void updateWindowGUIs() {
    for (final ChildFrame frame : WINDOWS) {
      SwingUtilities.updateComponentTreeUI(frame);
    }
  }

  protected ChildFrame(String title) {
    this(title, false);
  }

  public ChildFrame(String title, boolean closeOnInvisible) {
    super(title);
    setIconImages(NearInfinity.getInstance().getIconImages());
    this.closeOnInvisible = closeOnInvisible;
    WINDOWS.add(this);
    JPanel pane = new JPanel();
    setContentPane(pane);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), pane);
    pane.getActionMap().put(pane, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (ChildFrame.this.closeOnInvisible) {
          try {
            if (!ChildFrame.this.windowClosing(false)) {
              return;
            }
          } catch (Exception e2) {
            Logger.error(e2);
            return;
          }
          WINDOWS.remove(ChildFrame.this);
        }
        ChildFrame.this.setVisible(false);
      }
    });
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (ChildFrame.this.closeOnInvisible) {
          try {
            if (!ChildFrame.this.windowClosing(false)) {
              return;
            }
          } catch (Exception e2) {
            throw new IllegalAccessError(); // ToDo: This is just too ugly
          }
          WINDOWS.remove(ChildFrame.this);
        }
        ChildFrame.this.setVisible(false);
      }
    });
  }

  public void close() {
    setVisible(false);
    WINDOWS.remove(this);
  }

  /**
   * This method is called whenever the dialog is about to be closed and removed from memory.
   *
   * @param forced If {@code false}, the return value will be honored. If {@code true}, the return value will be
   *               disregarded.
   * @return If {@code true}, the closing procedure continues. If {@code false}, the closing procedure will be
   *         cancelled.
   * @throws Exception
   */
  protected boolean windowClosing(boolean forced) throws Exception {
    if (WINDOWS.size() == 1) {
      // storing size and/or position of last closing window
      if (shouldRememberPlacement(this)) {
        updateLastFrameRect(getSize(), getLocation());
      }
    }
    return true;
  }

  /**
   * Returns the size of the last created child frame. Falls back to the default size if information is not available.
   *
   * @return Size of the last created child frame as {@link Dimension} object.
   */
  protected Dimension getLastFrameSize() {
    final boolean useLast = BrowserMenuBar.getInstance().getOptions().rememberChildFrameRect();

    Dimension retVal = null;
    ChildFrame frame = null;
    for (Iterator<ChildFrame> iter = getFrameIterator(p -> p != this); iter.hasNext(); ) {
      final ChildFrame f = iter.next();
      if (shouldRememberPlacement(f)) {
        frame = f;
      }
    }

    if (useLast && frame != null) {
      retVal = frame.getSize();
    } else if (useLast && lastFrameSize != null) {
      retVal = lastFrameSize;
    } else {
      retVal = new Dimension(NearInfinity.getInstance().getWidth() - 200, NearInfinity.getInstance().getHeight() - 45);
    }
    updateLastFrameRect(retVal, null);

    return retVal;
  }

  /**
   * Returns the location of the last created child frame.
   * Falls back to the default location if information is not available.
   *
   * @return Location of the last created child frame as {@link Point} object.
   */
  protected Point getLastFrameLocation(Component parent) {
    final boolean useLast = BrowserMenuBar.getInstance().getOptions().rememberChildFrameRect();
    Point retVal = null;

    ChildFrame frame = null;
    for (Iterator<ChildFrame> iter = getFrameIterator(p -> p != this); iter.hasNext(); ) {
      final ChildFrame f = iter.next();
      if (shouldRememberPlacement(f)) {
        frame = f;
      }
    }

    if (useLast && frame != null) {
      retVal = frame.getLocation();
    } else if (useLast && lastFrameLocation != null) {
      retVal = lastFrameLocation;
    } else {
      retVal = Center.getCenterLocation(getSize(), parent.getBounds());
    }
    updateLastFrameRect(null, retVal);

    return retVal;
  }

  /** Returns whether placement information of the specified window should be remembered. */
  private static boolean shouldRememberPlacement(ChildFrame frame) {
    boolean retVal = false;
    if (frame instanceof ViewableContainer) {
      final Viewable viewable = ((ViewableContainer)frame).getViewable();
      // Auto-resizing Viewables (e.g. MOS or TIS resources) and non-Viewable windows are not considered
      retVal = (viewable instanceof AbstractStruct || viewable instanceof TextResource || viewable instanceof BamResource);
    }
    return retVal;
  }

  /** Stores the specified size and/or location for use with future child frames. */
  private static void updateLastFrameRect(Dimension dim, Point pt) {
    if (dim != null) {
      lastFrameSize = dim;
    }
    if (pt != null) {
      lastFrameLocation = pt;
    }
  }

  /** Closes the specified {@code ChildFrame} instance and releases associated resources. */
  private static void closeWindow(ChildFrame frame, WindowEvent event) {
    if (frame != null) {
      try {
        frame.windowClosing(true);
        frame.setVisible(false);
        if (event == null) {
          event = new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING);
        }
        WindowListener listeners[] = frame.getWindowListeners();
        for (final WindowListener listener : listeners) {
          listener.windowClosing(event);
        }
        if (frame instanceof Closeable) {
          frame.close();
        }
        frame.dispose();
      } catch (Exception e) {
        Logger.error(e);
      }
    }
  }
}
