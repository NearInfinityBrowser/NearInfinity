// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


/**
 * Central hub for accessing game-related I/O resources.
 */
public class FileManager {
  private static boolean caseSensitiveMode = getDefaultCaseSensitiveMode();

  /**
   * Returns whether file paths should be polled directly from the filesystem
   * to work with case-sensitive filesystems.
   *
   * <p>By default returns {@code true} only for Linux platforms.</p>
   */
  public static boolean isCaseSensitiveMode() {
    return caseSensitiveMode;
  }

  /**
   * This method can be used to override the default case-sensitive operation mode for filesystems.
   *
   * @param force Specify {@code true} to force polling path names from the filesystem.
   */
  public static void setCaseSensitiveMode(boolean force) {
    caseSensitiveMode = force;
  }

  /** Returns the default case-sensitivity mode for the current platform. */
  public static boolean getDefaultCaseSensitiveMode() {
    final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    return (osName.contains("nix") || osName.contains("nux") || osName.contains("bsd"));
  }

  /**
   * Returns a {@link Path} object to the file of the specified path based on the given {@code root} path.
   *
   * @param rootPath Limit search to the specified root {@code Path}. Specify {@code null} to use the current working
   *                 directory instead.
   * @param path     Relative path to a file or directory.
   * @param more     More optional path elements that are appended to {@code path}.
   * @return The {@code Path} based on {@code root} and the specified path elements.
   */
  public static Path query(Path rootPath, String path, String... more) {
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), path, more, false);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed root paths.
   *
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}. Specify
   *                  {@code null} to use the current working directory instead.
   * @param path      Relative path to a file or directory.
   * @param more      More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file. Returns a {@code Path} based on the search path of lowest
   *         priority if {@code path} does not exist.
   */
  public static Path query(List<Path> rootPaths, String path, String... more) {
    return queryPath(rootPaths, path, more, false);
  }

  /**
   * Returns a {@link Path} object to the file of the specified path based on the given {@code root} path.
   *
   * @param rootPath Limit search to the specified root {@code Path}. Specify {@code null} to search all registered root
   *                 {@code Path}s.
   * @param path     Relative path to a file or directory.
   * @param more     More optional path elements that are appended to {@code path}.
   * @return The {@code Path} based on {@code root} and the specified path elements. Returns {@code null} if
   *         {@code path} does not exist.
   */
  public static Path queryExisting(Path rootPath, String path, String... more) {
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), path, more, true);
  }

  /**
   * Returns a {@link Path} to the first matching file of the specified path in one of the listed root paths.
   *
   * @param rootPaths List of {@code Path} objects which are searched in order to find {@code path}. Specify
   *                  {@code null} to search in registered root {@code Path}s instead.
   * @param path      Relative path to a file or directory.
   * @param more      More optional path elements that are appended to {@code path}.
   * @return The {@code Path} to the first matching file. Returns {@code null} if {@code path} does not exist.
   */
  public static Path queryExisting(List<Path> rootPaths, String path, String... more) {
    return queryPath(rootPaths, path, more, true);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or case-insensitive filesystems.
   * Non-existing paths are resolved as much as possible. Non-existing path elements are appended to the resolved base
   * path without further modifications.
   *
   * @param path The path to resolve.
   * @param more More optional path elements.
   * @return The resolved path or {@code null} on error.
   */
  public static Path resolve(String path, String... more) {
    final Path fullPath = Paths.get(Objects.requireNonNull(path), more);
    final Path rootPath = fullPath.getParent();
    final String fileName = (fullPath.getFileName() != null) ? fullPath.getFileName().toString() : null;
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), new String[] {fileName}, false);
  }

  /**
   * Attempts to find a path that matches an existing path on case-sensitive or case-insensitive filesystems.
   *
   * @param path The path to resolve.
   * @param more More optional path elements.
   * @return The resolved path or {@code null} on error or the specified path does not exist.
   */
  public static Path resolveExisting(String path, String... more) {
    final Path fullPath = Paths.get(Objects.requireNonNull(path), more);
    final Path rootPath = fullPath.getParent();
    final String fileName = (fullPath.getFileName() != null) ? fullPath.getFileName().toString() : null;
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), new String[] {fileName}, true);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or case-insensitive filesystems.
   * Non-existing paths are resolved as much as possible. Non-existing path elements are appended to the resolved base
   * path without further modifications.
   *
   * @param path The path to resolve.
   * @return The resolved path.
   */
  public static Path resolve(Path path) {
    final Path rootPath =  Objects.requireNonNull(path).getParent();
    final String fileName = (path.getFileName() != null) ? path.getFileName().toString() : null;
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), new String[] {fileName}, false);
  }

  /**
   * Attempts to find a path that matches an existing path on both case-sensitive or case-insensitive filesystems.
   *
   * @param path The path to resolve.
   * @return The resolved path or {@code null} on error or the specified path does not exist.
   */
  public static Path resolveExisting(Path path) {
    final Path rootPath =  Objects.requireNonNull(path).getParent();
    final String fileName = (path.getFileName() != null) ? path.getFileName().toString() : null;
    return queryPath(new ArrayList<>(Arrays.asList(rootPath)), new String[] {fileName}, true);
  }

  /**
   * Returns whether the file system the specified {@code path} is pointing to is restricted to read-only operations.
   *
   * @param path The path to test.
   * @return {@code true} if the file system is restricted to read-only operations, {@code false} otherwise.
   */
  public static boolean isReadOnly(Path path) {
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
   *
   * @param path The patch to check.
   * @return {@code true} if the path points to an existing or non-existing location on the default filesystem. Returns
   *         {@code false} otherwise.
   */
  public static boolean isDefaultFileSystem(Path path) {
    if (path != null) {
      return (path.getFileSystem().equals(FileSystems.getDefault()));
    }
    return false;
  }

  /**
   * Returns whether a listed path entry points to the same file as {@code path}.
   *
   * @param path The path.
   * @param list List of potential matches.
   * @return {@code true} if a match is found, {@code false} otherwise.
   */
  public static boolean isSamePath(Path path, List<Path> list) {
    if (path != null && list != null) {
      for (final Path p : list) {
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
   *
   * @param path The path.
   * @param list List of potential parent paths.
   * @return {@code true} if a match is found, {@code false} otherwise.
   */
  public static boolean containsPath(Path path, List<Path> list) {
    return (getContainedPath(path, list) != null);
  }

  /**
   * Returns the first {@link Path} instance which is parent or the same path as {@code path}.
   *
   * @param path The path.
   * @param list List of potential parent paths.
   * @return A {@link Path} object if a match is found, {@code null} otherwise.
   */
  public static Path getContainedPath(Path path, List<Path> list) {
    Path retVal = null;
    if (path != null && list != null) {
      for (final Path p : list) {
        if (path.startsWith(p)) {
          retVal = p;
          break;
        }
      }
    }
    return retVal;
  }

  /**
   * Returns the file extension of the specified path string.
   *
   * @param path File or folder path string.
   * @return the empty or non-empty file extension, or {@code null} on error.
   */
  public static String getFileExtension(String path) {
    String retVal = null;
    if (path != null) {
      int pos = path.lastIndexOf('.');
      if (pos >= 0) {
        retVal = path.substring(pos + 1);
      } else {
        retVal = "";
      }
    }
    return retVal;
  }

  /**
   * Returns the file extension of the specified path.
   *
   * @param path File or folder path.
   * @return the empty or non-empty file extension, or {@code null} on error.
   */
  public static String getFileExtension(Path path) {
    String retVal = null;
    if (path != null) {
      retVal = getFileExtension(path.getFileName().toString());
    }
    return retVal;
  }

  /**
   * Attempts to resolve the path elements to a {@link Path} that matches the given criteria.
   *
   * @param rootPaths List of root {@link Path} instances to test with the specified path elements.
   * @param path      Path element to resolve against any of the root paths.
   * @param more      More optional path elements to resolve against any of the root paths.
   * @param mustExist Specifies whether the resolved path must point to an existing filesystem object.
   * @return a {@link Path} object with the first path the given criteria, {@code null} otherwise.
   */
  private static Path queryPath(List<Path> rootPaths, String path, String[] more, boolean mustExist) {
    final String[] paths = new String[more.length + 1];
    paths[0] = path;
    System.arraycopy(more, 0, paths, 1, more.length);
    return queryPath(rootPaths, paths, mustExist);
  }

  /**
   * Attempts to resolve the path elements to a {@link Path} that matches the given criteria.
   *
   * @param rootPaths List of root {@link Path} instances to test with the specified path elements.
   * @param paths     Optional path elements to resolve against any of the root paths.
   * @param mustExist Specifies whether the resolved path must point to an existing filesystem object.
   * @return a {@link Path} object with the first path the given criteria, {@code null} otherwise.
   */
  private static Path queryPath(List<Path> rootPaths, String[] paths, boolean mustExist) {
    if (rootPaths == null) {
      rootPaths = new ArrayList<>();
    }

    if (rootPaths.stream().noneMatch(Objects::nonNull)) {
      // add current working directory if no root paths are provided
      rootPaths.add(FileSystems.getDefault().getPath(".").toAbsolutePath().normalize());
    }

    if (paths.length == 0) {
      paths = new String[1];
    }

    if (paths[0] == null) {
      paths[0] = ".";
    }

    // ensure that "path" is relative
    if (!paths[0].isEmpty()) {
      if (paths[0].charAt(0) == '/' || paths[0].charAt(0) == '\\') {
        paths[0] = paths[0].substring(1);
      }
    }

    Path curPath = null;
    for (final Path rootPath : rootPaths) {
      if (rootPath != null) {
        try {
          final String path = paths[0];
          final String[] more = Arrays.copyOfRange(paths, 1, paths.length);
          final Path relPath = rootPath.getFileSystem().getPath(path, more).normalize();
          curPath = resolvePath(rootPath.resolve(relPath), mustExist);
          if (curPath != null && (mustExist || FileEx.create(curPath).exists())) {
            break;
          }
        } catch (IllegalArgumentException e) {
//          e.printStackTrace();
        }
      }
    }

    return curPath;
  }

  /**
   * Matches as many of the path elements against existing paths.
   *
   * @param path   Path to match against existing path.
   * @param forced Instructs the resolver to return only existing paths.
   * @return {@link Path} object that matches the given criteria, {@code null} otherwise.
   */
  private static Path resolvePath(Path path, boolean forced) {
    Path retVal = path;

    final FileEx pathEx = FileEx.create(path);
    if (path != null && (isCaseSensitiveMode() || !isDefaultFileSystem(path))) {
      // validating path segments
      Path validatedPath = path.getRoot();
      if (validatedPath != null && FileEx.create(validatedPath).exists()) {
        int idx = 0;
        for (; idx < path.getNameCount(); idx++) {
          final Path pathItem = path.getName(idx);
          final String pathName = pathItem.toString();

          final Path resolvedPath = validatedPath.resolve(pathName);
          if (Files.exists(resolvedPath)) {
            validatedPath = resolvedPath;
          } else {
            try (final DirectoryStream<Path> ds = Files.newDirectoryStream(validatedPath,
                p -> p.getFileName().toString().equalsIgnoreCase(pathName))) {
              final Iterator<Path> iter = ds.iterator();
              if (iter.hasNext()) {
                validatedPath = iter.next();
              } else {
                break;
              }
            } catch (IOException e) {
              break;
            }
          }
        }

        // adding remaining unvalidated path segments (if any)
        if (forced && idx < path.getNameCount()) {
          validatedPath = null;
        } else {
          for (; idx < path.getNameCount(); idx++) {
            validatedPath = validatedPath.resolve(path.getName(idx));
          }
        }

        retVal = validatedPath;
      }
    } else if (forced && !pathEx.exists()) {
      retVal = null;
    }

    if (retVal != null) {
      retVal = retVal.normalize();
    }

    return retVal;
  }
  private FileManager() {
  }
}
