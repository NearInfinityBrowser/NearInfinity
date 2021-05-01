// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
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
   * @throws IOException on error.
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

  private FileSystem _register(Path dlcFile)
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
    if (dlcFile != null && FileEx.create(dlcFile).isFile()) {
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
          if (key != null && FileEx.create(key).isFile()) {
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

  private FileSystem _validateDlc(Path dlcFile)
  {
    if (dlcFile == null || !FileEx.create(dlcFile).isFile()) {
      return null;
    }

    // checking first 3 LOC entries for compatibility
    try (SeekableByteChannel ch = Files.newByteChannel(dlcFile)) {
      ByteBuffer buffer = StreamUtils.getByteBuffer(30);
      for (int i = 0; i < 3; i++) {
        buffer.compact().position(0);
        if (ch.read(buffer) != buffer.limit()) {
          return null;
        }
        buffer.flip();
        if (buffer.getInt(0) != 0x04034b50) {   // signature
          return null;
        }
        if ((buffer.getShort(4) & 0xffff) > 20) { // version
          return null;
        }
        if ((buffer.getShort(8) & 0xffff) != 0) { // compression
          return null;
        }
        if (buffer.getInt(18) == -1 || buffer.getInt(22) == -1) { // contains zip64 header?
          return null;
        }
        long skip = (long)buffer.getInt(18) & 0xffffffffL;
        skip += (buffer.getShort(26) & 0xffff);
        skip += (buffer.getShort(28) & 0xffff);
        ch.position(ch.position() + skip);
      }
    } catch (Throwable t) {
      return null;
    }

    FileSystemProvider provider = new DlcFileSystemProvider();
    FileSystem fs = null;
    try {
      fs = provider.newFileSystem(dlcFile, null);
      Path key = _queryKey(fs.getPath("/"));
      if (key != null) {
        return fs;
      }
    } catch (Throwable t) {
      if (fs != null) {
        try {
          fs.close();
        } catch (Throwable t2) {
        }
        fs = null;
      }
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
