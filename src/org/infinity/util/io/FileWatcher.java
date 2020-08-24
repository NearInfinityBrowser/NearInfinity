// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Continuously monitors one or more registered paths for content changes and notifies registered
 * listeners about the changes.
 * TODO: watched directories are locked on Windows and can't be properly deleted - find workaround
 */
public class FileWatcher implements Runnable
{
  private static final long DEFAULT_TIMEOUT = 250;

  private static FileWatcher instance = null;

  private final HashMap<WatchKey, Path> pathMap = new HashMap<>();
  private final ArrayList<FileWatchListener> listeners = new ArrayList<>();

  private WatchService watcher;
  private Thread thread;
  private long timeOutMS;

  /** Returns the active FileWatcher instance. */
  public static FileWatcher getInstance()
  {
    if (instance == null) {
      instance = new FileWatcher(DEFAULT_TIMEOUT);
    }
    return instance;
  }

  @SuppressWarnings("unchecked")
  private static <T> WatchEvent<T> cast(WatchEvent<?> event)
  {
    return (WatchEvent<T>)event;
  }


  protected FileWatcher(long timeOutMS)
  {
    try {
      this.watcher = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      this.watcher = null;
    }
    this.thread = null;
    this.timeOutMS = Math.max(timeOutMS, 0L);
  }

  /**
   * Adds the specified filewatch listener to receive notifications about file changes in registered
   * directories.
   * @param l the filewatch listener
   */
  public void addFileWatchListener(FileWatchListener l)
  {
    if (l != null && !listeners.contains(l)) {
      listeners.add(l);
    }
  }

  /**
   * Removes the specified filewatch listener so that it no longer receives events from this
   * FileWatcher instance.
   * @param l the filewatch listener
   */
  public void removeFileWatchListener(FileWatchListener l)
  {
    if (l != null) {
      listeners.remove(l);
    }
  }

  /**
   * Returns an array of all filewatch listeners registered to this FileWatcher instance.
   * @return all of this {@code FileWatcher}'s {@code FileWatchListener}s.
   */
  public FileWatchListener[] getFileWatchListeners()
  {
    return listeners.toArray(new FileWatchListener[listeners.size()]);
  }

  /** Starts the file watcher background process. Does nothing if the process has already started. */
  public boolean start()
  {
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
      return true;
    } else {
      return false;
    }
  }

  /** Terminates the file watcher process. Does nothing if the process is not running. */
  public boolean stop()
  {
    if (thread != null) {
      Thread t = thread;
      thread = null;
      try { t.join(timeOutMS * 10); } catch (InterruptedException e) {}
      return true;
    } else {
      return false;
    }
  }

  /** Returns whether the file watcher background process is running. */
  public boolean isRunning()
  {
    return (thread != null);
  }

  /** Returns the interval between filesystem checks (in milliseconds). */
  public long getCheckInterval()
  {
    return timeOutMS;
  }

  /** Sets the interval between filesystem checks (in milliseconds). */
  public void setCheckInterval(long timeMS)
  {
    this.timeOutMS = Math.max(timeMS, 0L);
  }

  /** Removes all registered directories at once. */
  public void reset()
  {
    synchronized (pathMap) {
      for (final WatchKey key: pathMap.keySet()) {
        key.cancel();
      }
      pathMap.clear();
    }
  }

  /**
   * Adds the specified directory path and optional subdirectories to the watcher list with
   * enabled notifications for create and delete operations.
   * Does nothing if the path has already been registered.
   */
  public void register(Path dir, boolean recursive)
  {
    register(dir, recursive, true, true, false);
  }

  /**
   * Adds the specified directory path and optional subdirectories to the watcher list for a selected
   * set of notification types. Does nothing if the path has already been registered.
   * @param dir The directory path to add.
   * @param recursive Whether to add subdirectories of "dir" as well.
   * @param notifyCreate Whether to notify if a file is created in the directory.
   * @param notifyDelete Whether to notify if a file is deleted in the directory.
   * @param notifyModify Whether to notify if a file has been modified in the directory.
   */
  public void register(Path dir, boolean recursive, boolean notifyCreate, boolean notifyDelete, boolean notifyModify)
  {
    dir = FileManager.resolve(dir);
    if (dir != null && FileEx.create(dir).isDirectory()) {
      if (recursive) {
        try {
          Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
              register(dir, false, notifyCreate, notifyDelete, notifyModify);
              return FileVisitResult.CONTINUE;
            }
          });
        } catch (IOException e) {
        }
      } else {
        WatchKey key = getWatchKey(dir);
        if (key == null) {
          synchronized (pathMap) {
            try {
              ArrayList<Kind<Path>> list = new ArrayList<>();
              if (notifyCreate) { list.add(StandardWatchEventKinds.ENTRY_CREATE); }
              if (notifyDelete) { list.add(StandardWatchEventKinds.ENTRY_DELETE); }
              if (notifyModify) { list.add(StandardWatchEventKinds.ENTRY_MODIFY); }
              Kind<?>[] kinds = list.toArray(new Kind<?>[list.size()]);
              key = dir.register(watcher, kinds);
              pathMap.put(key, dir);
            } catch (UnsupportedOperationException uoe) {
              // no feedback necessary
            } catch (IOException ioe) {
              ioe.printStackTrace();
            }
          }
        }
      }
    }
  }

  /**
   * Removes the specified directory path and optional subdirectories from the watcher list.
   * Does nothing if the specified directory path is not registered.
   */
  public void unregister(Path dir, boolean recursive)
  {
    dir = FileManager.resolve(dir);
    if (dir != null) {
      if (recursive) {
        synchronized (pathMap) {
          Iterator<WatchKey> iter = pathMap.keySet().iterator();
          while (iter.hasNext()) {
            WatchKey key = iter.next();
            Path value = pathMap.get(key);
            if (value.startsWith(dir)) {
              key.cancel();
              iter.remove();
            }
          }
        }
      } else {
        WatchKey key = getWatchKey(dir);
        if (key != null) {
          synchronized (pathMap) {
            key.cancel();
            pathMap.remove(key);
          }
        }
      }
    }
  }

  /** Returns whether the specified directory path is registered in the watcher list. */
  public boolean isRegistered(Path dir)
  {
    return (getWatchKey(dir) != null);
  }

  private WatchKey getWatchKey(Path dir)
  {
    WatchKey retVal = null;
    dir = FileManager.resolve(dir);
    if (dir != null) {
      synchronized (pathMap) {
        for (final WatchKey key: pathMap.keySet()) {
          Path value = pathMap.get(key);
          if (dir.equals(value)) {
            retVal = key;
            break;
          }
        }
      }
    }
    return retVal;
  }

  private void init()
  {
  }

  private void done()
  {
    thread = null;
  }

  private void fireFileWatchEvent(FileWatchEvent event)
  {
    if (event != null) {
      for (FileWatchListener l: listeners) {
        if (l != null) {
          try {
            l.fileChanged(event);
          } catch (Throwable t) {
            t.printStackTrace();
          }
        }
      }
    }
  }

//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    init();
    try {
      while (thread != null) {
        // getting signaled key
        WatchKey key;
        try {
          key = watcher.poll(timeOutMS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          break;
        }

        if (key == null) {
          continue;
        }

        Path dir = pathMap.get(key);
        if (dir == null) {
          continue;
        }

        for (WatchEvent<?> event: key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();

          FileWatchEvent fwe = null;
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            // special: does not provide path
//            System.out.println("FileWatch event: " + kind.name());
            fwe = new FileWatchEvent(this, null, kind);
          } else {
            WatchEvent<Path> ev = cast(event);
            Path name = ev.context();
            Path child = dir.resolve(name);
//            System.out.println("FileWatch event: " + kind.name() + " -> " + child);
            fwe = new FileWatchEvent(this, child, kind);
          }
          fireFileWatchEvent(fwe);
        }

        boolean valid = key.reset();
        if (!valid) {
          synchronized (pathMap) {
            pathMap.remove(key);
          }
        }
      }
    } finally {
      done();
    }
  }

//--------------------- End Interface Runnable ---------------------

//-------------------------- INNER CLASSES --------------------------

  /**
   * An event that indicates that a content change in one of the registered directories took place.
   */
  public static class FileWatchEvent extends EventObject
  {
    private final Path path;
    private final WatchEvent.Kind<?> kind;

    public FileWatchEvent(Object source, Path path, WatchEvent.Kind<?> kind)
    {
      super(source);
      this.path = path;
      this.kind = kind;
    }

    /** The full path of the file that has triggered the watcher event. */
    public Path getPath() { return path; }

    /** The watch event type. Can be either of the {@link StandardWatchEventKinds}. */
    public WatchEvent.Kind<?> getKind() { return kind; }
  }


  /** The listener interface for receiving file watch events. */
  public static interface FileWatchListener extends EventListener
  {
    /**
     * Invoked when a file watch event is triggered.
     * @param e The event
     */
    void fileChanged(FileWatchEvent e);
  }
}
