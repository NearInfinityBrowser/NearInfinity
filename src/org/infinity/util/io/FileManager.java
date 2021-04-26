// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.infinity.resource.Profile;
import org.infinity.util.io.FileWatcher.FileWatchEvent;
import org.infinity.util.io.FileWatcher.FileWatchListener;

/**
 * Central hub for accessing game-related I/O resources.
 */
public class FileManager implements FileWatchListener
{
  private static final HashMap<Path, HashSet<Path>> pathCache = new HashMap<>();

  private static FileManager instance;

  // Stores whether filesystems use case-sensitive filenames
  private final HashMap<FileSystem, Boolean> mapCaseSensitive = new HashMap<>();

  public static void reset()
  {
    pathCache.clear();
    if (instance != null) {
      instance.close();
    }
    instance = null;
  }

  /**
   * Returns a {@link Path} object to the file of the specified path based on the
   * given {@code root} path.
   * @param rootPath Limit search to the specified root {@code Path}.
   *                 Specify {@code null} to use the current working directory instead.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} based on {@code root} and the specified path elements.
   */
  public static Path query(Path rootPath, String path, String... more)
  {
    return getInstance()._query(rootPath, path, more);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed
   * root paths.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}.
   *                  Specify {@code null} to use the current working directory instead.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file. Returns a {@code Path} based on the
   *         search path of lowest priority if {@code path} does not exist.
   */
  public static Path query(List<Path> rootPaths, String path, String... more)
  {
    return getInstance()._query(rootPaths, path, more);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed
   * {@code rootPaths} filtered by {@code rootFilter}.
   * @param rootFilter Limit search to {@code rootPaths} which are based on this root {@code Path}.
   *                   Specify {@code null} to ignore {@code rootFilter}.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file. Returns a {@code Path} based on the
   *         search path of lowest priority based on {@code root} if {@code path} does not exist.
   *         Returns a {@code Path} based on the current working path if {@code rootPaths} is empty
   *         after applying {@code rootFilter}.
   */
  public static Path query(Path rootFilter, List<Path> rootPaths, String path, String... more)
  {
    return getInstance()._query(rootFilter, rootPaths, path, more);
  }

  /**
   * Returns a {@link Path} object to the file of the specified path based on the
   * given {@code root} path.
   * @param rootPath Limit search to the specified root {@code Path}. Specify {@code null} to search
   *                 all registered root {@code Path}s.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} based on {@code root} and the specified path elements.
   *         Returns {@code null} if {@code path} does not exist.
   */
  public static Path queryExisting(Path rootPath, String path, String... more)
  {
    return getInstance()._queryExisting(rootPath, path, more);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed
   * root paths.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}.
   *                  Specify {@code null} to search in registered root {@code Path}s instead.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file.
   *         Returns {@code null} if {@code path} does not exist.
   */
  public static Path queryExisting(List<Path> rootPaths, String path, String... more)
  {
    return getInstance()._queryExisting(rootPaths, path, more);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed
   * {@code rootPaths} filtered by {@code rootFilter}.
   * @param rootFilter Limit search to {@code rootPaths} which are based on this root {@code Path}.
   *                   Specify {@code null} to ignore {@code rootFilter}.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}.
   * @param path Relative path to a file or directory.
   * @param more More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file.
   *         Returns {@code null} if {@code path} does not exist.
   */
  public static Path queryExisting(Path rootFilter, List<Path> rootPaths, String path, String... more)
  {
    return getInstance()._queryExisting(rootFilter, rootPaths, path, more);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or
   * case-insensitive filesystems. Non-existing paths are resolved as much as possible.
   * Non-existing path elements are appended to the resolved base path without further
   * modifications.
   * @param path The path to resolve.
   * @param more More optional path elements.
   * @return The resolved path or {@code null} on error.
   */
  public static Path resolve(String path, String... more)
  {
    if (path != null) {
      try {
        return _resolve(FileSystems.getDefault().getPath(path, more));
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Attempts to find a path that matches an existing path on case-sensitive or
   * case-insensitive filesystems.
   * @param path The path to resolve.
   * @param more More optional path elements.
   * @return The resolved path or {@code null} on error or the specified path does not exist.
   */
  public static Path resolveExisting(String path, String... more)
  {
    if (path != null) {
      try {
        return _resolveExisting(FileSystems.getDefault().getPath(path, more));
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or
   * case-insensitive filesystems. Non-existing paths are resolved as much as possible.
   * Non-existing path elements are appended to the resolved base path without further
   * modifications.
   * @param path The path to resolve.
   * @return The resolved path.
   */
  public static Path resolve(Path path)
  {
    return _resolve(path);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or
   * case-insensitive filesystems.
   * @param path The path to resolve.
   * @return The resolved path or {@code null} on error or the specified path does not exist.
   */
  public static Path resolveExisting(Path path)
  {
    return _resolveExisting(path);
  }

  /**
   * Removes the specified directory from the cache.
   * @param dir The directory to remove from the cache.
   */
  public static void invalidateDirectory(Path dir)
  {
    _invalidateDirectory(dir);
  }

  /**
   * Registers the specified file in the file cache.
   * This method should always be called if one or more individual files have been added to a
   * game directory. Does nothing if the parent directory has not been cached yet.
   * @param file The file to register.
   */
  public static void registerFile(Path file)
  {
    _registerFile(file);
  }

  /**
   * Removes the specified file from the file cache.
   * This method should always be called if one or more individual files have been removed from a
   * game directory. Does nothing if the parent directory has not been cached yet.
   * @param file The file to unregister.
   */
  public static void unregisterFile(Path file)
  {
    _unregisterFile(file);
  }

  /**
   * Returns whether the file system the specified {@code path} is pointing to
   * is restricted to read-only operations.
   * @param path The path to test.
   * @return {@code true} if the file system is restricted to read-only operations,
   *         {@code false} otherwise.
   */
  public static boolean isReadOnly(Path path)
  {
    if (path != null) {
      FileSystem fs = path.getFileSystem();
      if (fs != null && fs.isOpen()) {
        return fs.isReadOnly();
      }
    }
    return true;
  }

  /**
   * Returns whether the specified path points to a location on the default filesystem.
   * @param path The patch to check.
   * @return {@code true} if the path points to an existing or non-existing location on the
   *         default filesystem. Returns {@code false} otherwise.
   */
  public static boolean isDefaultFileSystem(Path path)
  {
    if (path != null) {
      return (path.getFileSystem().equals(FileSystems.getDefault()));
    }
    return false;
  }

  /**
   * Returns whether a listed path entry points to the same file as {@code path}.
   * @param path The path.
   * @param list List of potential matches.
   * @return {@code true} if a match is found, {@code false} otherwise.
   */
  public static boolean isSamePath(Path path, List<Path> list)
  {
    if (path != null && list != null) {
      for (final Path p: list) {
        try {
          if (Files.isSameFile(path, p)) {
            return true;
          }
        } catch (IOException e) {
        }
      }
    }
    return false;
  }

  /**
   * Returns whether a listed path entry is parent of or equal to {@code path}.
   * @param path The path.
   * @param list List of potential parent paths.
   * @return {@code true} if a match is found, {@code false} otherwise.
   */
  public static boolean containsPath(Path path, List<Path> list)
  {
    return (getContainedPath(path, list) != null);
  }

  /**
   * Returns the first {@link Path} instance which is parent or the same path as {@code path}.
   * @param path The path.
   * @param list List of potential parent paths.
   * @return A {@link Path} object if a match is found, {@code null} otherwise.
   */
  public static Path getContainedPath(Path path, List<Path> list)
  {
    Path retVal = null;
    if (path != null && list != null) {
      for (final Path p: list) {
        if (path.startsWith(p)) {
          retVal = p;
          break;
        }
      }
    }
    return retVal;
  }

  /**
   * Returns the file extension of the specified path.
   * @param path File or folder path.
   * @return the empty or non-empty file extension, or {@code null} on error.
   */
  public static String getFileExtension(Path path)
  {
    String retVal = null;
    if (path != null) {
      String leaf = path.getFileName().toString();
      int p = leaf.lastIndexOf('.');
      if (p >= 0) {
        retVal = leaf.substring(p+1);
      } else {
        retVal = "";
      }
    }
    return retVal;
  }


  private FileManager()
  {
    FileWatcher.getInstance().addFileWatchListener(this);
  }

  private Path _query(Path rootPath, String path, String... more)
  {
    List<Path> rootPaths = null;
    if (rootPath != null) {
      rootPaths = new ArrayList<>();
      rootPaths.add(rootPath);
    }
    return _queryPath(false, (Path)null, rootPaths, path, more);
  }

  private Path _query(List<Path> rootPaths, String path, String... more)
  {
    return _queryPath(false, (Path)null, rootPaths, path, more);
  }

  private Path _query(Path rootFilter, List<Path> rootPaths, String path, String... more)
  {
    return _queryPath(false, rootFilter, rootPaths, path, more);
  }

  private Path _queryExisting(Path rootPath, String path, String... more)
  {
    List<Path> rootPaths = null;
    if (rootPath != null) {
      rootPaths = new ArrayList<>();
      rootPaths.add(rootPath);
    }
    return _queryPath(true, (Path)null, rootPaths, path, more);
  }

  private Path _queryExisting(List<Path> rootPaths, String path, String... more)
  {
    return _queryPath(true, (Path)null, rootPaths, path, more);
  }

  private Path _queryExisting(Path rootFilter, List<Path> rootPaths, String path, String... more)
  {
    return _queryPath(true, rootFilter, rootPaths, path, more);
  }

  private Path _queryPath(boolean mustExist, Path rootFilter, List<Path> rootPaths, String path, String... more)
  {
    // path must be defined
    if (path == null) {
      return null;
    }

    if (rootPaths == null) {
      rootPaths = new ArrayList<>();
    }

    // filter search
    if (rootFilter != null) {
      int idx = 0;
      while (idx < rootPaths.size()) {
        Path curPath = rootPaths.get(idx);
        if (curPath.startsWith(rootFilter)) {
          rootPaths.remove(idx);
        } else {
          idx++;
        }
      }
    }

    // use current working path as fallback
    if (rootPaths.isEmpty()) {
      rootPaths.add(FileSystems.getDefault().getPath("").toAbsolutePath().normalize());
    }

    // ensure that path is relative
    if (!path.isEmpty()) {
      if (path.charAt(0) == '/' || path.charAt(0) == '\\') {
        path = path.substring(1);
      }
    }

    Path curPath = null;
    boolean exists = false;
    try {
      for (final Path curRoot: rootPaths) {
        try {
          Path relPath = curRoot.getFileSystem().getPath(path, more).normalize();
          if (mustExist) {
            curPath = _resolveExisting(curRoot.resolve(relPath));
            if (curPath != null) {
              exists = true;
              break;
            }
          } else {
            curPath = _resolve(curRoot.resolve(relPath));
            if (curPath != null && FileEx.create(curPath).exists()) {
              exists = true;
              break;
            }
          }
        } catch (Exception e) {
//          e.printStackTrace();
        }
      }
    } catch (Throwable t) {
      curPath = null;
      t.printStackTrace();
    }

    if (mustExist && !exists) {
      curPath = null;
    }

    return curPath;
  }

  private void close()
  {
    FileWatcher.getInstance().removeFileWatchListener(this);
  }

  @Override
  public void fileChanged(FileWatchEvent e)
  {
    if (e.getKind() == StandardWatchEventKinds.ENTRY_CREATE) {
      if (FileEx.create(e.getPath()).isDirectory()) {
        // load whole directory into cache
        _cacheDirectory(e.getPath(), true);
      } else {
        _registerFile(e.getPath());
      }
    } else if (e.getKind() == StandardWatchEventKinds.ENTRY_DELETE) {
      _unregisterFile(e.getPath());
    }
  }

  private static FileManager getInstance()
  {
    if (instance == null) {
      instance = new FileManager();
    }
    return instance;
  }

  // Attempts to find a path which matches an existing path on case-sensitive filesystems.
  // Simply returns "path" on case-insensitive filesystems.
  private static Path _resolve(Path path)
  {
    Path retVal = path;
    if (path != null && isFileSystemCaseSensitive(path.getFileSystem()) && !FileEx.create(path).exists()) {
      boolean found = false;
      Path curPath = path.normalize().toAbsolutePath();
      Path dir = curPath.getRoot();
      for (final Path searchPath: curPath) {
        String searchString = searchPath.getFileName().toString();
        found = false;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
          for (final Path dirPath: ds) {
            String dirString = dirPath.getFileName().toString();
            if (searchString.equalsIgnoreCase(dirString)) {
              dir = dir.resolve(dirString);
              found = true;
              break;
            }
          }
        } catch (Throwable t) {
        }
        if (!found) {
          break;
        }
      }

      if (found) {
        // use detected path
        retVal = dir;
      } else if (dir.getNameCount() < curPath.getNameCount()) {
        // resolve partial path (needed if filename does not exist in path)
        retVal = dir.resolve(curPath.subpath(dir.getNameCount(), curPath.getNameCount()));
      }
    }

    return retVal;
  }

  private static void _registerFile(Path file)
  {
    if (file != null) {
      file = _resolve(file);
      HashSet<Path> set = pathCache.get(file.getParent());
      if (set != null) {
        set.add(file);
      }
    }
  }

  private static void _unregisterFile(Path file)
  {
    if (file != null) {
      file = _resolve(file);
      HashSet<Path> set = pathCache.get(file.getParent());
      if (set != null) {
        set.remove(file);
        if (set.isEmpty()) {
          pathCache.remove(file.getParent());
        }
      } else if (pathCache.containsKey(file)) {
        pathCache.remove(file);
      }
    }
  }

  private static void _invalidateDirectory(Path dir)
  {
    if (dir != null && pathCache.containsKey(dir)) {
      pathCache.remove(dir);
    }
  }

  private static Path _resolveExisting(Path path)
  {
    Path retVal = _resolve(path);
    if (retVal != null) {
      Path folder = retVal.getParent();
      HashSet<Path> list = pathCache.get(folder);
      if (list == null) {
        list = _cacheDirectory(folder, false);
      }
      if (list == null) {
        retVal = null;
      } else {
        final String pathString = path.getFileName().toString();
        final boolean isCase = isFileSystemCaseSensitive(path.getFileSystem());
        boolean match = list
            .parallelStream()
            .anyMatch(p -> isCase ? pathString.equals(p.getFileName().toString()) : pathString.equalsIgnoreCase(p.getFileName().toString()));
        if (!match) {
          retVal = null;
        }
      }
    }
//    if (retVal != null && !FileEx.fromPath(retVal).exists()) {
//      retVal = null;
//    }
    return retVal;
  }

  private static HashSet<Path> _cacheDirectory(Path path, boolean force)
  {
    HashSet<Path> retVal = null;
    if (path != null && FileEx.create(path).isDirectory()) {
      if (force) {
        pathCache.remove(path);
      }
      retVal = pathCache.get(path);
      if (retVal == null) {
        HashSet<Path> fileList = new HashSet<>();
        try (Stream<Path> pathStream = Files.list(path)) {
          pathStream.forEach((file) -> { fileList.add(file); });
        } catch (IOException e) {
        }
        retVal = fileList;
        pathCache.put(path, retVal);
      }
    }
    return retVal;
  }

  // Returns whether the specified filesystem is case-sensitive
  private static boolean isFileSystemCaseSensitive(FileSystem fs)
  {
    Boolean retVal = Boolean.TRUE;
    if (fs != null) {
      retVal = getInstance().mapCaseSensitive.get(fs);
      if (retVal == null) {
        if (Profile.<Boolean>getProperty(Profile.Key.GET_GLOBAL_FILE_CASE_CHECK)) {
          final char[] separators = { '/', '\\', ':' };
          final String name = "/tmp/aaaBBB";
          for (final char sep: separators) {
            String s = (sep != '/') ? name.replace('/', sep) : name;
            try {
              Path path = fs.getPath(s);
              Path path2 = path.getParent().resolve(path.getFileName().toString().toUpperCase(Locale.ENGLISH));
              Path path3 = path.getParent().resolve(path.getFileName().toString().toLowerCase(Locale.ENGLISH));
              retVal = Boolean.valueOf(!(path.equals(path2) && path.equals(path3)));
              getInstance().mapCaseSensitive.put(fs, retVal);
              break;
            } catch (Throwable t) {
              retVal = Boolean.TRUE;
            }
          }
        } else {
          // forced
          retVal = Boolean.valueOf(false);
          getInstance().mapCaseSensitive.put(fs, retVal);
        }
      }
    }
    return retVal.booleanValue();
  }
}
