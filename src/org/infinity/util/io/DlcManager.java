// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;

import org.infinity.util.io.zip.DlcFileSystem;
import org.infinity.util.io.zip.DlcFileSystemProvider;

/**
 * Manages available DLCs used by the current game.
 */
public class DlcManager
{
  // list of supported KEY files
  private static final String[] KEY_FILES = {"mod.key", "chitin.key"};

  private static DlcManager instance;

  private final HashMap<Path, FileSystem> fileSystems;


  /**
   * Checks whether the specified file is a valid DLC and registers it internally if not yet done.
   * Returns the {@code FileSystem} object created from the file.
   * @param dlcFile The {@link Path} object pointing to a DLC archive.
   * @return The {@link FileSystem} object created from the file or fetched from the cache
   *         if it had been already registered. Returns {@code null} if the file does not point to
   *         a valid DLC archive.
   */
  public static FileSystem register(Path dlcFile) throws IOException
  {
    return getInstance()._register(dlcFile);
  }

  /**
   * Returns the FileSystem object created from the specified file if available.
   * @param dlcFile The file used to create a FileSystem object from.
   * @return A {@link FileSystem} object when available, {@code null} otherwise.
   */
  public static FileSystem getDlc(Path dlcFile)
  {
    return getInstance()._getDlc(dlcFile);
  }

  /**
   * Attempts to find the {@code mod.key} in the specified {@code path}.
   * @param path Either a {@code DlcPath} object pointing to the root of the DLC filesystem or
   *             the path to the DLC archive on the default filesystem.
   * @return {@code DlcPath} object pointing to the {@code mod.key} or {@code null} if not available.
   */
  public static Path queryKey(Path path)
  {
    return getInstance()._queryKey(path);
  }

  /** Closes all available {@link FileSystem} objects and removes them from the cache. */
  public static void close()
  {
    getInstance()._close();
  }

  private DlcManager()
  {
    this.fileSystems = new HashMap<>();
  }

  private FileSystem _register(Path dlcFile) throws IOException
  {
    FileSystem fs = _getDlc(dlcFile);
    if (fs == null) {
      fs = _validateDlc(dlcFile);
      if (fs != null) {
        fileSystems.put(dlcFile, fs);
      }
    }
    return fs;
  }

  private FileSystem _getDlc(Path dlcFile)
  {
    if (dlcFile != null && Files.isRegularFile(dlcFile)) {
      return fileSystems.get(dlcFile);
    }
    return null;
  }

  private Path _queryKey(Path path)
  {
    if (path != null) {
      FileSystem fs = path.getFileSystem();
      if (!(fs instanceof DlcFileSystem)) {
        fs = _getDlc(path);
      }

      if (fs != null) {
        for (final String keyFile: KEY_FILES) {
          Path key = fs.getPath(keyFile);
          if (key != null && Files.isRegularFile(key)) {
            try (InputStream is = StreamUtils.getInputStream(key)) {
              String sig = StreamUtils.readString(is, 8);
              if ("KEY V1  ".equals(sig)) {
                return key;
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    return null;
  }

  private FileSystem _validateDlc(Path dlcFile) throws IOException
  {
    if (dlcFile == null || !Files.isRegularFile(dlcFile)) {
      return null;
    }

    try (InputStream is = StreamUtils.getInputStream(dlcFile)) {
      if (StreamUtils.readInt(is) != 0x04034b50) {  // zip local file header signature
        return null;
      }
    } catch (Throwable t) {
      return null;
    }

    FileSystemProvider provider = new DlcFileSystemProvider();
    FileSystem fs = null;
    fs = provider.newFileSystem(dlcFile, null);
    Path key = _queryKey(fs.getPath("/"));
    if (key != null) {
      return fs;
    }

    if (fs != null) {
      try {
        fs.close();
      } catch (Throwable t) {
      }
      fs = null;
    }

    return null;
  }

  private void _close()
  {
    for (final FileSystem fs: fileSystems.values()) {
      try {
        fs.close();
      } catch (IOException e) {
      }
    }
    fileSystems.clear();
  }

  private static DlcManager getInstance()
  {
    if (instance == null) {
      instance = new DlcManager();
    }
    return instance;
  }
}
