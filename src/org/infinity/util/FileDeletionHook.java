// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.infinity.util.io.FileEx;

/**
 * Attempts to delete all registered files when the JVM terminates.
 */
public class FileDeletionHook extends Thread
{
  private static FileDeletionHook instance;

  private final Set<Path> listFilesToDelete;


  /** Returns the only allowed instance of this class. */
  public static FileDeletionHook getInstance()
  {
    if (instance == null) {
      instance = new FileDeletionHook();
    }
    return instance;
  }

  @Override
  public void run()
  {
    synchronized (listFilesToDelete) {
      for (final Path file: listFilesToDelete) {
        if (file != null && FileEx.create(file).exists()) {
          try {
            Files.delete(file);
          } catch (Throwable t) {
          }
        }
      }
    }
  }

  /** Registers a file or (empty) directory for deletion. */
  public void registerFile(Path file)
  {
    if (file != null) {
      listFilesToDelete.add(file);
    }
  }

  /** Removes the specified file or directory from the list for deletion. */
  public boolean unregisterFile(Path file)
  {
    if (file != null) {
      return listFilesToDelete.remove(file);
    }
    return false;
  }

  /** Returns whether the specified file or directory has been registered for deletion. */
  public boolean isFileRegistered(Path file)
  {
    if (file != null) {
      return listFilesToDelete.contains(file);
    }
    return false;
  }

  private FileDeletionHook()
  {
    this.listFilesToDelete = new LinkedHashSet<>();
  }
}
