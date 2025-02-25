// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Attempts to delete all registered files when the JVM terminates.
 */
public class FileDeletionHook extends Thread {
  private static FileDeletionHook instance;

  private final Set<Path> listFilesToDelete;

  /** Returns the only allowed instance of this class. */
  public static FileDeletionHook getInstance() {
    if (instance == null) {
      instance = new FileDeletionHook();
    }
    return instance;
  }

  @Override
  public void run() {
    synchronized (listFilesToDelete) {
      final Consumer<Path> op = path -> {
        if (path != null) {
          try {
            Files.deleteIfExists(path);
          } catch (Throwable t) {
            Logger.trace(t);
          }
        }
      };
      processReversed(listFilesToDelete.iterator(), op);
    }
  }

  /** Registers a file or (empty) directory for deletion. */
  public synchronized void registerFile(Path file) {
    if (file != null) {
      listFilesToDelete.add(file);
    }
  }

  /** Removes the specified file or directory from the list for deletion. */
  public synchronized boolean unregisterFile(Path file) {
    if (file != null) {
      return listFilesToDelete.remove(file);
    }
    return false;
  }

  /** Returns whether the specified file or directory has been registered for deletion. */
  public synchronized boolean isFileRegistered(Path file) {
    if (file != null) {
      return listFilesToDelete.contains(file);
    }
    return false;
  }

  private FileDeletionHook() {
    this.listFilesToDelete = new LinkedHashSet<>();
  }

  /**
   * Performs the specified operation on all elements referenced by the specified iterator in reversed order.
   *
   * @param iter {@link Iterator} over {@link Path} elements.
   * @param op   {@link Consumer} object to process for each path element.
   */
  private void processReversed(Iterator<Path> iter, Consumer<Path> op) {
    if (iter != null && iter.hasNext()) {
      final Path path = iter.next();
      processReversed(iter, op);
      if (op != null) {
        op.accept(path);
      }
    }
  }
}
