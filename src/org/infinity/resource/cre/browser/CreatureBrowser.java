// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.SpriteUtils;

/**
 * The Creature Browser implements a highly customizable browser and viewer for creature animations.
 */
public class CreatureBrowser extends ChildFrame
{
  private final ConcurrentLinkedQueue<TaskInfo> actionQueue = new ConcurrentLinkedQueue<>();
  private final Listeners listeners = new Listeners();

  private CreatureControlPanel panelCreature;
  private SettingsPanel panelSettings;
  private MediaPanel panelMedia;
  private RenderPanel panelCanvas;
  private CreResource cre;
  private SwingWorker<TaskInfo, Void> worker;

  /**
   * Creates an instance of the creature browser with a (virtual) default creature.
   */
  public CreatureBrowser()
  {
    this(null);
  }

  /**
   * Creates an instance of the creature browser and loads the specified CRE resource.
   * @param cre the CRE resource to load
   */
  public CreatureBrowser(CreResource cre)
  {
    super("");
    init();
    setCreResource(cre);
  }

  /** Returns the currently active CRE resource. */
  public CreResource getCreResource()
  {
    return cre;
  }

  /**
   * Discards the current creature data and loads the specified creature.
   * @param cre the CRE resource to load. Specify {@code null} to load the (virtual) default creature.
   */
  public void setCreResource(CreResource cre)
  {
    if ((this.cre == null && cre != null) ||
        this.cre != null && !this.cre.equals(cre) ||
        cre == null) {
      this.cre = cre;
      performBackgroundTask(this::taskSetCreResource, this::postTaskDefault, true);
    }
  }

  /** Returns the active {@code SpriteDecoder} instance. Returns {@code null} otherwise. */
  public SpriteDecoder getDecoder() { return getControlPanel().getControlModel().getDecoder(); }

  /** Returns the attached {@code CreatureControlPanel} instance. */
  public CreatureControlPanel getControlPanel() { return panelCreature; }

  /** Returns the attached {@code SettingsPanel} instance. */
  public SettingsPanel getSettingsPanel() { return panelSettings; }

  public AttributesPanel getAttributesPanel() { return getSettingsPanel().getAttributesPanel(); }

  /** Returns the attached {@code MediaPanel} instance. */
  public MediaPanel getMediaPanel() { return panelMedia; }

  /** Returns the attached {@code RenderPanel} instance. */
  public RenderPanel getRenderPanel() { return panelCanvas; }

  /** Shows an error dialog with the specified message. */
  public void showErrorMessage(String msg, String title)
  {
    if (msg == null || msg.isEmpty()) {
      msg = "An error has occurred.";
    }
    if (title == null || title.isEmpty()) {
      title = "Error";
    }
    JOptionPane.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE);
  }

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    getMediaPanel().pause();
    cleanup();
    return true;
  }

//--------------------- End Class ChildFrame ---------------------

  private void init()
  {
    setIconImages(NearInfinity.getInstance().getIconImages());

    // *** CRE customization panel ***
    panelCreature = new CreatureControlPanel(this);
    panelCreature.setBorder(new EtchedBorder());

    // *** Settings panel ***
    panelSettings = new SettingsPanel(this);
    panelSettings.setBorder(new EtchedBorder());

    // *** Animation viewer panel ***
    panelCanvas = new RenderPanel(this);

    JPanel viewerPanel = new JPanel(new BorderLayout());
    viewerPanel.setBorder(new EtchedBorder());
    viewerPanel.add(panelCanvas, BorderLayout.CENTER);

    // *** Controls panel ***
    // sub panel for playback controls
    panelMedia = new MediaPanel(this);
    panelMedia.setBorder(new EtchedBorder());

    // *** Top-level viewer panel ***
    JPanel viewerMainPanel = new JPanel(new BorderLayout());
    viewerMainPanel.add(viewerPanel, BorderLayout.CENTER);
    viewerMainPanel.add(panelMedia, BorderLayout.PAGE_END);

    // main panel to hold settings panel and main viewer panel
    JPanel mainCentralPanel = new JPanel(new BorderLayout());
    mainCentralPanel.add(panelSettings, BorderLayout.LINE_END);
    mainCentralPanel.add(viewerMainPanel, BorderLayout.CENTER);

    getContentPane().setLayout(new BorderLayout());
    add(panelCreature, BorderLayout.PAGE_START);
    add(mainCentralPanel, BorderLayout.CENTER);

    addComponentListener(listeners);
    setIconImage(Icons.getImage(Icons.ICON_CRE_VIEWER_24));
    setTitle("Creature Animation Browser");
    setSize(NearInfinity.getInstance().getPreferredSize());
    Center.center(this, NearInfinity.getInstance().getBounds());
    setExtendedState(NearInfinity.getInstance().getExtendedState() & ~ICONIFIED);
    setVisible(true);
  }

  private void cleanup()
  {
    SpriteUtils.clearCache();
  }

  /** Background task: Loads the selected creature and initializes the browser. */
  private Object taskSetCreResource() throws Exception
  {
    ProgressMonitor pm = new ProgressMonitor(this, "Initializing controls...", " ", 0, 2);
    pm.setMillisToDecideToPopup(0);
    pm.setMillisToPopup(0);
    try {
      pm.setProgress(1);
      getControlPanel().getControlModel().setSelectedCreature(getCreResource());
      getControlPanel().getControlModel().resetModified();
      getSettingsPanel().reset();
      getMediaPanel().reset(false);
      pm.setProgress(2);
    } finally {
      pm.close();
    }
    return null;
  }

  /** A generic catch-all operation that can be used to evaluate exceptions thrown in a background task. */
  private void postTaskDefault(Object o, Exception e)
  {
    if (e != null) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * A method that performs a lengthy GUI-interaction task in a background thread.
   * @param action a {@link Task} to execute in the background.
   * @param postAction an optional {@link PostTask} that is executed when the
   *                   background task is completed. Return value of the {@code Task} as well as
   *                   unhandled exceptions thrown in "action" are passed to the {@code PostTask}.
   * @param block whether user interaction in the window is blocked during execution of the background thread.
   */
  public synchronized void performBackgroundTask(Task action, PostTask postAction, boolean block)
  {
    actionQueue.add(new TaskInfo(Objects.requireNonNull(action), postAction, block));
    performBackgroundTask();
  }

  private synchronized void performBackgroundTask()
  {
    final TaskInfo taskInfo = actionQueue.poll();
    if (worker == null && taskInfo != null) {
      worker = new SwingWorker<TaskInfo, Void>() {
        @Override
        protected TaskInfo doInBackground() throws Exception
        {
          TaskInfo retVal = taskInfo;
          WindowBlocker blocker = null;
          try {
            if (retVal.blockWindow) {
              blocker = new WindowBlocker(CreatureBrowser.this);
              blocker.setBlocked(true);
            }
            retVal.result = retVal.action.get();
          } catch (Exception e) {
            if (retVal.postAction != null) {
              retVal.exception = e;
            } else {
              e.printStackTrace();
            }
          } finally {
            if (blocker != null) {
              blocker.setBlocked(false);
              blocker = null;
            }
          }
          return retVal;
        }
      };
      worker.addPropertyChangeListener(listeners);
      worker.execute();
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements PropertyChangeListener, ComponentListener
  {
    public Listeners()
    {
    }

    //--------------------- Begin Interface PropertyChangeListener ---------------------

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
      if (event.getSource() == worker) {
        if ("state".equals(event.getPropertyName()) &&
            SwingWorker.StateValue.DONE == event.getNewValue()) {
          TaskInfo retVal = null;
          try {
            retVal = worker.get();
          } catch (ExecutionException | InterruptedException e) {
          }
          if (retVal != null) {
            if (retVal.postAction != null) {
              retVal.postAction.accept(retVal.result, retVal.exception);
            }
          }
          worker = null;
          performBackgroundTask();
        }
      }
    }

    //--------------------- End Interface PropertyChangeListener ---------------------

    //--------------------- Begin Interface ComponentListener ---------------------

    @Override
    public void componentResized(ComponentEvent e)
    {
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
      getMediaPanel().pause();
    }

    //--------------------- End Interface ComponentListener ---------------------
  }

  /** Represents a supplier of results. */
  public static interface Task
  {
    /** Gets a result. */
    Object get() throws Exception;
  }

  /**
   * Represents an operation that accepts two arguments:
   * {@code Object} returned by and potential {@code Exception} thrown in the previous {@code Task} operation.
   */
  public static interface PostTask
  {
    void accept(Object o, Exception e);

    default PostTask andThen(PostTask after) {
      Objects.requireNonNull(after);

      return (o, e) -> {
          accept(o, e);
          after.accept(o, e);
      };
  }

  }

  /**
   * Helper structure for data related to executing tasks in the background.
   */
  private static class TaskInfo
  {
    /** Task executed in background thread. */
    public final Task action;
    /**
     * Optional task executed when {@code action} thread completed.
     * Parameters: {@code Object} is the result of the {@code action} task,
     *             {@code Exception} is an exception thrown in the {@code action} task.
     */
    public final PostTask postAction;
    /** Indicates whether window should be blocked during background thread execution. */
    public final boolean blockWindow;

    /** The return value of {@code action}. */
    public Object result;
    /** Unhandled exceptions thrown in the background task are forwarded to the post action task. */
    public Exception exception;

    public TaskInfo(Task action, PostTask postAction, boolean block)
    {
      this.action = Objects.requireNonNull(action);
      this.postAction = postAction;
      this.blockWindow = block;
    }
  }
}
