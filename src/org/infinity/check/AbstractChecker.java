// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import org.infinity.NearInfinity;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;

/**
 * Utility class for performing checking of resources in several threads with
 * ability to cancel check.
 *
 * @author Mingun
 */
public abstract class AbstractChecker
{
  /** Text for progress note. Contains two int placeholders: current and maximum count of items. */
  public static final String ONE_TYPE_FORMAT   = "Checking resource %2$d/%3$d";
  public static final String MULTI_TYPE_FORMAT = "Checking %1$ss %2$d/%3$d";

  /**
   * Handle to widget that shows check progress. Creates when {@link #runCheck}
   * invoked and resetted to {@code null} when {@link #advanceProgress advanceProgress(true)}
   * is called.
   */
  private ProgressMonitor progress;
  /** Current number of checked items, that progress shows. */
  private int progressIndex;
  private String lastExt;
  /** Text for progress note. Contains two int placeholders: current and maximum count of items. */
  private final String operationFormat;

  protected AbstractChecker(String operationFormat) {
    this.operationFormat = operationFormat;
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
   * @param progressTitle Brief description of what kind of resources is checked
   *        (dialogs, scripts and so on)
   * @param entries Entries for check. Any {@code null} values will be ignored,
   *        any other will be checked in several threads
   *
   * @return {@code true}, if check cancelled, {@code false} otherwise
   */
  protected boolean runCheck(String progressTitle, List<ResourceEntry> entries)
  {
    if (entries.isEmpty()) {
      return false;
    }
    try {
      final int max = entries.size();
      progress = new ProgressMonitor(
        NearInfinity.getInstance(), progressTitle + Misc.MSG_EXPAND_LARGE,
        String.format(operationFormat, "WWWW", max, max),
        0, max
      );
      progressIndex = 0;
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

      Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);

      if (isCancelled) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation cancelled",
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
