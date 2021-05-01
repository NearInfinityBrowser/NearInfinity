// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

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
import org.infinity.resource.Closeable;

public class ChildFrame extends JFrame
{
  private static final List<ChildFrame> windows = new ArrayList<>();
  private final boolean closeOnInvisible;

  public static void closeWindow(Class<ChildFrame> frameClass)
  {
    WindowEvent event = new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING);
    for (Iterator<ChildFrame> i = windows.iterator(); i.hasNext();) {
      ChildFrame frame = i.next();
      if (frame.getClass() == frameClass) {
        i.remove();
        try {
          frame.windowClosing(true);
          frame.setVisible(false);
          WindowListener listeners[] = frame.getWindowListeners();
          for (final WindowListener listener : listeners) {
            listener.windowClosing(event);
          }
          if (frame instanceof Closeable) {
            frame.close();
          }
          frame.dispose();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void closeWindows()
  {
    WindowEvent event = new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING);
    final List<ChildFrame> copy = new ArrayList<>(windows);
    windows.clear();
    for (final ChildFrame frame : copy) {
      try {
        frame.windowClosing(true);
        frame.setVisible(false);
        WindowListener listeners[] = frame.getWindowListeners();
        for (final WindowListener listener : listeners) {
          listener.windowClosing(event);
        }
        if (frame instanceof Closeable) {
          frame.close();
        }
        frame.dispose();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Returns first window with specified class or {@code null} if such window do not exist.
   *
   * @param frameClass Runtime class of window to find
   * @param <T> Class of window to find
   *
   * @return Finded window or {@code null}
   */
  public static <T extends ChildFrame> T getFirstFrame(Class<T> frameClass)
  {
    for (final ChildFrame frame : windows) {
      if (frame.getClass() == frameClass) {
        return frameClass.cast(frame);
      }
    }
    return null;
  }

  /**
   * Returns an iterator over the registered {@code ChildFrame} instances that are compatible with the
   * specified class type.
   * @param <T> Class of the filtered instances
   * @param frameClass Runtime class of the windows to filter
   * @return An iterator over matching instances. Returns {@code null} if the parameter is {@code null}.
   */
  public static <T extends ChildFrame> Iterator<T> getFrameIterator(Class<T> frameClass)
  {
    if (frameClass != null) {
      return windows.stream().filter(frameClass::isInstance).map(frameClass::cast).iterator();
    }
    return null;
  }

  /**
   * Returns an iterator over the registered {@code ChildFrame} instances that are matching the given predicate.
   * @param pred Predicate used to filter registered windows
   * @return An iterator over matching {@code ChildFrame} instances
   */
  public static Iterator<ChildFrame> getFrameIterator(Predicate<ChildFrame> pred)
  {
    if (pred != null) {
      return windows.stream().filter(pred).iterator();
    } else {
      return windows.iterator();
    }
  }

  /**
   * Shows first window of specified class. If window do not yet exists, create
   * it with {@code init} function and then shows.
   *
   * @param frameClass Runtime class of window to show
   * @param init Function that will be called, if window with specified class no not exist
   * @param <T> Class of window to show
   *
   * @return Finded or created window
   *
   * @see #setVisible(Class, boolean, Supplier)
   */
  public static <T extends ChildFrame> T show(Class<T> frameClass, Supplier<T> init)
  {
    return setVisible(frameClass, true, init);
  }

  /**
   * Sets visibility to first window with specified class. If such window do not yet
   * exist and it need to be show ({@code isVisible == true}) then it created with
   * {@code init} function. Otherwise nothing is do
   *
   * @param frameClass Runtime class of window to show
   * @param isVisible New visibility of specified window
   * @param init Function that will be called, if window with specified class no not exist
   * @param <T> Class of window to show
   *
   * @return Finded of created window. May be {@code null} if window do not exist
   *         and it must be hidden
   *
   * @see #show(Class, Supplier)
   */
  public static <T extends ChildFrame> T setVisible(Class<T> frameClass, boolean isVisible, Supplier<T> init)
  {
    T frame = getFirstFrame(frameClass);
    if (frame == null && isVisible) {
      frame = init.get();
    }
    if (frame != null) {
      frame.setVisible(isVisible);
    }
    return frame;
  }

  public static void updateWindowGUIs()
  {
    for (final ChildFrame frame : windows) {
      SwingUtilities.updateComponentTreeUI(frame);
    }
  }

  protected ChildFrame(String title)
  {
    this(title, false);
  }

  public ChildFrame(String title, boolean closeOnInvisible)
  {
    super(title);
    setIconImages(NearInfinity.getInstance().getIconImages());
    this.closeOnInvisible = closeOnInvisible;
    windows.add(this);
    JPanel pane = new JPanel();
    setContentPane(pane);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                                            pane);
    pane.getActionMap().put(pane, new AbstractAction()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (ChildFrame.this.closeOnInvisible) {
          try {
            if (!ChildFrame.this.windowClosing(false))
              return;
          } catch (Exception e2) {
            e2.printStackTrace();
            return;
          }
          windows.remove(ChildFrame.this);
        }
        ChildFrame.this.setVisible(false);
      }
    });
    addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent e)
      {
        if (ChildFrame.this.closeOnInvisible) {
          try {
            if (!ChildFrame.this.windowClosing(false))
              return;
          } catch (Exception e2) {
            throw new IllegalAccessError(); // ToDo: This is just too ugly
          }
          windows.remove(ChildFrame.this);
        }
        ChildFrame.this.setVisible(false);
      }
    }
    );
  }

  public void close()
  {
    setVisible(false);
    windows.remove(this);
  }

  /**
   * This method is called whenever the dialog is about to be closed and removed from memory.
   * @param forced If {@code false}, the return value will be honored.
   *               If {@code true}, the return value will be disregarded.
   * @return If {@code true}, the closing procedure continues.
   *         If {@code false}, the closing procedure will be cancelled.
   * @throws Exception
   */
  protected boolean windowClosing(boolean forced) throws Exception
  {
    return true;
  }
}
