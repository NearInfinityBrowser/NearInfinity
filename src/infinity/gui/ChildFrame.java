// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.resource.Closeable;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class ChildFrame extends JFrame
{
  private static final List<ChildFrame> windows = new ArrayList<ChildFrame>();
  private final boolean closeOnInvisible;

  public static void closeWindow(Class<ChildFrame> frameClass)
  {
    for (Iterator<ChildFrame> i = windows.iterator(); i.hasNext();) {
      ChildFrame frame = i.next();
      if (frame.getClass() == frameClass) {
        i.remove();
        try {
          frame.windowClosing();
          frame.setVisible(false);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static void closeWindows()
  {
    WindowEvent event = new WindowEvent(NearInfinity.getInstance(), WindowEvent.WINDOW_CLOSING);
    List<ChildFrame> copy = new ArrayList<ChildFrame>(windows);
    windows.clear();
    for (int i = 0; i < copy.size(); i++) {
      ChildFrame frame = copy.get(i);
      try {
        frame.windowClosing();
        frame.setVisible(false);
        WindowListener listeners[] = frame.getWindowListeners();
        for (final WindowListener listener : listeners)
          listener.windowClosing(event);
        frame.dispose();
        if (frame instanceof Closeable)
          frame.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static ChildFrame getFirstFrame(Class<? extends ChildFrame> frameClass)
  {
    for (int i = 0; i < windows.size(); i++) {
      ChildFrame frame = windows.get(i);
      if (frame.getClass() == frameClass)
        return frame;
    }
    return null;
  }

  public static List<ChildFrame> getFrames(Class<? extends ChildFrame> frameClass)
  {
    List<ChildFrame> frames = new ArrayList<ChildFrame>();
    for (int i = 0; i < windows.size(); i++) {
      ChildFrame frame = windows.get(i);
      if (frame.getClass() == frameClass)
        frames.add(frame);
    }
    return frames;
  }

  public static void updateWindowGUIs()
  {
    for (int i = 0; i < windows.size(); i++)
      SwingUtilities.updateComponentTreeUI(windows.get(i));
  }

  protected ChildFrame(String title)
  {
    this(title, false);
  }

  public ChildFrame(String title, boolean closeOnInvisible)
  {
    super(title);
    this.closeOnInvisible = closeOnInvisible;
    windows.add(this);
    JPanel pane = new JPanel();
    setContentPane(pane);
    final ChildFrame frame = this;
    pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                                                            pane);
    pane.getActionMap().put(pane, new AbstractAction()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        if (frame.closeOnInvisible) {
          try {
            frame.windowClosing();
          } catch (Exception e2) {
            e2.printStackTrace();
            return;
          }
          windows.remove(frame);
        }
        frame.setVisible(false);
      }
    });
    addWindowListener(new WindowAdapter()
    {
      @Override
      public void windowClosing(WindowEvent e)
      {
        try {
          frame.windowClosing();
        } catch (Exception e2) {
          throw new IllegalAccessError(); // ToDo: This is just too ugly
        }
        if (frame.closeOnInvisible)
          windows.remove(frame);
      }
    }
    );
  }

  public void close()
  {
    setVisible(false);
    windows.remove(this);
  }

  protected void windowClosing() throws Exception
  {
  }
}

