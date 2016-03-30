// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Central hub for accessing game-related I/O resources.
 */
public class FileManager
{
  // Stores whether filesystems use case-sensitive filenames
  private static final HashMap<FileSystem, Boolean> mapCaseSensitive = new HashMap<>();

  private static FileManager instance;

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
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@path}.
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
   * {@code rootPaths} filtered by {@rootFilter}.
   * @param rootFilter Limit search to {@code rootPaths} which are based on this root {@code Path}.
   *                   Specify {@code null} to ignore {@code rootFilter}.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@path}.
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
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@path}.
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
   * {@code rootPaths} filtered by {@rootFilter}.
   * @param rootFilter Limit search to {@code rootPaths} which are based on this root {@code Path}.
   *                   Specify {@code null} to ignore {@code rootFilter}.
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@path}.
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
        return _resolve(false, FileSystems.getDefault().getPath(path, more));
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
        return _resolve(true, FileSystems.getDefault().getPath(path, more));
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
    return _resolve(false, path);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or
   * case-insensitive filesystems.
   * @param path The path to resolve.
   * @return The resolved path or {@code null} on error or the specified path does not exist.
   */
  public static Path resolveExisting(Path path)
  {
    return _resolve(true, path);
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


  private FileManager() {}

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
    try {
      for (final Path curRoot: rootPaths) {
        Path relPath = curRoot.getFileSystem().getPath(path, more).normalize();
        curPath = _resolve(mustExist, curRoot.resolve(relPath));
        if (curPath != null && Files.exists(curPath)) {
          break;
        }
      }
    } catch (Throwable t) {
      curPath = null;
      t.printStackTrace();
    }

    if (mustExist && curPath != null && !Files.exists(curPath)) {
      curPath = null;
    }

    return curPath;
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
  private static Path _resolve(boolean mustExist, Path path)
  {
    Path retVal = path;
    if (path != null && isFileSystemCaseSensitive(path.getFileSystem())) {
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

    try {
      if (mustExist && retVal != null && !Files.exists(retVal)) {
        retVal = null;
      }
    } catch (Throwable t) {
      retVal = null;
    }

    return retVal;
  }

  // Returns whether the specified filesystem is case-sensitive
  private static boolean isFileSystemCaseSensitive(FileSystem fs)
  {
    Boolean retVal = Boolean.TRUE;
    if (fs != null) {
      retVal = mapCaseSensitive.get(fs);
      if (retVal == null) {
        final char[] separators = { '/', '\\', ':' };
        final String name = "/tmp/aaaBBB";
        for (final char sep: separators) {
          String s = (sep != '/') ? name.replace('/', sep) : name;
          try {
            Path path = fs.getPath(s);
            Path path2 = path.getParent().resolve(path.getFileName().toString().toUpperCase(Locale.ENGLISH));
            Path path3 = path.getParent().resolve(path.getFileName().toString().toLowerCase(Locale.ENGLISH));
            retVal = Boolean.valueOf(!(path.equals(path2) && path.equals(path3)));
            mapCaseSensitive.put(fs, retVal);
            break;
          } catch (Throwable t) {
            retVal = Boolean.TRUE;
          }
        }
      }
    }
    return retVal.booleanValue();
  }

//-------------------------- INNER CLASSES --------------------------

}
