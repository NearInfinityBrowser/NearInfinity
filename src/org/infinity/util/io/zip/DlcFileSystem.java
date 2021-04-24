// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io.zip;

import static org.infinity.util.io.zip.ZipUtils.toRegexPattern;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import org.infinity.util.io.ByteBufferInputStream;

/**
 * FileSystem implementation for DLC archives in zip format inspired by
 * Oracles example code for virtual filesystems.
 *
 * Provides methods for read-only operations on zip archives created with the
 * "store" compression method.
 *
 * Supported filesystem properties:
 * - encoding: Specifies the filename encoding (Default: CP437)
 */
public class DlcFileSystem extends FileSystem
{
  private static final Set<String> supportedFileAttributeViews = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(DlcFileAttributeView.VIEW_BASIC, DlcFileAttributeView.VIEW_ZIP)));

  private static final String GLOB_SYNTAX = "glob";
  private static final String REGEX_SYNTAX = "regex";

  // the outstanding input streams that need to be closed
  private final Set<InputStream> streams = Collections.synchronizedSet(new HashSet<InputStream>());

  // configurable by env map
  private final String  nameEncoding;  // default encoding for name/comment

  // guarantees read/write access without having concurrency issues
  private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

  private final DlcFileSystemProvider provider;
  private final DlcPath defaultDir;
  private final boolean readOnly;
  private final Path dfpath;
  private final ZipCoder zc;
  private final FileChannel ch;
  private final ZipNode root;

  private volatile boolean isOpen = true;



  DlcFileSystem(DlcFileSystemProvider provider, Path dfpath, Map<String, ?> env) throws IOException
  {
    // configurable env setup
    if (env != null) {
      this.nameEncoding = env.containsKey("encoding") ? (String) env.get("encoding") : "CP437";
    } else {
      this.nameEncoding = "CP437";
    }

    this.readOnly = true;
    this.provider = provider;
    this.dfpath = dfpath;
    if (Files.notExists(this.dfpath)) {
      throw new FileSystemNotFoundException(this.dfpath.toString());
    }
    this.dfpath.getFileSystem().provider().checkAccess(this.dfpath, AccessMode.READ);
    this.zc = ZipCoder.get(nameEncoding);
    this.defaultDir = new DlcPath(this, getBytes("/"));
    this.ch = FileChannel.open(this.dfpath, StandardOpenOption.READ);
    this.root = ZipNode.createRoot(ch);
  }

  @Override
  public FileSystemProvider provider()
  {
    return provider;
  }

  @Override
  public void close() throws IOException
  {
    if (!isOpen) {
      return;
    }
    isOpen = false; // set closed
    if (!streams.isEmpty()) { // unlock and close all remaining streams
      Set<InputStream> copy = new HashSet<>(streams);
      for (InputStream is : copy) {
        is.close();
      }
    }
    ch.close(); // close the ch just in case no update

    provider.removeFileSystem(dfpath, this);
  }

  @Override
  public boolean isOpen()
  {
    return isOpen;
  }

  @Override
  public boolean isReadOnly()
  {
    return readOnly;
  }

  @Override
  public String getSeparator()
  {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories()
  {
    ArrayList<Path> pathArr = new ArrayList<>();
    pathArr.add(new DlcPath(this, new byte[] { '/' }));
    return pathArr;
  }

  @Override
  public Iterable<FileStore> getFileStores()
  {
    ArrayList<FileStore> list = new ArrayList<>(1);
    list.add(new DlcFileStore(new DlcPath(this, new byte[]{ '/' })));
    return list;
  }

  @Override
  public Set<String> supportedFileAttributeViews()
  {
    return supportedFileAttributeViews;
  }

  @Override
  public Path getPath(String first, String... more)
  {
    String path;
    if (more.length == 0) {
      path = first;
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append(first);
      for (String segment : more) {
        if (segment.length() > 0) {
          if (sb.length() > 0) {
            sb.append('/');
          }
          sb.append(segment);
        }
      }
      path = sb.toString();
    }
    return new DlcPath(this, getBytes(path));
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern)
  {
    int pos = syntaxAndPattern.indexOf(':');
    if (pos <= 0 || pos == syntaxAndPattern.length()) {
      throw new IllegalArgumentException();
    }
    String syntax = syntaxAndPattern.substring(0, pos);
    String input = syntaxAndPattern.substring(pos + 1);
    String expr;
    if (syntax.equals(GLOB_SYNTAX)) {
      expr = toRegexPattern(input);
    } else if (syntax.equals(REGEX_SYNTAX)) {
        expr = input;
    } else {
      throw new UnsupportedOperationException("Syntax '" + syntax + "' not recognized");
    }

    // return matcher
    final Pattern pattern = Pattern.compile(expr);
    return new PathMatcher() {
      @Override
      public boolean matches(Path path)
      {
        return pattern.matcher(path.toString()).matches();
      }
    };
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchService newWatchService() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString()
  {
    return dfpath.toString();
  }

  @Override
  protected void finalize() throws IOException
  {
    close();
  }

  Path getDlcFile()
  {
    return dfpath;
  }

  DlcPath getDefaultDir()
  {
    return defaultDir;
  }

  FileStore getFileStore(DlcPath path)
  {
    return new DlcFileStore(path);
  }

  DlcFileAttributes getFileAttributes(byte[] path)
  {
    ZipNode folder = null;
    beginRead();
    try {
      ensureOpen();
      folder = root.getNode(path);
    } finally {
      endRead();
    }
    if (folder != null) {
      return new DlcFileAttributes(folder);
    } else {
      return null;
    }
  }

  boolean exists(byte[] path)
  {
    beginRead();
    try {
      ensureOpen();
      return (root.getNode(path) != null);
    } finally {
      endRead();
    }
  }

  boolean isDirectory(byte[] path)
  {
    beginRead();
    try {
      ZipNode folder = root.getNode(path);
      return (folder != null && folder.isDirectory());
    } finally {
      endRead();
    }
  }

  private DlcPath toDlcPath(byte[] path)
  {
    // make it absolute
    byte[] p = new byte[path.length + 1];
    p[0] = '/';
    System.arraycopy(path, 0, p, 1, path.length);
    return new DlcPath(this, p);
  }

  // returns the list of child paths of "path"
  Iterator<Path> iteratorOf(byte[] path, DirectoryStream.Filter<? super Path> filter) throws IOException
  {
    beginRead(); // iteration of inodes needs exclusive lock
    try {
      ensureOpen();
      ZipNode folder = root.getNode(path);
      if (folder == null) {
        throw new NotDirectoryException(getString(path));
      }

      List<ZipNode> children = folder.getChildren();
      List<Path> pathList = new ArrayList<>();
      for (final ZipNode child: children) {
        pathList.add(toDlcPath(child.getPath()));
      }
      return Collections.unmodifiableList(pathList).iterator();
    } finally {
      endRead();
    }
  }

  // Returns the byte array representation of the specified string
  final byte[] getBytes(String name)
  {
    return zc.getBytes(name);
  }

  // Returns the string representation of the specified byte array using the current character encoding.
  final String getString(byte[] name)
  {
    return zc.toString(name);
  }

  // Returns an input stream for reading the contents of the specified file entry.
  InputStream newInputStream(byte[] path) throws IOException
  {
    beginRead();
    try {
      ensureOpen();
      ZipNode folder = root.getNode(path);
      if (folder == null) {
        throw new NoSuchFileException(getString(path));
      }
      if (folder.isDirectory()) {
        throw new FileSystemException(getString(path), "is a directory", null);
      }

      return getInputStream(folder);
    } finally {
      endRead();
    }
  }

  SeekableByteChannel newByteChannel(byte[] path, Set<? extends OpenOption> options,
                                     FileAttribute<?>... attrs) throws IOException
  {
    checkOptions(options);
    if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
      checkWritable();
    }

    beginRead();
    try {
      ensureOpen();
      ZipNode folder = root.getNode(path);
      if (folder == null) {
        throw new NoSuchFileException(getString(path));
      }
      if (folder.isDirectory()) {
        throw new FileSystemException(getString(path), "is a directory", null);
      }

      final long basePos = folder.getCentral().getDataOffset(ch);
      final long baseSize = folder.getCentral().sizeUncompressed;
      final SeekableByteChannel sbc = Files.newByteChannel(getDlcFile(), options, attrs);
      sbc.position(basePos);
      return new SeekableByteChannel() {
        @Override
        public boolean isOpen()
        {
          return sbc.isOpen();
        }

        @Override
        public void close() throws IOException
        {
          sbc.close();
        }

        @Override
        public int write(ByteBuffer src) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long size() throws IOException
        {
          return baseSize;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException
        {
          checkOpen();
          if (sbc.position() >= basePos+baseSize) {
            return -1;
          }
          ByteBuffer buffer = ByteBuffer.allocate(65536);
          int remaining = (int)(baseSize - position());
          remaining = Math.min(dst.remaining(), remaining);
          int processed = 0;
          while (remaining > 0) {
            buffer.compact().position(0);
            buffer.limit(Math.min(remaining, buffer.remaining()));
            int nread = sbc.read(buffer);
            if (nread < 0) {
              break;
            }
            buffer.flip();
            dst.put(buffer);
            remaining -= nread;
            processed += nread;
          }
          return processed;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException
        {
          checkOpen();
          if (newPosition < 0) {
            throw new IOException("Negative position");
          }
          sbc.position(basePos + newPosition);
          return this;
        }

        @Override
        public long position() throws IOException
        {
          checkOpen();
          return sbc.position() - basePos;
        }

        private void checkOpen() throws IOException
        {
          if (sbc == null || !sbc.isOpen()) {
            throw new IOException("Channel not open");
          }
        }
      };
    } finally {
      endRead();
    }
  }

  FileChannel newFileChannel(byte[] path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException
  {
    checkOptions(options);
    if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
      checkWritable();
    }
    beginRead();

    try {
      ensureOpen();
      ZipNode folder = root.getNode(path);
      if (folder == null) {
        throw new NoSuchFileException(getString(path));
      }
      if (folder.isDirectory()) {
        throw new FileSystemException(getString(path), "is a directory", null);
      }

      final long basePos = folder.getCentral().getDataOffset(ch);
      final long baseSize = folder.getCentral().sizeUncompressed;
      final FileChannel fch = FileChannel.open(getDlcFile(), options, attrs);
      fch.position(basePos);
      return new FileChannel() {
        @Override
        protected void implCloseChannel() throws IOException
        {
          fch.close();
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Position is negative");
          } else if (size < 0) {
            throw new IOException("Size is negative");
          }
          return fch.tryLock(basePos + position, Math.min(size, baseSize - position), shared);
        }

        @Override
        public FileChannel truncate(long size) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException
        {
          checkOpen();
          return fch.transferTo(basePos + position, Math.min(count, baseSize - position), target);
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException
        {
          throw new UnsupportedOperationException();
        }

        @Override
        public long size() throws IOException
        {
          checkOpen();
          return baseSize;
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
        {
          checkOpen();
          if (fch.position() >= basePos+baseSize) {
            return -1L;
          }

          ByteBuffer buffer = ByteBuffer.allocate(65536);
          long remaining = baseSize - position();
          long processed = 0;
          int idx = offset;
          int maxIdx = Math.min(dsts.length - offset, idx + length);
          while (idx < maxIdx) {
            long toRead = Math.min(dsts[idx].remaining(), remaining);
            while (toRead > 0) {
              buffer.compact().position(0);
              buffer.limit(Math.min((int)toRead, buffer.remaining()));
              int nread = fch.read(buffer);
              if (nread < 0) {
                idx = maxIdx;
                break;
              }
              buffer.flip();
              dsts[idx].put(buffer);
              toRead -= nread;
              remaining -= nread;
              processed += nread;
            }
            idx++;
          }
          return processed;
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException
        {
          checkOpen();
          if (position >= basePos+baseSize) {
            return -1;
          } else if (position < 0) {
            throw new IOException("Negative position");
          }

          long curPosition = fch.position();
          try {
            fch.position(basePos + position);
            long retVal = read(new ByteBuffer[]{dst}, 0, 1);
            return (int)retVal;
          } finally {
            fch.position(curPosition);
          }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException
        {
          return (int)read(new ByteBuffer[]{dst}, 0, 1);
        }

        @Override
        public FileChannel position(long newPosition) throws IOException
        {
          checkOpen();
          if (newPosition < 0) {
            throw new IOException("Negative position");
          }
          fch.position(basePos + newPosition);
          return this;
        }

        @Override
        public long position() throws IOException
        {
          checkOpen();
          return fch.position() - basePos;
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Negative position");
          } else if (size < 0) {
            throw new IOException("Negative size");
          } else if (position > baseSize) {
            throw new IOException("Position exceeds file size");
          }
          long absPos = basePos + position;
          long absSize = Math.min(size, baseSize - position);
          return fch.map(mode, absPos, absSize);
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException
        {
          checkOpen();
          if (position < 0) {
            throw new IOException("Position is negative");
          } else if (size < 0) {
            throw new IOException("Size is negative");
          }
          return fch.lock(basePos + position, Math.min(size, baseSize - position), shared);
        }

        @Override
        public void force(boolean metaData) throws IOException
        {
          checkOpen();
          // do nothing
        }

        private void checkOpen() throws IOException
        {
          if (fch == null || !fch.isOpen()) {
            throw new IOException("Channel not open");
          }
        }
      };
    } finally {
      endRead();
    }
  }

  private void checkWritable()
  {
    throw new ReadOnlyFileSystemException();
  }

  private void checkOptions(Set<? extends OpenOption> options)
  {
    // check for options of null type and option is an intance of
    // StandardOpenOption
    for (OpenOption option : options) {
      if (option == null) {
        throw new NullPointerException();
      }
      if (!(option instanceof StandardOpenOption)) {
        throw new IllegalArgumentException();
      }
    }
  }

  private final void beginRead()
  {
    rwlock.readLock().lock();
  }

  private final void endRead()
  {
    rwlock.readLock().unlock();
  }

  private void ensureOpen()
  {
    if (!isOpen) {
      throw new ClosedFileSystemException();
    }
  }

  private InputStream getInputStream(ZipNode folder) throws IOException
  {
    InputStream is = null;

    if (folder == null) {
      throw new NullPointerException();
    }
    if (folder.isDirectory()) {
      throw new FileSystemException(folder.toString(), "is a directory", null);
    }

    beginRead();
    try {
      ensureOpen();
      long offset = folder.getCentral().getDataOffset(ch);
      long size = folder.getCentral().sizeCompressed;
      if (offset < 0 || size < 0) {
        throw new IOException("Data offset out of range");
      }
      is = new ByteBufferInputStream(ch.map(MapMode.READ_ONLY, offset, size));
      streams.add(is);
      return is;
    } finally {
      endRead();
    }
  }
}
