// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;

/**
 * Utility class for performing searching of resources in several threads with
 * ability to cancel search.
 *
 * @author Mingun
 */
public abstract class AbstractSearcher
{
  /** Text for progress note. Contains two int placeholders: current and maximum count of items. */
  private static final String FMT_PROGRESS = "Processing resource %d/%d";

  /**
   * Handle to widget that shows search progress. Creates when {@link #runSearch}
   * invoked and resetted to {@code null} when {@link #advanceProgress advanceProgress(true)}
   * is called.
   */
  private ProgressMonitor progress;
  /** Current number of checked items, that progress shows. */
  private int progressIndex;
  /** GUI component that own search window. */
  protected final Component parent;

  public AbstractSearcher(Component parent) {
    this.parent = parent;
  }

  /**
   * Creates new work item for search specified resource.
   *
   * @param entry Pointer to resource for check. Never {@code null}
   *
   * @return Work item whose {@link Runnable#run run()} method performs search.
   */
  protected abstract Runnable newWorker(ResourceEntry entry);

  /**
   * Runs check that {@link #newWorker spawns} working items that performs actual
   * checking.
   *
   * @param entries Entries for search. Any {@code null} values will be ignored,
   *        any other will be searched in several threads
   *
   * @return {@code true}, if search cancelled, {@code false} otherwise
   */
  protected boolean runSearch(List<ResourceEntry> entries)
  {
    try {
      final int max = entries.size();
      progressIndex = 0;
      progress = new ProgressMonitor(
        parent, "Searching..." + Misc.MSG_EXPAND_LARGE,
        String.format(FMT_PROGRESS, max, max),
        0, max
      );
      progress.setNote(String.format(FMT_PROGRESS, 0, max));
      progress.setMillisToDecideToPopup(100);

      final ThreadPoolExecutor executor = Misc.createThreadPool();
      boolean isCancelled = false;
      Debugging.timerReset();
      for (ResourceEntry entry : entries) {
        if (progress.isCanceled()) {
          break;
        }
        if (entry == null) {
          advanceProgress(false);
          continue;
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

      Debugging.timerShow("Search completed", Debugging.TimeFormat.MILLISECONDS);

      if (isCancelled) {
        JOptionPane.showMessageDialog(parent, "Search cancelled",
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
        if (progressIndex % 100 == 0) {
          progress.setNote(String.format(FMT_PROGRESS, progressIndex, progress.getMaximum()));
        }
        progress.setProgress(progressIndex);
      }
    }
  }
}
