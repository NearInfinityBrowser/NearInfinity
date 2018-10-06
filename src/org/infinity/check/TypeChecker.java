// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.infinity.NearInfinity;
import org.infinity.gui.ChildFrame;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;

/**
 * Utility class for performing checking of resources of the different types in
 * several threads with ability to cancel check.
 *
 * @author Mingun
 */
public abstract class TypeChecker extends ChildFrame
{
  /** Text for progress note. Contains string placeholder for current type. */
  private static final String FMT_PROGRESS = "Checking %ss...";

  /**
   * Handle to widget that shows check progress. Creates when {@link #runCheck}
   * invoked and resetted to {@code null} when {@link #advanceProgress advanceProgress(true)}
   * is called.
   */
  private ProgressMonitor progress;
  /** Current number of checked items, that progress shows. */
  private int progressIndex;

  /**
   * Constructor.
   *
   * @param title Title for check window
   */
  protected TypeChecker(String title) {
    super(title);
  }
  /**
   * Creates new work item for check specified resource.
   *
   * @param entry Pointer to resource for check. Never {@code null}
   *
   * @return Work item whose {@link Runnable#run run()} method performs check.
   */
  protected abstract Runnable newWorker(ResourceEntry entry);
  /**
   * Runs check that {@link #newWorker spawns} working items that performs actual
   * checking.
   *
   * @param entries Entries for check. Any {@code null} values will be ignored,
   *        any other will be checked in several threads
   *
   * @return {@code true}, if check cancelled, {@code false} otherwise
   */
  protected boolean runCheck(List<ResourceEntry> entries)
  {
    try {
      String type = "WWWW";
      final int max = entries.size();
      progressIndex = 0;
      progress = new ProgressMonitor(
        NearInfinity.getInstance(), "Checking..." + Misc.MSG_EXPAND_SMALL,
        String.format(FMT_PROGRESS, type),
        0, max
      );
      progress.setMillisToDecideToPopup(100);

      final ThreadPoolExecutor executor = Misc.createThreadPool();
      boolean isCancelled = false;
      Debugging.timerReset();
      int i = 0;
      for (ResourceEntry entry : entries) {
        if (progress.isCanceled()) {
          break;
        }
        if (entry == null) {
          ++i;
          advanceProgress(false);
          continue;
        }
        if (i++ % 10 == 0) {
          final String ext = entry.getExtension();
          if (!type.equalsIgnoreCase(ext)) {
            type = ext;
            progress.setNote(String.format(FMT_PROGRESS, type));
          }
        }

        Misc.isQueueReady(executor, true, -1);
        executor.execute(newWorker(entry));
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);

      if (isCancelled) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Check cancelled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      }
      return isCancelled;
    } finally {
      advanceProgress(true);
    }
  }
  /** Move progress along. */
  protected final void advanceProgress() { advanceProgress(false); }
  /**
   * Move progress along.
   *
   * @param finished If {@code true}, progress dialog is closed, otherwise progress advanced
   */
  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        progress.setProgress(progressIndex);
      }
    }
  }
}
