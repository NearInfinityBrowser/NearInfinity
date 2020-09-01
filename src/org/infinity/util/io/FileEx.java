// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.infinity.util.FileDeletionHook;

/**
 * Expands the {@link File} class by custom filesystem support.
 */
public class FileEx extends File
{
  private static final FileSystem DEFAULT_FS = FileSystems.getDefault();

  private final Path path;

  /**
   * Constructs a FileEx instance from the specified Path argument.
   * @param path The path definition with associated filesystem.
   * @throws  NullPointerException
   *          If {@code path} is {@code null}
   */
  public static FileEx create(Path path)
  {
    return new FileEx(path);
  }

  /**
   * Creates a new {@code FileEx} instance by converting the given
   * pathname string into an abstract pathname using the default filesystem.
   * If the given string is the empty string, then the result is the empty
   * abstract pathname.
   * @param pathname A pathname string
   * @throws  NullPointerException
   *          If the {@code pathname} argument is {@code null}
   */
  public FileEx(String pathname)
  {
    this(pathname, (FileSystem)null);
  }

  /**
   * Creates a new {@code FileEx} instance by converting the given
   * pathname string into an abstract pathname using the specified filesystem.
   * If the given string is the empty string, then the result is the empty
   * abstract pathname.
   * @param pathname A pathname string
   * @param filesystem The associated filesystem. Specify {@code null} to use
   *                   the default filesystem.
   * @throws  NullPointerException
   *          If the {@code pathname} argument is {@code null}
   */
  public FileEx(String pathname, FileSystem filesystem)
  {
    super(pathname);
    path = validatePath(validateFileSystem(filesystem).getPath(pathname));
  }

  /**
   * Creates a new {@code FileEx} instance from a parent pathname string
   * and a child pathname string using the default filesystem.
   * @param   parent  The parent pathname string
   * @param   child   The child pathname string
   * @throws  NullPointerException
   *          If {@code child} is {@code null}
   */
  public FileEx(String parent, String child)
  {
    this(parent, child, null);
  }

  /**
   * Creates a new {@code FileEx} instance from a parent pathname string
   * and a child pathname string with the specified filesystem.
   * @param   parent  The parent pathname string
   * @param   child   The child pathname string
   * @param filesystem The associated filesystem. Specify {@code null} to use
   *                   the default filesystem.
   * @throws  NullPointerException
   *          If {@code child} is {@code null}
   */
  public FileEx(String parent, String child, FileSystem filesystem)
  {
    super(parent, child);
    if (parent == null) {
      path = validatePath(validateFileSystem(filesystem).getPath(child));
    } else {
      path = validatePath(validateFileSystem(filesystem).getPath(parent, child));
    }
  }

  /**
   * Creates a new {@code FileEx} instance from a parent abstract
   * pathname and a child pathname string with the default filesystem.
   * @param   parent  The parent abstract pathname
   * @param   child   The child pathname string
   * @throws  NullPointerException
   *          If {@code child} is {@code null}
   * @throws InvalidPathException If no valid path can be constructed from the
   *         specified arguments.
   */
  public FileEx(File parent, String child)
  {
    this(parent, child, null);
  }

  /**
   * Creates a new {@code FileEx} instance from a parent abstract
   * pathname and a child pathname string with the specified filesystem.
   * @param   parent  The parent abstract pathname
   * @param   child   The child pathname string
   * @param filesystem The associated filesystem. Specify {@code null} to use
   *                   the default filesystem.
   * @throws  NullPointerException
   *          If {@code child} is {@code null}
   * @throws InvalidPathException If no valid path can be constructed from the
   *         specified arguments.
   */
  public FileEx(File parent, String child, FileSystem filesystem)
  {
    super(parent, child);
    if (parent == null) {
      path = validatePath(validateFileSystem(filesystem).getPath(child));
    } else {
      path = validatePath(validateFileSystem(filesystem).getPath(parent.getAbsolutePath(), child));
    }
  }

  /**
   * Constructs a {@code FileEx} instance from the specified Path argument.
   * @param path The path definition with associated filesystem.
   * @throws  NullPointerException
   *          If {@code path} is {@code null}
   */
  public FileEx(Path path)
  {
    super((path != null) ? path.toString() : null);
    this.path = validatePath(path);
  }

  @Override
  public boolean isAbsolute()
  {
    return (path == null) ? super.isAbsolute() : path.isAbsolute();
  }

  @Override
  public String getAbsolutePath()
  {
    return (path == null) ? super.getAbsolutePath() : path.toAbsolutePath().toString();
  }

  @Override
  public File getAbsoluteFile()
  {
    return (path == null) ? super.getAbsoluteFile() : new FileEx(path.toAbsolutePath());
  }

  @Override
  public String getCanonicalPath() throws IOException
  {
    return (path == null) ? super.getCanonicalPath() : path.toAbsolutePath().normalize().toString();
  }

  @Override
  public File getCanonicalFile() throws IOException
  {
    return (path == null) ? super.getCanonicalFile() : new FileEx(path.toAbsolutePath().normalize());
  }

  @Override
  public boolean canRead()
  {
    return (path == null) ? super.canRead() : Files.isReadable(path);
  }

  @Override
  public boolean canWrite()
  {
    return (path == null) ? super.canWrite() : Files.isWritable(path);
  }

  @Override
  public boolean exists()
  {
    return (path == null) ? super.exists() : Files.exists(path, LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public boolean isDirectory()
  {
    return (path == null) ? super.isDirectory() : Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public boolean isFile()
  {
    return (path == null) ? super.isFile() : Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
  }

  @Override
  public boolean isHidden()
  {
    try {
      return (path == null) ? super.isHidden() : Files.isHidden(path);
    } catch (IOException ex) {
      return false;
    }
  }

  @Override
  public long lastModified()
  {
    try {
      return (path == null) ? super.lastModified() : Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
    } catch (IOException ex) {
      return 0L;
    }
  }

  @Override
  public long length()
  {
    try {
      return (path == null) ? super.length() : Files.size(path);
    } catch (IOException ex) {
      return 0L;
    }
  }

  @Override
  public boolean createNewFile() throws IOException
  {
    if (path == null)
      return super.createNewFile();
    else {
      try {
        Files.createFile(path);
        return true;
      } catch (FileAlreadyExistsException | UnsupportedOperationException ex) {
        return false;
      }
    }
  }

  @Override
  public boolean delete()
  {
    if (path == null)
      return super.delete();
    else {
      try {
        Files.delete(path);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  @Override
  public void deleteOnExit()
  {
    if (path == null)
      super.deleteOnExit();
    else
      FileDeletionHook.getInstance().registerFile(path);
  }

  @Override
  public String[] list()
  {
    if (path == null)
      return super.list();
    else {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
          ArrayList<String> list = new ArrayList<>();
          for (Iterator<Path> iter = ds.iterator(); iter.hasNext(); )
            list.add(iter.next().getFileName().toString());
          return list.toArray(new String[list.size()]);
        } catch (Exception ex) {
        }
      }
      return null;
    }
  }

  @Override
  public String[] list(FilenameFilter filter)
  {
    if (path == null)
      return super.list(filter);
    else {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
          File dir = new FileEx(path);
          ArrayList<String> list = new ArrayList<>();
          for (Iterator<Path> iter = ds.iterator(); iter.hasNext(); ) {
            String s = iter.next().getFileName().toString();
            if (filter == null || filter.accept(dir, s))
              list.add(s);
          }
          return list.toArray(new String[list.size()]);
        } catch (Exception ex) {
        }
      }
      return null;
    }
  }

  @Override
  public File[] listFiles()
  {
    if (path == null)
      return super.listFiles();
    else {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
          ArrayList<File> list = new ArrayList<>();
          for (Iterator<Path> iter = ds.iterator(); iter.hasNext(); )
            list.add(new FileEx(iter.next()));
          return list.toArray(new File[list.size()]);
        } catch (Exception ex) {
        }
      }
      return null;
    }
  }

  @Override
  public File[] listFiles(FilenameFilter filter)
  {
    if (path == null)
      return super.listFiles();
    else {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
          File dir = new FileEx(path);
          ArrayList<File> list = new ArrayList<>();
          for (Iterator<Path> iter = ds.iterator(); iter.hasNext(); ) {
            Path p = iter.next();
            if (filter == null || filter.accept(dir, p.getFileName().toString()))
              list.add(new FileEx(p));
          }
          return list.toArray(new File[list.size()]);
        } catch (Exception ex) {
        }
      }
      return null;
    }
  }

  @Override
  public File[] listFiles(FileFilter filter)
  {
    if (path == null)
      return super.listFiles();
    else {
      if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
          ArrayList<File> list = new ArrayList<>();
          for (Iterator<Path> iter = ds.iterator(); iter.hasNext(); ) {
            File f = new FileEx(iter.next());
            if (filter == null || filter.accept(f))
              list.add(f);
          }
          return list.toArray(new File[list.size()]);
        } catch (Exception ex) {
        }
      }
      return null;
    }
  }

  @Override
  public boolean mkdir()
  {
    if (path == null)
      return super.mkdir();
    else {
      try {
        Files.createDirectory(path);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  @Override
  public boolean mkdirs()
  {
    if (path == null)
      return super.mkdirs();
    else {
      try {
        Files.createDirectories(path);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  @Override
  public boolean renameTo(File dest)
  {
    if (path == null)
      return super.renameTo(dest);
    else {
      Path target = path.getFileSystem().getPath(dest.toString());
      try {
        Files.move(path, target);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  @Override
  public boolean setLastModified(long time)
  {
    if (path == null)
      return super.setLastModified(time);
    else {
      if (time < 0L) throw new IllegalArgumentException("Negative time");
      try {
        Files.setLastModifiedTime(path, FileTime.fromMillis(time));
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  @Override
  public boolean setReadOnly()
  {
    if (path == null)
      return super.setReadOnly();
    else {
      try {
        FileStore fs = Files.getFileStore(path);
        if (fs.supportsFileAttributeView(DosFileAttributeView.class)) {
          Files.setAttribute(path, "dos:readonly", true);
        } else if (fs.supportsFileAttributeView(PosixFileAttributeView.class)) {
          PosixFileAttributeView pfav = Files.getFileAttributeView(path, PosixFileAttributeView.class);
          Set<PosixFilePermission> set = pfav.readAttributes().permissions();
          set.remove(PosixFilePermission.OWNER_WRITE);
          set.remove(PosixFilePermission.GROUP_WRITE);
          set.remove(PosixFilePermission.OTHERS_WRITE);
          pfav.setPermissions(set);
          return true;
        } else {
          throw new UnsupportedOperationException();
        }
      } catch (IOException ex) {
      }
      return false;
    }
  }

  @Override
  public boolean setWritable(boolean writable)
  {
    return setWritable(writable, true);
  }

  @Override
  public boolean setWritable(boolean writable, boolean ownerOnly)
  {
    if (path == null)
      return super.setWritable(writable, ownerOnly);
    else {
      try {
        FileStore fs = Files.getFileStore(path);
        if (fs.supportsFileAttributeView(DosFileAttributeView.class)) {
          Files.setAttribute(path, "dos:readonly", false);
        } else if (fs.supportsFileAttributeView(PosixFileAttributeView.class)) {
          PosixFileAttributeView pfav = Files.getFileAttributeView(path, PosixFileAttributeView.class);
          Set<PosixFilePermission> set = pfav.readAttributes().permissions();
          set.add(PosixFilePermission.OWNER_WRITE);
          if (!ownerOnly) {
            set.add(PosixFilePermission.GROUP_WRITE);
            set.add(PosixFilePermission.OTHERS_WRITE);
          }
          pfav.setPermissions(set);
          return true;
        } else {
          throw new UnsupportedOperationException();
        }
      } catch (IOException ex) {
      }
      return false;
    }
  }

  @Override
  public boolean setReadable(boolean readable)
  {
    return setReadable(readable, true);
  }

  @Override
  public boolean setReadable(boolean readable, boolean ownerOnly)
  {
    if (path == null)
      return super.setReadable(readable, ownerOnly);
    else {
      try {
        FileStore fs = Files.getFileStore(path);
        if (fs.supportsFileAttributeView(DosFileAttributeView.class)) {
          return true;  // always true
        } else if (fs.supportsFileAttributeView(PosixFileAttributeView.class)) {
          PosixFileAttributeView pfav = Files.getFileAttributeView(path, PosixFileAttributeView.class);
          Set<PosixFilePermission> set = pfav.readAttributes().permissions();
          set.add(PosixFilePermission.OWNER_READ);
          if (!ownerOnly) {
            set.add(PosixFilePermission.GROUP_READ);
            set.add(PosixFilePermission.OTHERS_READ);
          }
          pfav.setPermissions(set);
          return true;
        } else {
          throw new UnsupportedOperationException();
        }
      } catch (IOException ex) {
      }
      return false;
    }
  }

  @Override
  public boolean setExecutable(boolean executable)
  {
    return setExecutable(executable, true);
  }

  @Override
  public boolean setExecutable(boolean executable, boolean ownerOnly)
  {
    if (path == null)
      return super.setExecutable(executable, ownerOnly);
    else {
      try {
        FileStore fs = Files.getFileStore(path);
        if (fs.supportsFileAttributeView(DosFileAttributeView.class)) {
          return true;  // always true
        } else if (fs.supportsFileAttributeView(PosixFileAttributeView.class)) {
          PosixFileAttributeView pfav = Files.getFileAttributeView(path, PosixFileAttributeView.class);
          Set<PosixFilePermission> set = pfav.readAttributes().permissions();
          set.add(PosixFilePermission.OWNER_EXECUTE);
          if (!ownerOnly) {
            set.add(PosixFilePermission.GROUP_EXECUTE);
            set.add(PosixFilePermission.OTHERS_EXECUTE);
          }
          pfav.setPermissions(set);
          return true;
        } else {
          throw new UnsupportedOperationException();
        }
      } catch (IOException ex) {
      }
      return false;
    }
  }

  @Override
  public boolean canExecute()
  {
    return (path == null) ? super.canExecute() : Files.isExecutable(path);
  }

  @Override
  public int compareTo(File pathname)
  {
    if (pathname == null)
      throw new NullPointerException();

    if (path == null && !(pathname instanceof FileEx)) {
      return super.compareTo(pathname);
    } else {
      return toPath().compareTo(pathname.toPath());
    }
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof File)
      return compareTo((File)obj) == 0;
    return false;
  }

  @Override
  public int hashCode()
  {
    return (path == null) ? super.hashCode() : path.hashCode();
  }

  @Override
  public String toString()
  {
    return (path == null) ? super.toString() : path.toString();
  }

  /**
   * Returns a {@link Path java.nio.file.Path} object constructed from the
   * this abstract path and the associated {@link java.nio.file.FileSystem}.
   * @return a {@code Path} constructed from this abstract path.
   */
  @Override
  public Path toPath()
  {
    return (path == null) ? super.toPath() : path;
  }

  /**
   * Returns the file system associated with this object.
   * @return the file system associated with this object
   */
  public FileSystem getFileSystem()
  {
    return (path == null) ? DEFAULT_FS : path.getFileSystem();
  }

  // Ensures that the returned FileSystem is always non-null.
  private static FileSystem validateFileSystem(FileSystem fs)
  {
    return (fs != null) ? fs : DEFAULT_FS;
  }

  // Returns a well-defined Path instance for internal use.
  private static Path validatePath(Path path)
  {
    if (path != null) {
      if (path.getFileSystem() == null || DEFAULT_FS.equals(path.getFileSystem()))
        path = null;
    }
    return path;
  }
}
