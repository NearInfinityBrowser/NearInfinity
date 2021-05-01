// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import org.infinity.NearInfinity;
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
  public static final String SEARCH_ONE_TYPE_FORMAT   = "Processing resource %2$d/%3$d";
  public static final String SEARCH_MULTI_TYPE_FORMAT = "Processing %1$ss %2$d/%3$d";
  /** Text for progress note. Contains two int placeholders: current and maximum count of items. */
  public static final String CHECK_ONE_TYPE_FORMAT   = "Checking resource %2$d/%3$d";
  public static final String CHECK_MULTI_TYPE_FORMAT = "Checking %1$ss %2$d/%3$d";

  /**
   * Handle to widget that shows search progress. Creates when {@link #runSearch}
   * invoked and resetted to {@code null} when {@link #advanceProgress advanceProgress(true)}
   * is called.
   */
  private ProgressMonitor progress;
  /** Current number of checked items, that progress shows. */
  private int progressIndex;
  /** Extension of the last processed resource. */
  private String lastExt;
  /** Text for progress note. Contains two int placeholders: current and maximum count of items. */
  private final String operationFormat;
  /** GUI component that own search window. */
  protected final Component parent;

  protected AbstractSearcher(String operationFormat, Component parent) {
    this.operationFormat = operationFormat;
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
   * @param operation Brief description of what kind of resources is searches/checked
   *        (dialogs, scripts and so on)
   * @param entries Entries for search. Any {@code null} values will be ignored,
   *        any other will be searched in several threads
   *
   * @return {@code true}, if search cancelled, {@code false} otherwise
   */
  protected boolean runSearch(String operation, List<ResourceEntry> entries)
  {
    if (entries.isEmpty()) {
      return false;
    }
    try {
      final int max = entries.size();
      progressIndex = 0;
      progress = new ProgressMonitor(
        NearInfinity.getInstance(), operation + "..." + Misc.MSG_EXPAND_LARGE,
        String.format(operationFormat, "WWWW", max, max),
        0, max
      );
      progress.setMillisToDecideToPopup(100);
      lastExt = entries.get(0).getExtension();
      updateProgressNote();

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
          if (!lastExt.equalsIgnoreCase(ext)) {
            lastExt = ext;
            updateProgressNote();
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

      Debugging.timerShow(operation + " completed", Debugging.TimeFormat.MILLISECONDS);

      if (isCancelled) {
        JOptionPane.showMessageDialog(parent, operation + " cancelled",
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
          updateProgressNote();
        }
        progress.setProgress(progressIndex);
      }
    }
  }

  private void updateProgressNote() {
    progress.setNote(String.format(operationFormat, lastExt, progressIndex, progress.getMaximum()));
  }
}
